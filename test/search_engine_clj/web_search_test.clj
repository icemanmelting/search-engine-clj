(ns search-engine-clj.web-search-test
  (:require [clojure.test :refer :all]
            [search-engine-clj.db.postgresql :refer [db def-db-fns]]
            [search-engine-clj.db.redis :refer [wcar*]]
            [taoensso.carmine :as car]
            [hugsql.core :refer [db-run]]
            [search-engine-clj.web-setup :refer :all])
  (:import (java.util Date)))

(def ^:private id1 "doc1")

(def ^:private id2 "doc2")

(def ^:private id3 "doc3")

(def ^:private test-id "id-test")

(def ^:private non-existing-id "non-id")

(def ^:private test-content "This is a test")

(def ^:private owner "foo@bar.com")

(def ^:private content1 "I really like bananas, apples not so much")

(def ^:private content2 "I dont like bananas, apples so and so")

(def ^:private content3 "this is updated like bananas")

(def-db-fns "documents.sql")

(defn- setup-session []
  (db-run db (str "INSERT INTO users (login, password, salt) VALUES"
                  "('" owner "', '3bb95188c01763e81875ce9644f496a4e3f98d9eb181c5f128ba32a01b62b6de', 'bar');"))
  (db-run db (str "INSERT INTO sessions (id, login, seen) VALUES"
                  "('00000000-0000-0000-0000-000000000000', '" owner "', now());")))

(defn- new-document [id content]
  (let [query (str "INSERT INTO documents(id, owner_id, content, created_at) VALUES"
                   "(:id, '" owner "', :content, NOW());")]
    (db-run db query {:id id
                      :content content})))

(defn- truncate-session []
  (db-run db "TRUNCATE users CASCADE"))

(defn- check-redis [id]
  (is (wcar* (car/get id))))

(defn clear-redis []
  (wcar* (car/flushall)))

(defn- populate-pg []
  (new-document id1 content1)
  (new-document id2 content2)
  (new-document id3 content3))

(defn- web-search-fixture [f]
  (truncate-session)
  (setup-session)
  (clear-redis)
  (populate-pg)
  (f))

(use-fixtures :each web-search-fixture)

(deftest test-search

  (testing "test document retrieval by id"

    (do (set-authorized-requests!)
        (web-run :get (str "/_search/" id1)))

    (check-redis id1)

    (is (= content1 (-> (extract-body) :content))))

  (testing "document research by AND"

    (do (set-authorized-requests!)
        (web-run :post "/_search" {:query "apples AND bananas"}))

    (check-redis "apples AND bananas")

    (let [results (extract-body)
          ids-set (into #{} (map :id results))]
      (is (= 2 (count results)))
      (is (true? (every? true? (map #(contains? #{id1 id2} %) ids-set))))))

  (testing "document research by OR"

    (do (set-authorized-requests!)
        (web-run :post "/_search" {:query "apples OR updated"}))

    (check-redis "apples OR updated")

    (let [results (extract-body)
          ids-set (into #{} (map :id results))]
      (is (= 3 (count results)))
      (is (true? (every? true? (map #(contains? #{id1 id2 id3} %) ids-set))))))

  (testing "document research by NOT"

    (do (set-authorized-requests!)
        (web-run :post "/_search" {:query "-updated"}))

    (let [results (extract-body)
          ids-set (into #{} (map :id results))]
      (is (= 2 (count results)))
      (is (true? (every? true? (map #(contains? #{id1 id2} %) ids-set))))))

  (testing "document research by WILDCARD"

    (do (set-authorized-requests!)
        (web-run :post "/_search" {:query "bana*"}))

    (check-redis "bana*")

    (let [results (extract-body)
          ids-set (into #{} (map :id results))]
      (is (= 3 (count results)))
      (is (true? (every? true? (map #(contains? #{id1 id2 id3} %) ids-set))))))

  (testing "unauthorized request"

    (do (set-unauthorized-requests!)
        (web-run :post "/_search" {:query "'banana'"}))

    (let [status (-> @resp :status)]
      (is (= 403 status)))))

(deftest test-creation

  (testing "Non existing document"

    (do (set-authorized-requests!)
        (web-run :post "/_index" {:id test-id
                                  :content test-content}))

    (let [[{:keys [id content]} err] (get-document-by-id db {:id test-id
                                                             :owner owner})]
      (is (nil? err))

      (are [x y] (= x y)
                 test-id id
                 test-content content)))

  (testing "Upsert existing document"

    (let [[{:keys [id content]} err] (get-document-by-id db {:id id1
                                                             :owner owner})]
      (is (nil? err))

      (are [x y] (= x y)
                 id1 id
                 content1 content)

      (do (set-authorized-requests!)
          (web-run :post "/_index" {:id id1
                                    :content test-content}))

      (let [[{:keys [id content]} err] (get-document-by-id db {:id id1
                                                               :owner owner})]
        (is (nil? err))

        (are [x y] (= x y)
                   id1 id
                   test-content content))))

  (testing "unauthorized request"

    (do (set-unauthorized-requests!)
        (web-run :post "/_index" {:id id1
                                  :content test-content}))

    (let [status (-> @resp :status)]
      (is (= 403 status)))))


(deftest test-deletion

  (testing "delete existing document"

    (let [[{:keys [id content]} err] (get-document-by-id db {:id id1
                                                             :owner owner})]
      (is (nil? err))

      (are [x y] (= x y)
                 id1 id
                 content1 content)

      (do (set-authorized-requests!)
          (web-run :delete (str "/_search/" id1)))

      (let [[{:keys [id content]} err] (get-document-by-id db {:id id1
                                                               :owner owner})]
        (is (nil? err))

        (is (nil? content)))))

  (testing "delete non existing document"

    (do (set-authorized-requests!)
        (web-run :delete (str "/_search/" non-existing-id)))

    (let [status (-> @resp :status)
          message (extract-body)]

      (are [expected result] (= expected result)
                             status 422
                             (str "Document with id " non-existing-id " not found") message)))

  (testing "unauthorized request"

    (do (set-unauthorized-requests!)
        (web-run :delete (str "/_search/" non-existing-id)))

    (let [status (-> @resp :status)]
      (is (= 403 status)))))
