(ns search-engine-clj.web-search-test
  (:require [clojure.test :refer :all]
            [search-engine-clj.db.postgresql :refer [db]]
            [hugsql.core :refer [db-run]]
            [search-engine-clj.web-setup :refer :all])
  (:import (java.util Date)))

(def ^:private id1 "doc1")

(def ^:private id2 "doc2")

(def ^:private id3 "doc3")

(def ^:private content1 "I really like bananas, apples not so much")

(def ^:private content2 "I dont like bananas, apples so and so")

(def ^:private content3 "this is updated like bananas")

(defn- setup-session []
  (db-run db (str "INSERT INTO users (login, password, salt) VALUES"
                  "('foo@bar.com', '3bb95188c01763e81875ce9644f496a4e3f98d9eb181c5f128ba32a01b62b6de', 'bar');"))
  (db-run db (str "INSERT INTO sessions (id, login, seen) VALUES"
                  "('00000000-0000-0000-0000-000000000000', 'foo@bar.com', now());")))

(defn- new-document [id content]
  (let [query "INSERT INTO documents(id, owner_id, content, created_at) VALUES (:id, 'foo@bar.com', :content, NOW());"]
    (db-run db query {:id id
                      :content content})))

(defn- truncate-session []
  (db-run db "TRUNCATE users CASCADE"))

(defn- populate-pg []
  (new-document id1 content1)
  (new-document id2 content2)
  (new-document id3 content3))

(defn- web-search-fixture [f]
  (truncate-session)
  (setup-session)
  (populate-pg)
  (f))

(use-fixtures :each web-search-fixture)

(deftest test-search

  (testing "test document retrieval by id"

    (do (set-authorized-requests!)
        (web-run :get (str "/_search/" id1)))

    (is (= content1 (-> (extract-body) :content))))

  (testing "document research by AND"

    (do (set-authorized-requests!)
        (web-run :post "/_search" {:query "apples AND bananas"}))

    (let [results (-> (extract-body) :results)
          ids-set (into #{} (map :id results))]
      (is (= 2 (count results)))
      (is (true? (every? true? (map #(contains? #{id1 id2} %) ids-set))))))

  (testing "document research by OR"

    (do (set-authorized-requests!)
        (web-run :post "/_search" {:query "apples OR updated"}))

    (let [results (-> (extract-body) :results)
          ids-set (into #{} (map :id results))]
      (is (= 3 (count results)))
      (is (true? (every? true? (map #(contains? #{id1 id2 id3} %) ids-set))))))

  (testing "document research by NOT"

    (do (set-authorized-requests!)
        (web-run :post "/_search" {:query "-updated"}))

    (let [results (-> (extract-body) :results)
          ids-set (into #{} (map :id results))]
      (is (= 2 (count results)))
      (is (true? (every? true? (map #(contains? #{id1 id2} %) ids-set))))))

  (testing "document research by WILDCARD"

    (do (set-authorized-requests!)
        (web-run :post "/_search" {:query "bana*"}))

    (let [results (-> (extract-body) :results)
          ids-set (into #{} (map :id results))]
      (is (= 3 (count results)))
      (is (true? (every? true? (map #(contains? #{id1 id2 id3} %) ids-set))))))

  (testing "document research by EXACT"

    (do (set-authorized-requests!)
        (web-run :post "/_search" {:query "'bananas'"}))

    (let [results (-> (extract-body) :results)
          ids-set (into #{} (map :id results))]
      (is (= 3 (count results)))
      (is (true? (every? true? (map #(contains? #{id1 id2 id3} %) ids-set))))))

  (testing "document research by EXACT no matches"

    (do (set-authorized-requests!)
        (web-run :post "/_search" {:query "'banana'"}))

    (let [results (-> (extract-body) :results)
          ids-set (into #{} (map :id results))]
      (is (= 0 (count results)))))

  (testing "unauthorized request"

    (do (set-unauthorized-requests!)
        (web-run :post "/_search" {:query "'banana'"}))

    (let [status (-> @resp :status)]
      (is (= 403 status)))))

