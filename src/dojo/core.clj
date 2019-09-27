(ns dojo.core
  (:require [web.core]
            [db.core]
            [route-map.core :as route-map])
  (:gen-class))

(defn tables-ctl [{db :db}]
  (let [tbls (db.core/query db "select * from information_schema.tables")]
    {:status 200
     :body tbls}))

(def routes
  {:GET (fn [_] {:status 200 :body "Hello"})
   "db" {"tables" {:GET tables-ctl}}})

(defn do-format [resp]
  (if-let [b (:body resp)]
    (-> 
     resp
     (assoc :body (cheshire.core/generate-string b))
     (assoc-in [:headers "content-type"] "application/json"))
    resp))

(defn handler [{req :request :as ctx}]
  (let [route   (route-map/match [(or (:request-method req) :get) (:uri req)] routes)]
    (if-let [handler (:match route)]
      (-> (handler ctx)
          (do-format)
          )
      {:status 200
       :body (str [(or (:request-method req) :get) (:uri req)] "not found" route)})))

(defn start [cfg]
  (let [ctx (atom {:cfg cfg})
        db (when (:db cfg) (db.core/datasource (:db cfg)))
        _ (swap! ctx assoc :db db)
        disp (fn [req] (handler (assoc @ctx :request req)))
        _ (swap! ctx assoc :dispatch disp)
        web (when (:web cfg) (web.core/start {:port 8887} disp))
        _ (swap! ctx assoc :web web)]
    ctx))

(defn stop [ctx]
  (try
    (when-let [srv (:web @ctx)] (srv))
    (catch Exception e))
  (try 
    (when-let [db (:db @ctx)] (db.core/shutdown db))
    (catch Exception e)))

(defn dispatch [ctx req]
  ((:dispatch @ctx) req))

(defn -main [& args]
  (start {:db (db.core/db-spec-from-env)
          :web {}}))

(comment
  (def ctx (start {:db (db.core/db-spec-from-env)
                   :web {}}))

  (dispatch ctx {:uri "/"})

  (stop ctx)


  )
