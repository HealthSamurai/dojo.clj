(ns user
  (:require [cider-nrepl.main]))

(defn start-nrepl []
  (println "Starting nrepl...")
  (cider-nrepl.main/init
   ["refactor-nrepl.middleware/wrap-refactor"
    "cider.nrepl/cider-middleware"]))

(defn -main [& args]
  (start-nrepl))
