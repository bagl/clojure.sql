(ns clojure.sql.util
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
  (let [regex #"(?<=[^:]):([\w-]+)"
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
    (if (= pars-key-set phs-key-set)
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

(defn list-files
  "List files in dir recursively which ends with the extension 'ext'"
  [dir-path ext]
  (filter #(and (.isFile %)
                (.endsWith (.getName %) ext))
          (file-seq (clojure.java.io/file dir-path))))
 
(defn sql-files-seq
  "Returns a vector of query name and filepath to the SQL file"
  [dir-path]
  (let [strip-extension #(.substring % 0 (- (count %) (count ".sql")))
        replace-underscores #(s/replace % #"_" "-")
        format-name (comp replace-underscores strip-extension)]
    (->> (list-files dir-path ".sql")
         (map #(list (format-name (.getName %)) (.getPath %))))))
