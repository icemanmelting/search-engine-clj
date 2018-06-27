(ns search-engine-clj.core
  (:require [compojure.core :refer :all]
            [org.httpkit.server :refer [run-server]]
            [ring.middleware.cors :as cors]
            [ring.middleware.json :as json :refer [wrap-json-response]]
            [ring.middleware.params :refer [wrap-params]]
            [search-engine-clj.web-search :as ws]
            [search-engine-clj.response :refer [error render response]]
            [search-engine-clj.sessions :refer [authorize] :as sessions])
  (:gen-class))

(defn- wrap-json-body [h]
  (json/wrap-json-body h {:keywords? true
                          :malformed-response (response (error :unprocessable-entity "Wrong JSON format"))}))

(defn- wrap-cors [h]
  (cors/wrap-cors h
                  :access-control-allow-origin #".+"
                  :access-control-allow-methods [:get :put :post :delete :options]))

(defroutes web-search-routes
  (GET ws/find-document-by-id-route [] (wrap-params (render ws/find-document-by-id)))
  (POST ws/find-document-route [] (render ws/find-document))
  (POST ws/upsert-document-route [] (render ws/upsert-document))
  (DELETE ws/delete-document-route [] (wrap-params (render ws/delete-document))))

(defroutes session-routes
  (POST "/session" [] (render sessions/create))
  (GET "/session" [] (authorize (render sessions/find-one)))
  (DELETE "/session" [] (authorize (render sessions/destroy))))

(defroutes app
  (-> (routes session-routes (authorize web-search-routes)) wrap-json-body wrap-json-response wrap-cors))

(defn -main []
  (run-server app {:port 8080})
  (println "Listening on port 8080"))
