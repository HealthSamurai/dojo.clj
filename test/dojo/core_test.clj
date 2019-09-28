(ns dojo.core-test
  (:require [dojo.core :as sut]
            [tsys]
            [clojure.test :refer :all]))

(deftest test-dojo
  (tsys/ensure-app)

  (tsys/match
   {:uri "/"}
   {:status 200})


  (tsys/match
   {:uri "/ups"}
   {:status 404})

  (tsys/match
   {:uri "/db/tables" :params {:q "class"}}
   {:status 200
    :body [{:table_name "pg_catalog.pg_class"}]})

  (tsys/match
   {:uri "/db/dbs"}
   {:status 200
    :body [{:datname "obscure"}]})

  (tsys/match
   {:uri "/db/dbs" :params {:q "post"}}
   {:status 200
    :body [{:datname "obscure"}]})



  )

