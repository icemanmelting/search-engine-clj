(ns search-engine-clj.web-search
  (:require [search-engine-clj.db.postgresql :refer [db def-db-fns]]
            [search-engine-clj.response :refer [one many error]]
            [search-engine-clj.engine :as engine]
            [search-engine-clj.validation :refer [humanize-error non-empty-str validate]]
            [clojure.tools.logging :as log]))

(def ^:private document-fmt {:id non-empty-str
                                :content non-empty-str})

(def ^:private query-fmt {:query non-empty-str})

(def find-document-by-id-route "/_search/:id")

(def find-document-route "/_search")

(def upsert-document-route "/_index")

(def delete-document-route "/_search/:id")

(def-db-fns "documents.sql")

(defn find-document-by-id [{{:keys [id]} :params {:keys [login]} :session}]
  (if id
    (let [[doc err] (get-document-by-id db {:id id :owner login})]
      (if-not err
        (if doc
          (one :ok (select-keys doc [:id :content :created_at]))
          (one :ok {:api_message (str "No documents found with id " id)}))
        (do
          (log/error err (str "Problem retrieving document with id: " id))
          (error :unprocessable-entity (str "Problem retrieving document with id: " id)))))
    (do
      (log/error "You need to supply an id")
      (error :unprocessable-entity "You need to supply an id"))))

(defn find-document [{{:keys [query] :as body} :body {:keys [login]} :session}]
  (let [[_ err] (validate query-fmt body)]
    (if-not err
      (let [[docs err] (get-all-documents-by-user db {:user login})] ;;for demo sake only, final version would need pagination maybe, to limit memory usage. Could also use redis for caching
        (if-not err
          (if (seq docs)
            (let [matching-docs (filter #(engine/search (:content %) query) docs)]
              (many :ok (pmap #(select-keys % [:id :content :created_at]) matching-docs)))
            (many :ok []))
          (do
            (log/error err (str "Problem retrieving documents for user" login))
            (error :unprocessable-entity (str "Problem retrieving documents for user" login)))))
      (error :unprocessable-entity (humanize-error err)))))

(defn upsert-document [{{:keys [content id] :as body} :body {:keys [login]} :session}]
  (let [[_ err] (validate document-fmt body)]
    (if-not err
      (let [[_ err] (insert-document db {:owner login
                                         :id id
                                         :content content})]
        (if-not err
          (one :ok {:id id})
          (do
            (log/error err (str "Problem inserting document with id " id))
            (error :unprocessable-entity (str "Problem inserting document with id" id)))))
      (error :unprocessable-entity (humanize-error err)))))

(defn delete-document [{{:keys [id]} :params {:keys [login]} :session}]
  (if id
    (let [[doc err] (delete-document-by-id db {:id id :owner login})]
      (if-not err
        (one :ok {:id id})
        (do
          (log/error err (str "Problem deleting document with id: " id))
          (error :unprocessable-entity (log/error err (str "Problem deleting document with id: " id))))))
    (do
      (log/error "You need to supply an id")
      (error :unprocessable-entity "You need to supply an id"))))
