# Clojure.sql

Clojure library for making querying databases easier by writing your queries as SQL
and transforming them into clojure functions.

## Usage

```clojure
(require '[clojure.sql.core :as sql])

(def db-spec {:subprotocol "postgresql"
              :subname "//127.0.0.1:5432/test"})


; define the query using database specification and SQL with placeholders (:keywords)
(defquery material-on-stock
          db-spec
          "select part_no, quantity
           from inventory_stock_levels
           where part_no in (:part-no)
              and warehouse = :warehouse
           order by :part-no")

; use your defined query
(material-on-stock {:part-no [100024040 100024041] :warehouse "CC110"})
```
