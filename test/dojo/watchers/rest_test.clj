(ns dojo.watchers.rest-test
  (:require [dojo.watchers.rest :as sut]
            [tsys]
            [db.core :as dbc]
            [clojure.test :refer :all]))

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
   (doseq [i (range 100)]
     (doseq [d ["2019-09-25T16:40:54+00:00"
                "2019-09-26T16:40:54+00:00"
                "2019-09-27T16:40:54+00:00"]]
       (dbc/insert
        db {:table :rest_requests}
        {:ts d
         :meth "post"
         :uri "/Encounter"
         :d i})
       (dbc/insert
        db {:table :rest_requests}
        {:ts d
         :meth "get"
         :uri "/Patient"
         :d i}))))

  (dbc/query db "select * from rest_stats")

  (time (dbc/query db "select * from rest_daily_cnt"))


  )

