(ns search-engine-clj.web-setup
  (:require [clojure.test :refer :all]
            [search-engine-clj.core :refer [app]]
            [ring.mock.request :as mock]
            [cheshire.core :as json]))

(def resp (atom nil))

(def ^:private req-fn (atom nil))

(defn req [method uri body]
  (-> (mock/request method uri)
      (mock/content-type "application/json; charset=utf-8")
      (mock/body (json/generate-string body))))

(defn auth-req [method uri body]
  (mock/header (req method uri body) "Authorization" "Bearer 00000000-0000-0000-0000-000000000000"))

(defn set-authorized-requests! []
  (reset! req-fn auth-req))

(defn set-unauthorized-requests! []
  (reset! req-fn req))

(defn web-run
  ([req-fn method uri body] (reset! resp (app (req-fn method uri body))))
  ([method uri body] (web-run @req-fn method uri body))
  ([method uri] (web-run method uri nil)))

(defn extract-body []
  (if-let [body (try (json/decode (:body @resp) keyword)
                     (catch Exception _))]
    (if (coll? body)
      body
      (first body))
    (:body @resp)))
