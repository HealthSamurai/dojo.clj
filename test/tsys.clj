(ns tsys
  (:require 
   [db.core :as dbc]
   [dojo.core]
   [matcho.core]
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

(defonce *app (atom nil))
(defn ensure-app []
  (when-not @*app
    (ensure-db)
    (reset! *app
            (dojo.core/start {:db (assoc (dbc/db-spec-from-env) :database "test")}))))

(defn dispatch [req]
  (dojo.core/dispatch @*app req))

(defn match [req exp-resp]
  (let [resp (dispatch req)]
    (matcho.core/match resp exp-resp)
    resp))

(defn db [] @*db)

(comment
  @*db
  (ensure-db)
  (ensure-app)
  *app

  (match {:uri "/"}
         {:status 300})

  )



