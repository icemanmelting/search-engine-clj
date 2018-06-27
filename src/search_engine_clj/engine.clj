(ns search-engine-clj.engine
  (:require [clojure.string :as s]))

(def ^:private and-pattern (re-pattern " AND "))

(def ^:private or-pattern (re-pattern " OR "))

(def ^:private not-pattern (re-pattern "-.*"))

(def ^:private wildcard-pattern (re-pattern "\\*"))

(def ^:private exact-pattern (re-pattern "'(.*)\\'"))

(defmulti ^:private query-content (fn [content query-terms operator]
                                    operator))

(defmethod query-content :and [content query-terms _]
  (every? #(not (nil? %))
          (map #(re-find (re-pattern %) content) query-terms)))

(defmethod query-content :or [content query-terms _]
  (some #(not (nil? %))
        (map #(re-find (re-pattern %) content) query-terms)))

(defmethod query-content :not [content query-patterns _]
  (nil? (re-find (re-pattern query-patterns) content)))

(defmethod query-content :wildcard [content query-patterns _]
  (not (nil? (re-find (re-pattern query-patterns) content))))

(defmethod query-content :exact [content query-patterns _]
  (not (nil? (re-find (re-pattern (str ".*\\b" query-patterns "\\b.*")) content))))

(defn search [content query]
  (let [and (re-find and-pattern query)
        or (re-find or-pattern query)
        n (re-find not-pattern query)
        w (re-find wildcard-pattern query)
        exact (re-find exact-pattern query)
        queries (cond-> {}
                  and (assoc :and (s/split query and-pattern))
                  or (assoc :or (s/split query or-pattern))
                  n (assoc :not (s/replace query #"-" ""))
                  w (assoc :wildcard (s/replace query wildcard-pattern ".*"))
                  exact (assoc :exact (second exact)))
        queries (if (seq queries)
                  queries
                  (assoc queries :and query))]
    (every? true? (for [[k v] queries]
                    (query-content content v k)))))
