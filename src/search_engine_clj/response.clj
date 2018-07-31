(ns search-engine-clj.response
  (:require [cheshire.core :as json]
            [ring.util.response :refer [content-type]])
  (:import (java.util Date)))

(def codes
  {:continue 100
   :switching-protocols 101
   :processing 102
   :ok 200
   :created 201
   :accepted 202
   :non-authoritative-information 203
   :no-content 204
   :reset-content 205
   :partial-content 206
   :multi-status 207
   :im-used 226
   :multiple-choices 300
   :moved-permanently 301
   :found 302
   :see-other 303
   :not-modified 304
   :use-proxy 305
   :temporary-redirect 307
   :bad-request 400
   :unauthorized 401
   :payment-required 402
   :forbidden 403
   :not-found 404
   :method-not-allowed 405
   :not-acceptable 406
   :proxy-authentication-required 407
   :request-timeout 408
   :conflict 409
   :gone 410
   :length-required 411
   :precondition-failed 412
   :request-entity-too-large 413
   :request-uri-too-long 414
   :unsupported-media-type 415
   :requested-range-not-satisfiable 416
   :expectation-failed 417
   :authentication-timeout 419
   :unprocessable-entity 422
   :locked 423
   :failed-dependency 424
   :upgrade-required 426
   :internal-server-error 500
   :not-implemented 501
   :bad-gateway 502
   :service-unavailable 503
   :gateway-timeout 504
   :http-version-not-supported 505
   :insufficient-storage 507
   :not-extended 510})

(defn json
  ([st d h]
   (let [response (cond-> {:status (or (st codes) 200)
                           :body d}
                    h (assoc :headers h))
         json-response (update-in response [:body] json/generate-string)]
     (content-type json-response "application/json; charset=utf-8")))
  ([st d]
   (json st d nil)))

(defn plain-text
  ([st d h]
   (content-type (cond-> {:status (or (st codes) 200)
                          :body d}
                   h (assoc :headers h)) "text/plain"))
  ([st d]
   (plain-text st d nil)))

(defn error
  ([st d h]
   (cond-> {:status (or (st codes) 400)
            :body d}
     h (assoc :headers h)))
  ([st d]
   (error st d nil)))
