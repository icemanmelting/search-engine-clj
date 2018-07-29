(ns search-engine-clj.engines.search
  (:require [search-engine-clj.engines.opencl-engine :as opencl]
            [search-engine-clj.engines.regex-engine :as regex]))

(defmulti search (fn [type content query]
                   type))

(defmethod search :opencl [_ content query]
  (opencl/search content query))

(defmethod search :regex [_ content query]
  (regex/search content query))

(defmethod search :default [type content query]
  (throw (ex-info (str "Engine type " type " doesn't exist") {})))
