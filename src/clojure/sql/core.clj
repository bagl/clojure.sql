(ns clojure.sql.core
  (:require [clojure.java.jdbc]
            [clojure.sql.util :refer :all]))

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
      (let [final-opts (merge default-opts opts)
            db-spec (get final-opts :db-spec db-spec)
            parser (get final-opts :parser regex-parser)
            query-vec (query-vec (parser sql) params final-opts)]
        (apply (partial clojure.java.jdbc/query db-spec query-vec)
               (flatten (seq final-opts))))))))

(defmacro defquery
  "Defines a query function using the given name"
  ([query-name db-spec sql]
   `(def ~query-name (query ~db-spec ~sql)))
  ([query-name db-spec sql opts]
   `(def ~query-name (query ~db-spec ~sql ~opts))))
