(ns search-engine-clj.engine-test
  (:require [clojure.test :refer :all]
            [search-engine-clj.engine :refer [search]]))

(def ^:private test-string "I really like bananas, apples not so much")

(deftest test-search-engine
  (testing "and operator"

    (is (true? (search test-string "bananas AND apples")))
    (is (false? (search test-string "bananas AND oranges"))))

  (testing "or operator"

    (is (true? (search test-string "bananas OR oranges")))
    (is (false? (search test-string "mangos AND oranges"))))

  (testing "wildcard operator"

    (is (true? (search test-string "bana*")))
    (is (false? (search test-string "mang*"))))

  (testing "exact operator"

    (is (true? (search test-string "'bananas'")))
    (is (false? (search test-string "'mangos'"))))

  (testing "not operator"
    (is (true? (search test-string "-oranges")))
    (is (false? (search test-string "-apples")))))
