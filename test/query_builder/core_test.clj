(ns query-builder.core-test
  (:require [clojure.test :refer :all]
            [clojure.string]
            [query-builder.core :refer :all]))

#_(deftest test-split-regex
  (are [res sql] (= res (clojure.string/split sql *split-regex*))
    ; splits at '?'
    ["a = ", " and b = ", " and c = "]
    "a = ? and b = ? and c = "

    ; splits at ':keyword'
    ["a = ", " and b = ", " and c = "]
    "a = :key1 and b = :key2 and c = :key3" 

    ; does not split at '::casts'
    ["a = 1::text"]
    "a = 1::text"

    ; does not understand quotation
    ["a = 'mistake", "' and b = 1"]
    "a = 'mistake?' and b = 1"

    ; does not understand quotation
    ["a = 'part", "' and b = 1"]
    "a = 'part:val' and b = 1"))

#_(deftest test-placeholders
  (are [res sql] (= res (placeholders *split-regex* sql))
       [:? :key1 :? :key2]
       "a = ? and b = :key1 and c in (?) and d = :key2"

       []
       "a = 1::text and b = 1"))

(deftest test-substitute-with
  (are [param-value placeholders param-hash]
       (= param-value (substitute-with param-hash placeholders))

       [1, 2, 3]
       [:a, :b, :c]
       {:a 1, :b 2, :c 3}
       
       [[3 3] 1 2 [3 3]]
       [:c :a :b :c]
       {:a 1 :b 2 :c [3 3]}
       )

  (testing "not enough parameters given"
    (is (thrown? java.lang.IllegalArgumentException
                 (substitute-with {:a 1 :b 2} [:a :b :c]))))
    (is (thrown? java.lang.IllegalArgumentException
                 (substitute-with {:a 1 :b 2 :c 3} [:a :b :d]))) 
  )

(deftest test-->question-marks
  (is (= ["?", "?,?,?", "?,?", "?"]
         (->question-marks [1 [1 1 1] [1 1] 1]))))

(deftest test-query-vec
  (is (= ["a = ? and b in (?,?,?) and c = ?", 1, 2 3 4, 5]
         (query-vec "a = :a and b in (:bs) and c = :c" {:a 1 :bs [2 3 4] :c 5}))))

(deftest test-regex-parser
  (are [splitted-sql params sql] (= [splitted-sql params] (regex-parser sql))
       ["a = ", " and b in (", ") and c = "]
       [:a :bs :c]
       "a = :a and b in (:bs) and c = :c"))

       ["a = ", " and b in (", ") and c = 1"]
       [:a :bs]
       "a = :a and b in (:bs) and c = 1"  
