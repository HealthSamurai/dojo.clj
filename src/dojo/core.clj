(ns dojo.core
  (:require [web.core]
            [db.core])
  (:gen-class))

(defn handler [ctx]
  (println "Here in context: " (:db ctx) " req: " (:request ctx))
  {:status 200 :body "Hello"})

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
