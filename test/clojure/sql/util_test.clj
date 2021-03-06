(ns clojure.sql.util-test
  (:require [clojure.test :refer :all]
            [clojure.sql.util :refer :all]))  

(deftest test-substitute-with
  (are [param-value placeholders param-hash]
       (= param-value (substitute-with param-hash placeholders))

       []
       []
       {}

       [1, 2, 3]
       [:a, :b, :c]
       {:a 1, :b 2, :c 3}
       
       [[3 3] 1 2 [3 3]]
       [:c :a :b :c]
       {:a 1 :b 2 :c [3 3]}
       )

  (testing "Unmatched parameters"
    (is (thrown? java.lang.IllegalArgumentException
                 (substitute-with {:a 1 :b 2} [:a :b :c]))))
    (is (thrown? java.lang.IllegalArgumentException
                 (substitute-with {:a 1 :b 2 :c 3} [:a :b :d]))))

(deftest test-->question-marks
  (is (= ["?", "?,?,?", "?,?", "?"]
         (->question-marks [1 [1 1 1] [1 1] 1]))))

(deftest test-query-vec
  (are [qvec splitted-sql params opts] (= qvec (query-vec splitted-sql params opts))
    ; no params or opts
    [{} "select"]
    [["select"] []]
    nil
    nil

    ; only params
    [{} "a = ? and b in (?,?,?) and c = ?", 1, 2 3 4, 5]
    [["a = " " and b in (" ") and c = "] [:a :bs :c]]
    {:a 1 :bs [2 3 4] :c 5}
    {}

    ; only opts
    [{:fetch-size 100} "select"]
    [["select"] []]
    nil
    {:fetch-size 100}

    ; with params and opts
    [{:fetch-size 100} "a = ? and b in (?,?,?) and c = ?", 1, 2 3 4, 5]          
    [["a = " " and b in (" ") and c = "] [:a :bs :c]]
    {:a 1 :bs [2 3 4] :c 5}
    {:fetch-size 100}))

(deftest test-regex-parser
  (are [splitted-sql params sql] (= [splitted-sql params] (regex-parser sql))
       ["a = ", " and b in (", ") and c = "]
       [:a :bs :c]
       "a = :a and b in (:bs) and c = :c"

       ["a = ", " and b in (", ") and c in (", ")"]
       [:a :bs :cs]
       "a = :a and b in (:bs) and c in (:cs)"

       ["a = ", " and b in (", ") and c = 1"]
       [:a :bs]
       "a = :a and b in (:bs) and c = 1"
       
       ["par-1 = "]
       [:par-1]
       "par-1 = :par-1"

       ["par_1 = "]
       [:par_1]
       "par_1 = :par_1"))
