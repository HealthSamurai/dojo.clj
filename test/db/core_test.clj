(ns db.core-test
  (:require [db.core :as sut]
            [matcho.core :as matcho]
            [clojure.test :refer :all]))

(deftest db.core-test
  (def db-spec (sut/db-spec-from-env))
  db-spec

  (defonce db (sut/connection db-spec))

  (matcho/match
   (sut/query db "select 1 a")
   [{:a 1}])

  (matcho/match
   (sut/query-first db "select 1 a")
   {:a 1})

  (matcho/match
   (sut/query-value db "select 1 a")
   1)

  (sut/table-exists? db "users")
  (sut/table-exists? db "informational_schema.tables")

  (sut/exec! db
             "
drop table if exists tests;
create table tests (
 id serial primary key,
 resource jsonb
);

")

  (sut/insert db {:table :tests} {:id 1 :resource {:name "John"}})

  (sut/update db {:table :tests} {:id 1 :resource {:name "Ivan"}})

  (matcho/match
   (sut/query db {:select [:*] :from [:tests]})
   [{:id 1 :resource {:name "Ivan"}}])

  )

