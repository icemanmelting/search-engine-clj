(ns search-engine-clj.engine-test
  (:require [clojure.test :refer :all]
            [search-engine-clj.engines.search :refer [search]]))

(def ^:private opencl-engine :opencl)

(def ^:private regex-engine :regex)

(def ^:private test-strings ["I really like bananas, apples not so much"])

(deftest test-search-engine-opencl
  (testing "and operator"

    (is (= [true] (search :opencl test-strings "bananas AND apples")))
    (is (= [false] (search :opencl test-strings "bananas AND oranges"))))

  (testing "or operator"

    (is (= [true] (search :opencl test-strings "bananas OR oranges")))
    (is (= [false] (search :opencl test-strings "mangos AND oranges"))))

  (testing "wildcard operator"

    (is (= [true] (search :opencl test-strings "bana*")))
    (is (= [false] (search :opencl test-strings "mang*"))))

  (testing "not operator"

    (is (= [true] (search :opencl test-strings "-oranges")))
    (is (= [false] (search :opencl test-strings "-apples")))))


(deftest test-search-engine-regex
  (testing "and operator"

    (is (= [true] (search :regex test-strings "bananas AND apples")))
    (is (= [false] (search :regex test-strings "bananas AND oranges"))))

  (testing "or operator"

    (is (= [true] (search :regex test-strings "bananas OR oranges")))
    (is (= [false] (search :regex test-strings "mangos OR oranges"))))

  (testing "wildcard operator"

    (is (= [true] (search :regex test-strings "bana*")))
    (is (= [false] (search :regex test-strings "mang*"))))

  (testing "not operator"
    (is (= [true] (search :regex test-strings "-oranges")))
    (is (= [false] (search :regex test-strings "-apples")))))
