(ns search-engine-clj.db.postgresql
  (:require [hugsql.core :as hugsql]
            [hugsql.adapter.clojure-java-jdbc :as adp]
            [clojure.data.json :as json]
            [search-engine-clj.config :as config])
  (:import (java.util UUID)
           (org.postgresql.util PGobject)))

(set! *warn-on-reflection* true)

(defn uuid
  ([] (UUID/randomUUID))
  ([s] (try
         (UUID/fromString s)
         (catch Exception _ nil))))

(def db {:classname "org.postgresql.Driver"
         :subprotocol "postgresql"
         :subname (str "//"
                       (-> config/db :postgresql :hostname)
                       ":"
                       (-> config/db :postgresql :port)
                       "/"
                       (-> config/db :postgresql :database)
                       "?useSSL=false")
         :user (-> config/db :postgresql :user)
         :password (-> config/db :postgresql :password)})

(def ^:private adapter (adp/hugsql-adapter-clojure-java-jdbc))

(deftype AdapterWrapper [adapter]
  hugsql.adapter/HugsqlAdapter

  (execute [this db sqlvec options]
    (.execute ^hugsql.adapter.clojure_java_jdbc.HugsqlAdapterClojureJavaJdbc adapter db sqlvec options))

  (query [this db sqlvec options]
    (.query ^hugsql.adapter.clojure_java_jdbc.HugsqlAdapterClojureJavaJdbc adapter db sqlvec options))

  (result-one [this result options]
    [(.result-one ^hugsql.adapter.clojure_java_jdbc.HugsqlAdapterClojureJavaJdbc adapter result options) nil])

  (result-many [this result options]
    [(.result-many ^hugsql.adapter.clojure_java_jdbc.HugsqlAdapterClojureJavaJdbc adapter result options) nil])

  (result-affected [this result options]
    [(.result-affected ^hugsql.adapter.clojure_java_jdbc.HugsqlAdapterClojureJavaJdbc adapter result options) nil])

  (result-raw [this result options]
    [(.result-raw ^hugsql.adapter.clojure_java_jdbc.HugsqlAdapterClojureJavaJdbc adapter result options) nil])

  (on-exception [this exception]
    [nil exception]))

(hugsql/set-adapter! (AdapterWrapper. adapter))

(defmacro def-db-fns [n]
  `(hugsql/def-db-fns ~n))

(defn wrap-tx-results [& results]
  (let [results (doall results)]
    (case (count results)
      0 [nil (ex-info "No entries have been persisted" {})]
      1 (first results)
      (reduce (fn [[_ err1] [_ err2]]
                (cond
                  err1 [nil err1]
                  err2 [nil err2]))
              results))))
