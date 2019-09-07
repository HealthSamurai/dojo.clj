(ns dojo.core
  (:require [web.core])
  (:gen-class))

(defn dispatch [ctx]
  (println "Here")
  {:status 200 :body "Hello"})

(defn start [cfg]
  (let [ctx (atom {:cfg cfg})
        web (web.core/start {:port 8887}
             (fn [req]
               (dispatch (assoc @ctx :request req))))]
    (swap! ctx assoc :web web)
    ctx))

(defn -main [& args]
  (start))

(comment
  (def ctx (start {}))

  ctx

  ((:web @ctx))

  )
