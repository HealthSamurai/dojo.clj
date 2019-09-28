(ns dojo.reporters-test
  (:require [dojo.reporters :as sut]
            [db.core]
            [clojure.test :refer :all]))

(def db-size
  {:id :pg/db-size
   :type :sql
   :desc "Number of connections"
   :tgs #{:metrics :db :pg}
   :query "SELECT
current_database() as id,
pg_database_size(current_database()) as size,
pg_size_pretty( pg_database_size(current_database())) as display
;"})

(def db-conn
  {:id :pg/conn-num
   :desc "Number of connections"
   :type :sql
   :tgs #{:metrics :db :pg}
   :query "SELECT sum(numbackends) as num_conn FROM pg_stat_database"})


(deftest reporters-test
  (def reps
    {:config {:db (db.core/db-spec-from-env)}
     :reporters {:pg/db-size db-size
                 :pg/conn-num db-conn}})

  (get-in reps [:config :db])

  (def db (db.core/datasource (get-in reps [:config :db])))

  (sut/sql-report {:db db} (get-in reps [:reporters :pg/db-size]))


  (def sys (sut/start reps))
  sys

  (sut/stop-by-name "reporters")


  (sut/stop sys)


  )

