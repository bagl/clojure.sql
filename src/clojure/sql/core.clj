(ns clojure.sql.core
  (:require [clojure.string :as s]
            [clojure.set :as c.set]))

(defn symmetric-diff
  "Returns a symmetric difference of two sets"
  [s1 s2]
  (c.set/difference (c.set/union s1 s2)
                    (c.set/intersection s1 s2)))

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
  (let [pars-key-set (set (keys param-hash))
        phs-key-set (set placeholders)]
    (if (and (every? pars-key-set placeholders)
             (every? phs-key-set (keys param-hash)))
      (map param-hash placeholders)
      (->> (symmetric-diff phs-key-set pars-key-set) ; builds and throws exception
        (format "Unmatched query parameters: %s")
        IllegalArgumentException.
        throw))))

(defn ->question-marks
  "Transforms one level nested collections into strings of
  question marks to be used in parametrized query"
  [param-values]
  (let [len #(if (coll? %) (count %) 1)
        join (partial s/join ",")
        ->? #(repeat (len %) \?)]
    (map (comp join ->?) param-values)))

(defn query-vec
  "Forms a query vector of the form [opts sql params...] to be
  used directly in jdbc/query"
  [parsed-sql-vec param-hash opts]
  (let [[splitted-sql placeholders] parsed-sql-vec
        param-values (substitute-with param-hash placeholders)
        sql-with-? (->> (concat (->question-marks param-values) [nil])
                        (interleave splitted-sql)
                        s/join)]
    (concat [(or opts {})] [sql-with-?] (flatten param-values))))

(defn query
  "Returns a query function with embedded sql, db-spec and options.
  db-spec and options can be overriden when calling the function"
  ([db-spec sql]
   (query db-spec sql nil))
  ([db-spec sql default-opts]
   (fn query*
     ([]
      (query* nil nil))
     ([params]
      (query* params nil))
     ([params opts]
      (let [parser (get default-opts :parser regex-parser)
            db-spec (get opts :db-spec db-spec)
            final-opts (merge default-opts opts)
            query-vec (query-vec (parser sql) params final-opts)]
        `(clojure.java.jdbc/query ~db-spec ~query-vec ~@(flatten (seq final-opts))))))))

(defmacro defquery
  "Defines a query function using the given name"
  ([query-name db-spec sql]
   `(def ~query-name (query ~db-spec ~sql)))
  ([query-name db-spec sql opts]
   `(def ~query-name (query ~db-spec ~sql ~opts))))

((query 'dbs "select"))
((query 'dbs "select :a" {:parser (fn [& all] [["ahoj"] []])}))
((query 'dbs "select a = :a and b in (:b)" {:fetch-size 100})
 {:a 1 :b [2 3]}
 {:row-fn 'vec})
