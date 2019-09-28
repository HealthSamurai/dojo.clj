(ns dojo.core
  (:require [web.core]
            [db.core]
            [route-map.core :as route-map])
  (:gen-class))

(defn tables-ctl
  [{db :db {{q :q} :params} :request}]

  (let [tbls (db.core/query db ["
SELECT
    table_schema || '.' ||  table_name as table_name
    , pg_size_pretty(total_bytes) AS total
    , pg_size_pretty(index_bytes) AS index
    , pg_size_pretty(toast_bytes) AS toast
    , pg_size_pretty(table_bytes) AS table
  FROM (
  SELECT *, total_bytes-index_bytes-COALESCE(toast_bytes,0) AS table_bytes FROM (
      SELECT c.oid,nspname AS table_schema, relname AS TABLE_NAME
              , c.reltuples AS row_estimate
              , pg_total_relation_size(c.oid) AS total_bytes
              , pg_indexes_size(c.oid) AS index_bytes
              , pg_total_relation_size(reltoastrelid) AS toast_bytes
          FROM pg_class c
          LEFT JOIN pg_namespace n ON n.oid = c.relnamespace
          WHERE relkind = 'r' AND relname ilike ?
  ) a
) a
ORDER BY total_bytes desc
LIMIT 30;
" (str "%" q "%")])]
    {:status 200
     :body tbls}))

(defn dbs-ctl
  [{db :db {{q :q} :params} :request}]
  {:status 200
   :body (db.core/query db "select * from pg_database order by datname")})

(def routes
  {:GET (fn [_] {:status 200 :body "Hello"})
   "db" {"tables" {:GET tables-ctl}
         "dbs" {:GET dbs-ctl}}})


(defn handler [{req :request :as ctx}]
  (let [route   (route-map/match [(or (:request-method req) :get) (:uri req)] routes)]
    (if-let [handler (:match route)]
      (handler ctx)
      {:status 404
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

(defn db-from-env []
  (db.core/db-spec-from-env))

(defn -main [& args]
  (start {:db (db-from-env) 
          :web {}}))

(comment
  (def ctx (start {:db (db.core/db-spec-from-env)
                   :web {}}))

  (:db @ctx)

  (dispatch ctx {:uri "/"})
  (dispatch ctx {:uri "/db/tables" :params {:q "class"}})

  (stop ctx)

  


  )
