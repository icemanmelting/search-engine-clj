(ns search-engine-clj.db.redis
  (:require [taoensso.carmine :as car]
            [search-engine-clj.config :as config]))

(set! *warn-on-reflection* true)

(def server-conn {:spec (-> config/db :redis)})

(defmacro wcar* [& body] `(car/wcar server-conn ~@body))

(defn retrieve-from-cache [handler]
  (fn [{{:keys [id]} :params {:keys [query]} :body :as r}]
    (handler (assoc r :doc (or (wcar* (car/get id)) (seq (wcar* (car/get query))))))))

(defn delete-from-cache [handler]
  (fn [{{:keys [id]} :params :as r}]
    (wcar* (car/del id))
    (handler r)))
