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
  (if (and (every? (set (keys param-hash)) placeholders)
           (every? (set placeholders) (keys param-hash)))
    (map param-hash placeholders)
    ;; TODO: add what parameters are unmatched (diff 2 sets?)
    (throw (IllegalArgumentException. "Unmatched parameter"))))

(defn- ->question-marks
  "Transforms one level nested collections into strings of
  question marks to be used in parametrized query"
  [param-values]
  (let [len #(if (coll? %) (count %) 1)
        join (partial s/join ",")
        ->? #(repeat (len %) \?)]
    (map (comp join ->?) param-values)))

(defn consv-nonempty
  [item coll]
  (if (empty? item)
    (vec coll)
    (vec (cons item coll))))

(defn- query-vec
  "Forms a query vector of the form [sql params...] to be
  used directly in jdbc/query"
  ([sql param-hash opts]
   (query-vec regex-parser sql param-hash opts))
  ([parser sql param-hash opts]
     (let [[splitted-sql placeholders] (parser sql)
           param-values (substitute-with param-hash placeholders)
           sql-with-? (s/join (interleave splitted-sql
                              (concat (->question-marks param-values) [nil]))) ;concat to make the seqs of equal length
           qv-with-params (if (empty? param-hash) ; do it that late to check for unmatched params
                              [sql]
                              (into [sql-with-?] (flatten param-values)))
           qv-opts (select-keys opts [:return-keys :result-type :concurrency
                                      :cursors :fetch-size :max-rows])]
       (consv-nonempty qv-opts qv-with-params))))

(defn- result-set-opts
  [opts]
  (let [opts-keys [:result-set-fn :as-arrays? :row-fn :identifiers]]
  ((comp flatten vec) (select-keys opts opts-keys))))

;; TODO: Add possibility for custom parser
(defn query
  ([db-spec sql]
   (query db-spec sql nil))
  ([db-spec sql default-opts]
   (fn query*
     ([]
      (query* nil nil))
     ([params]
      (query* params nil))
     ([params opts]
      (let [final-opts (merge default-opts opts)
            rs-opts (result-set-opts final-opts)
            qvec+opts (query-vec sql params final-opts)]
        `(clojure.java.jdbc/query ~db-spec ~qvec+opts ~@rs-opts))))))

(defmacro defquery
  ([query-name db-spec sql]
   `(def ~query-name (query ~db-spec ~sql)))
  ([query-name db-spec sql opts]
   `(def ~query-name (query ~db-spec ~sql ~opts))))

((query 'dbs "select a = :a and b in (:b)" {:fetch-size 100})
 {:a 1 :b [2 3]}
 {:row-fn 'vec}
 )
