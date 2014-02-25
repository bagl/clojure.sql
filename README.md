# Clojure.sql

Clojure library for making querying databases easier by writing your queries as SQL
and transforming them into clojure functions.

Everything is work in progress. This is my first clojure project so comments
are welcome. If you read the code (around 100 LOC) and have any recommendations
please share them.

## Usage

The library provides you with `query` function and 2 macros
(`defquery` and `defqueries-from-dir`).

### defquery

    clojure.sql.core/defquery
    ([query-name db-spec sql] [query-name db-spec sql opts])
    Macro
      Defines a query function using the given name

Macro defines a function for you that you can call with a map of parameters and
values. Parameters are in SQL written as keywords (such as `:myparam`).

```clojure
(require '[clojure.sql.core :refer [query defquery]])

(def db-spec {:subprotocol "postgresql"
              :subname "//127.0.0.1:5432/test"})


; define a query
(defquery material-on-stock             ; query name
          db-spec                       ; database specification
          ; SQL string
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

One can use the whole set of options for the jdbc/query see [](http://clojure.github.io/java.jdbc/index.html#clojure.java.jdbc/query), ie:

    :identifiers, :as-arrays?, :row-fn, and :result-set-fn
    :return-keys, :result-type, :concurrency, :cursors, :fetch-size, :max-rows

Two additional options can be supplied:

    :db-spec
    :parser

db-spec takes, well, db-spec or DB connection so you can use the query in
transaction, etc. Parser can be injected if you have any special needs with
regard to parsing the raw SQL and it's parameter placeholders.

Example of using options:

```clojure
(defquery valve-state
  db-spec
  "select datetime, tagname, value
  from discretehistory dh
  where timeResolution = :resolution
    and tagname in (:tags)"
  {:fetch-size 100 :as-arrays? true}) ; specify default options if any

(valve-state {:resolution 1000
              :tags ["HV1010_Open" "HV1011_Open"]}
             {:as-arrays? false}) ; now :as-arrays? overrides default
                                  ; from defquery, fetch-size is still 100
```

If you want to use SQL without parametrs but with query options, you must
supply nil instead of parameter hash:

```clojure
(defquery query-without-params "select * from tags")

(query-without-params nil {:as-arrays? true}) ; nil is important here!
```

### defqueries-from-dir

    clojure.sql.core/defqueries-from-dir
    ([db-spec dir-path])
    Macro
      Defines query functions from files of raw SQL in the directory given
      
      Query names will be the filenames with underscores '_' replaced with dashes '-'
      and without the '.sql' extension:
    
        'my_query.sql' file transforms into 'my-query' function


If you have this tree structure:

    resources
    └── sql
        ├── production
        │   ├── daily_production.sql
        │   └── quality_by_product.sql
        └── sales
            ├── customer_credit_levels.sql
            └── sales_by_salespersons.sql


You can run `defqueries-from-dir` and get following queries:

```clojure
(defqueries-from-dir db-spec "resources/sql/")

(daily-production ...)
(quality-by-product ...)
(customer-credit-levels ...)
(sales-by-salespersons ...)
```

### Additional options
