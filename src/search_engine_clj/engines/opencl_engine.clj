(ns search-engine-clj.engines.opencl-engine
  (:require [clojure.string :as s]
            [clojure.java.io :refer [resource]]
            [uncomplicate.clojurecl.core :refer :all]
            [uncomplicate.commons.core :refer [with-release info]]))

(def ^:private string-matching-source (slurp (resource "stringSearch.cl")))

(def ^:private opencl-platforms (platforms))

(def ^:private opencl-contexts (map #(context (devices %)) opencl-platforms))

(def ^:private opencl-devices-and-context
  (flatten (map #(for [d (devices %)]
                   {:device d
                    :local-size (info d :max-work-group-size)
                    :global-size (* (info d :max-work-group-size) (info d :max-compute-units))
                    :context (context (devices %))}) opencl-platforms)))

(def ^:private opencl-devices-and-program
  (map #(assoc % :command-queue (command-queue (:context %) (:device %))
                 :program (build-program! (program-with-source (:context %) [string-matching-source]))) opencl-devices-and-context))

(def ^:private opencl-devices-complete (map #(assoc % :kernel (kernel (:program %) "stringSearch")) opencl-devices-and-program))

(def ^:private devices-count (count opencl-devices-complete))

(defn- search-strings [pattern strs context kernel cqueue]
  (let [text-array (byte-array (-> strs (s/join) (.getBytes) seq))
        chars-per-item (int-array (map count strs))
        pattern-array (byte-array (-> pattern (.getBytes) seq))
        result-array-size (count strs)
        result-array (int-array result-array-size)
        work-sizes (work-size [result-array-size] [result-array-size])]
    (with-release [text-buffer (cl-buffer context (alength text-array) :write-only)
                   pattern-buffer (cl-buffer context (alength pattern-array) :write-only)
                   per-item-buffer (cl-buffer context (* 4 (alength chars-per-item)) :write-only)
                   result-buffer (cl-buffer context (* 4 (alength chars-per-item)) :read-only)]

                  (set-args! kernel text-buffer pattern-buffer (int-array [(int (count pattern))]) per-item-buffer result-buffer)

                  (-> cqueue
                      (enq-write! text-buffer text-array)
                      (enq-write! pattern-buffer pattern-array)
                      (enq-write! per-item-buffer chars-per-item)
                      (enq-write! result-buffer result-array)
                      (enq-nd! kernel work-sizes)
                      (enq-read! result-buffer result-array)
                      (finish!))

                  (->> result-array (map int)))))

(defn- split-collection [docs]
  (loop [result []
         strs docs
         [{:keys [global-size] :as d} & devs] opencl-devices-complete]
    (if d
      (let [result (conj result (take global-size strs))
            strs-count (count strs)
            strs-left (- strs-count global-size)]
        (if (> strs-left 0)
          (recur result (take-last strs-left strs) devs)
          result))
      result)))

(defn search-pattern [strs pattern]
  (let [strs (split-collection strs)]
    (flatten (map #(let [{:keys [context kernel max-compute-units command-queue]} %2]
                     (search-strings pattern %1 context kernel command-queue)) strs (take (count strs) opencl-devices-complete)))))

(def ^:private and-pattern (re-pattern " AND "))

(def ^:private or-pattern (re-pattern " OR "))

(def ^:private not-pattern (re-pattern "-.*"))

(def ^:private wildcard-pattern (re-pattern "\\*"))

(def ^:private exact-pattern (re-pattern "'(.*)\\'"))

(defmulti ^:private query-content (fn [content query-terms operator]
                                    operator))

(defmethod query-content :and [content query-terms _]
  (->> query-terms
       (map #(search-pattern content %))
       (apply map +)
       (map #(= (count query-terms) %))))

(defmethod query-content :or [content query-terms _]
  (->> query-terms
       (map #(search-pattern content %))
       (apply map +)
       (map #(> % 0))))

(defmethod query-content :not [content query-terms _]
  (->> query-terms
       (map #(search-pattern content %))
       (apply map +)
       (map #(= 0 %))))

(defmethod query-content :wildcard [content query-terms _]
  (->> query-terms
       (map #(search-pattern content %))
       (apply map +)
       (map #(= (count query-terms) %))))

(defn search [content query]
  (let [and (re-find and-pattern query)
        or (re-find or-pattern query)
        n (re-find not-pattern query)
        w (re-find wildcard-pattern query)
        queries (cond-> {}
                  and (assoc :and (s/split query and-pattern))
                  or (assoc :or (s/split query or-pattern))
                  n (assoc :not [(s/replace query #"-" "")])
                  w (assoc :wildcard [(s/replace query wildcard-pattern "")]))
        queries (if (seq queries)
                  queries
                  (assoc queries :and [query]))]
    (flatten (for [[k v] queries]
               (query-content content v k)))))
