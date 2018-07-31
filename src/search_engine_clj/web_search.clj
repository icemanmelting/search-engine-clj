(ns search-engine-clj.web-search
  (:require [search-engine-clj.db.postgresql :refer [db def-db-fns]]
            [search-engine-clj.response :refer [json error]]
            [search-engine-clj.engines.search :refer [search]]
            [search-engine-clj.validation :refer [humanize-error non-empty-str validate]]
            [clojure.tools.logging :as log]
            [search-engine-clj.config :as config]
            [taoensso.carmine :as car]
            [search-engine-clj.db.redis :refer [wcar*]]))

(def ^:private document-fmt {:id non-empty-str
                             :content non-empty-str})

(def ^:private query-fmt {:query non-empty-str})

(def ^:private engine-conf (:engine (config/read-from-resource "engine.edn")))

(def ^:private engine-type (:type engine-conf))

(def ^:private cache-ttl (:cache-ttl engine-conf))

(def find-document-by-id-route "/_search/:id")

(def find-document-route "/_search")

(def upsert-document-route "/_index")

(def delete-document-route "/_search/:id")

(def-db-fns "documents.sql")

(defn- handle-error [msg]
  (log/error msg)
  (error :unprocessable-entity msg))

(defn find-document-by-id [{{:keys [id]} :params {:keys [login]} :session}]
  (if id
    (if-let [doc (wcar* (car/get id))]
      (json :ok doc)
      (let [[doc err] (get-document-by-id db {:id id :owner login})]
        (if-not err
          (if doc
            (let [doc (select-keys doc [:id :content :created_at])]
              (wcar* (car/setex id cache-ttl doc))
              (json :ok doc))
            (json :ok {:api_message (str "No documents found with id " id)}))
          (handle-error (str err " Problem retrieving document with id: " id)))))
    (handle-error "You need to supply an id")))

(defn find-document [{{:keys [query] :as body} :body {:keys [login]} :session}]
  (let [[_ err] (validate query-fmt body)]
    (if-not err
      (if-let [results (seq (wcar* (car/get query)))]
        (json :ok results)
        (let [[docs err] (get-all-documents-by-user db {:user login})]
          (if-not err
            (if (seq docs)
              (let [search-result (search engine-type (map :content docs) query)
                    matching-docs (->> (map #(when %1 %2) search-result docs)
                                       (filter some?))
                    result-documents (pmap #(select-keys % [:id :content :created_at]) matching-docs)]
                (wcar* (car/setex query cache-ttl result-documents))
                (json :ok result-documents))
              (json :ok []))
            (handle-error (str err " Problem retrieving documents for user" login)))))
      (error :unprocessable-entity (humanize-error err)))))

(defn upsert-document [{{:keys [content id] :as body} :body {:keys [login]} :session}]
  (let [[_ err] (validate document-fmt body)]
    (if-not err
      (let [_ (wcar* (car/del id))
            [_ err] (insert-document db {:owner login
                                         :id id
                                         :content content})]
        (if-not err
          (json :ok {:id id})
          (handle-error (str err " Problem inserting document with id " id))))
      (error :unprocessable-entity (humanize-error err)))))

(defn delete-document [{{:keys [id]} :params {:keys [login]} :session}]
  (if id
    (let [[doc err] (get-document-by-id db {:id id :owner login})]
      (if-not err
        (if doc
          (let [_ (wcar* (car/del id))
                [_ err] (delete-document-by-id db {:id id :owner login})]
            (if-not err
              (json :ok {:id id})
              (handle-error (str err "Problem deleting document with id: " id))))
          (error :unprocessable-entity (str "Document with id " id " not found")))
        (handle-error (str err " Problem retrieving document with id " id))))
    (handle-error "You need to supply an id")))
