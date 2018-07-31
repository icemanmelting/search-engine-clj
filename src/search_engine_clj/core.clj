(ns search-engine-clj.core
  (:require [compojure.core :refer :all]
            [org.httpkit.server :refer [run-server]]
            [ring.middleware.cors :as cors]
            [ring.middleware.json :as json]
            [ring.middleware.params :refer [wrap-params]]
            [search-engine-clj.web-search :as ws]
            [search-engine-clj.response :refer [error]]
            [search-engine-clj.sessions :refer [authorize] :as sessions])
  (:gen-class))

(defn- wrap-json-body [h]
  (json/wrap-json-body h {:keywords? true
                          :malformed-response (error :unprocessable-entity "Wrong JSON format")}))

(defn- wrap-cors [h]
  (cors/wrap-cors h
                  :access-control-allow-origin #".+"
                  :access-control-allow-methods [:get :put :post :delete :options]))

(defroutes web-search-routes
  (GET ws/find-document-by-id-route [] (wrap-params ws/find-document-by-id))
  (DELETE ws/delete-document-route [] (wrap-params ws/delete-document))
  (POST ws/find-document-route [] ws/find-document)
  (POST ws/upsert-document-route [] ws/upsert-document))

(defroutes session-routes
  (GET "/session" [] (authorize sessions/find-one))
  (DELETE "/session" [] (authorize sessions/destroy))
  (POST "/session" [] sessions/create))

(defroutes app
  (-> (routes session-routes (authorize web-search-routes)) wrap-json-body wrap-cors))

(defn -main []
  (run-server app {:port 8080})
  (println "Listening on port 8080"))
