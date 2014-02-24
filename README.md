# Clojure.sql

Clojure library for making querying databases easier by writing your queries as SQL
and transforming them into clojure functions.

Everything is work in progress. This is my first clojure project so comments
are welcome. Read the code if possible, it's under 100 LOC.

## Usage

```clojure
(require '[clojure.sql.core :as sql])

(def db-spec {:subprotocol "postgresql"
              :subname "//127.0.0.1:5432/test"})


; define a query
(defquery material-on-stock             ; name
          db-spec                       ; database specification
          ; SQL string
          ; parameters are written as :keywords
          "select part_no, quantity     
           from inventory_stock_levels
           where part_no in (:part-no)  
              and warehouse = :warehouse
           order by :part-no")

; use your defined query
; parameters are given in a map
(material-on-stock {:part-no [100024040 100024041] ; one can give multiple values
                    :warehouse "CC110"})
```

One can use the whole set of options for the jdbc/query such as this:
```clojure
(defquery valve-state
  db-spec
  "select datetime, tagname, value
  from discretehistory dh
  where timeResolution = :resolution
    and tagname in (:tags)"
  {:fetch-size 100 :as-arrays? true})

(valve-state {:resolution 1000
              :tags ["HV1010_Open" "HV1011_Open"]}
             {:as-arrays? false}) ; now :as-arrays? overrides default
                                  ; from defquery, fetch-size is still 100
```

If you want to use SQL without parametrs but with query options, you must
supply nil instead of parameter hash. Now we will use the query function on
the fly instead of defining named query:

```clojure
((query "select * from tags")
  nil
  {:as-arrays? true})
```
