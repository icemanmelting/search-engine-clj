(ns search-engine-clj.sessions
  (:require [clojure.string :as s]
            [clojure.tools.logging :as log]
            [schema.core :as sc]
            [search-engine-clj.validation :refer [validate humanize-error non-empty-str]]
            [search-engine-clj.response :refer [response error one]]
            [search-engine-clj.config :as config]
            [search-engine-clj.db.postgresql :refer [db def-db-fns uuid]])
  (:import (java.sql Timestamp)
           (java.security MessageDigest)))

(defn- uuid-from-header [r]
  (when-let [auth (get-in r [:headers "authorization"])]
    (when (> (count auth) 7)
      (uuid (subs auth 7)))))

(defn sha-256 [& args]
  (let [s (s/join (map #(str % "&||&") args))
        b (.getBytes s "UTF-8")
        md (MessageDigest/getInstance "SHA-256")]
    (.toString (BigInteger. 1 (.digest md b)) 16)))

(def-db-fns "sessions.sql")

(defn lookup [id]
  (let [[session err] (find-session db {:id id})]
    (when err
      (log/error err "Cannot lookup for a session in Postgres" id))
    session))

(defn find-one [{session :session}]
  (one :ok session))

(defn- touch
  ([id ts]
   (touch-session db {:id id :seen (Timestamp. ts)}))
  ([id]
   (touch id (System/currentTimeMillis))))

(defn- kill [id]
  (touch id 0))

(defn- find-user-by-id [l]
  (let [[user err] (find-user db {:login l})]
    (when err
      (log/error err "Cannot lookup for a user in Postgres" l))
    user))

(defn- live? [s]
  (if-let [seen (:seen s)]
    (> (+ (.getTime seen) 3600000) (System/currentTimeMillis))
    false))

(defn authorize [handler]
  (fn [r]
    (if-let [id (uuid-from-header r)]
      (if-let [s (lookup id)]
        (if (live? s)
          (do
            (touch id)
            (handler (assoc r :session s)))
          (response (error :authentication-timeout "Session expired")))
        (response (error :forbidden "Login required")))
      (response (error :forbidden "Login required")))))

(def ^:private login-fmt {:login non-empty-str :password non-empty-str})

(defn create [{body :body}]
  (let [[{l :login rp :password} err] (validate login-fmt body)]
    (if-not err
      (if-let [u (find-user-by-id l)]
        (if (= (:password u) (sha-256 rp (:salt u)))
          (let [id (uuid)
                [result err] (create-session db {:id id :login l :oid (:oid u)})]
            (if-not err
              (one :created (lookup id))
              (error :unprocessable-entity (humanize-error err))))
          (error :forbidden "Incorrect credentials"))
        (error :forbidden "Incorrect credentials"))
      (error :unprocessable-entity (humanize-error err)))))

(defn destroy [{session :session}]
  (let [[result err] (kill (:id session))]
    (if-not err
      (one :ok session)
      (error :unprocessable-entity (humanize-error err)))))
