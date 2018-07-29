(ns search-engine-clj.engines.regex-engine
  (:require [clojure.string :as s]))

(def ^:private and-pattern (re-pattern " AND "))

(def ^:private or-pattern (re-pattern " OR "))

(def ^:private not-pattern (re-pattern "-.*"))

(def ^:private wildcard-pattern (re-pattern "\\*"))

(defmulti ^:private query-content (fn [content query-terms operator]
                                    operator))

(defn- and-loop [& values]
  (loop [[v & vs] values]
    (if v
      (if (seq vs)
        (recur vs)
        (not (nil? v)))
      (not (nil? v)))))

(defmethod query-content :and [content query-terms _]
  (->> query-terms
       (map #(for [c content]
               (re-find (re-pattern %) c)))
       (apply map and-loop)))

(defn- or-loop [& values]
  (loop [[v & vs] values]
    (if v
      (not (nil? v))
      (if (seq vs)
        (recur vs)
        (not (nil? v))))))

(defmethod query-content :or [content query-terms _]
  (->> query-terms
       (map #(re-find (re-pattern %2) %1) content)
       (map or-loop)))

(defmethod query-content :not [content query-terms _]
  (->> query-terms
       (map #(for [c content]
               (re-find (re-pattern %) c)))
       (apply map #(not (and-loop %)))))

(defmethod query-content :wildcard [content query-terms _]
  (prn (->> query-terms
            (map #(for [c content]
                    (re-find (re-pattern %) c)))))
  (->> query-terms
       (map #(for [c content]
               (re-find (re-pattern %) c)))
       (apply map and-loop)))

(defn search [content query]
  (let [and (re-find and-pattern query)
        or (re-find or-pattern query)
        n (re-find not-pattern query)
        w (re-find wildcard-pattern query)
        queries (cond-> {}
                  and (assoc :and (s/split query and-pattern))
                  or (assoc :or (s/split query or-pattern))
                  n (assoc :not [(s/replace query #"-" "")])
                  w (assoc :wildcard [(s/replace query wildcard-pattern ".*")]))
        queries (if (seq queries)
                  queries
                  (assoc queries :and [query]))]
    (flatten (for [[k v] queries]
               (query-content content v k)))))
