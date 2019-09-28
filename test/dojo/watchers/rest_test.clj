(ns dojo.watchers.rest-test
  (:require [dojo.watchers.rest :as sut]
            [tsys]
            [db.core :as dbc]
            [clojure.test :refer :all]
            [clojure.string :as str]))


(deftest rest-watcher
  (def db (tsys/ensure-db))

  (dbc/exec! db "
create extension if not exists pipelinedb;
DROP VIEW IF EXISTS rest_stats;
DROP VIEW IF EXISTS rest_daily_cnt;
DROP FOREIGN TABLE IF EXISTS rest_requests;
CREATE FOREIGN TABLE
IF NOT EXISTS
rest_requests (
    ts timestamp,
    meth text,
    uri text,
    qs text,
    d  bigint
) SERVER pipelinedb;

CREATE VIEW rest_stats WITH (action=materialize) AS
SELECT
  ts::date as date,
  meth as meth,
  uri as uri,
  count(*) as num,
  avg(d) as avg_d,
  max(d) as max_d,
  min(d) as min_d
FROM rest_requests
GROUP BY
  ts::date,
  meth,
  uri
;


CREATE VIEW rest_daily_cnt WITH (action=materialize) AS
SELECT
  ts::date as date,
  count(*) as num
FROM rest_requests
GROUP BY
  ts::date
;

")

  (time
   (doseq [i (range 1000)]
     (dbc/insert
      db {:table :rest_requests}
      {:ts "2019-09-25T16:40:54+00:00"
       :meth "post"
       :uri "/Encounter"
       :d i})))



  (let [add-item (dbc/mk-copy db 1000 :rest_requests [:ts :meth :uri :qs :d])]
    (time
     (doseq [i (range 1001)]
       (add-item {:ts "2019-09-25T16:40:54+00:00" :meth "post" :uri "/Encounter" :d i}))))


  (dbc/query db "select * from rest_stats")

  (time (dbc/query db "select * from rest_daily_cnt"))

  )

