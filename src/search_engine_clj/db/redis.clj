(ns search-engine-clj.db.redis
  (:require [taoensso.carmine :as car]
            [search-engine-clj.config :as config]))

(set! *warn-on-reflection* true)

(def server-conn {:spec (-> config/db :redis)})

(defmacro wcar* [& body] `(car/wcar server-conn ~@body))
