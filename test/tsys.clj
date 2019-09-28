(ns tsys
  (:require 
   [db.core :as dbc]
   [clojure.test :refer :all]))


(def *db (atom nil))

(defn ensure-db []
  (if-let [d @*db]
    d
    (let [db-spec (dbc/db-spec-from-env)
          db (dbc/connection db-spec)]
      (with-open [conn (:connection db)]
        (when-not (dbc/database-exists? db "test")
          (db.core/exec! db "create database test")))
      (reset! *db (dbc/datasource (assoc db-spec :database "test"))))))

(defn db [] @*db)

(comment
  @*db
  (ensure-db)
  )



