(ns user
  (:require [clojure.java.shell :as shell]
            [dojo.core]
            [cider-nrepl.main]
            [figwheel.main.api]
            [build]
            [clojure.string :as str]))

(defn repl []
  (println "Starting nrepl...")
  (cider-nrepl.main/init
   ["cider.nrepl/cider-middleware"
    "cider.piggieback/wrap-cljs-repl"]))

(def dev-output-dir "build.dev/public")

(defn figwheel []
  (build/link-files dev-output-dir)
  (figwheel.main.api/start
   {:id "app"
    :options {:main 'app.dev
              :pretty-print  true
              :source-map true
              :asset-path "js"
              :output-to (str dev-output-dir "/app.js")
              :output-dir (str dev-output-dir "/js")
              :optimizations :none}
    :config {:mode :serve
             :open-url false
             :watch-dirs ["src"]}})

  (build/shell "rm -f build")
  (build/shell "ln -s build.dev build"))

(defn cljs-repl []
  (figwheel.main.api/cljs-repl "app"))

(defn server []
  (dojo.core/start {:db (dojo.core/db-from-env)
                    :web {}}))


(comment
  (figwheel)
  (repl/stop-figwheel!)

  (cljs-repl)

  (def srv (server))

  ;; run figwheel
  ;; run server
  ;; for ui open http://localhost:8887/static/index.html

  (dojo.core/stop srv)
  )
