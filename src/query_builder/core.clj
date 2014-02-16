(ns query-builder.core
  (:require [clojure.string :as s]))

(defn regex-parser
  "Splits string at ':keyword' and returns splitted
  string without keywords and coll of extracted keywords"
  [sql]
  (let [regex #"(?<=[^:]):(\w+)"
        splitted-sql (s/split sql regex)
        placeholders (map (comp keyword second) ; matched group is second element
                          (re-seq regex sql))]
    [splitted-sql placeholders])) 

(defn substitute-with
  "Substitute elements in placehoders collection for values
  from the parameter hash"
  [param-hash placeholders]
  (if (every? (set (keys param-hash)) placeholders)
    (map param-hash placeholders)
    ; TODO: add what parameter is missing (diff 2 sets?)
    (throw (IllegalArgumentException. "Missing parameter"))))

(defn ->question-marks
  "Transforms one level nested collections into strings of
  question marks to be used in parametrized query"
  [param-values]
  (let [len #(if (coll? %) (count %) 1)
        join (partial s/join ",")
        ->? #(repeat (len %) \?)]
    (map (comp join ->?) param-values)))

(defn query-vec
  "Forms a query vector of the form [sql params...] to be
  used directly in jdbc/query"
  ([sql param-hash]
   (query-vec regex-parser sql param-hash))
  ([parser sql param-hash]
   (let [[splitted-sql placeholders] (parser sql)
         param-values (substitute-with param-hash
                                       placeholders)
         sql-with-? (s/join
                      (interleave
                        splitted-sql
                        (->question-marks param-values)))]
     (into [sql-with-?] (flatten param-values)))))

(defn consv
  [item coll]
  ((comp vec cons) item coll))
