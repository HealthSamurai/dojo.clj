(ns dojo.reporters
  (:require [db.core]))


(defn sql-report [{db :db} {q :query :as opts}]
  (->
   (db.core/query-first db q)
   (assoc :ev (:id opts))
   (assoc :tgs (:tgs opts))))

(defn run [ctx]
  (doseq [[k opts] (:reporters ctx)]
    (println "* run " k)
    (cond
      (= :sql (:type opts))
      (println "SQL:" (sql-report ctx (assoc opts :id k)))
      :else (println "Unknown reporter" opts))))

(defn *start [ctx]
  (loop []
    (Thread/sleep 1000)
    (run ctx)
    (recur)))

(defn start [ctx]
  (let [db (db.core/datasource (get-in ctx [:config :db]))
        ctx (assoc ctx :db db)
        th (Thread. (fn [] (*start ctx))) ]
    (.setName th "reporters")
    (.start th)
    th))


(defn stop [state]
  (.stop state))

(defn stop-by-name [nm]
  (doseq [t (-> 
             (Thread/getAllStackTraces)
             .keySet)]
    (when (= nm (.getName t))
      (println "Interrupt " t)
      (.interrupt t))))
