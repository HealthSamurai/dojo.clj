(ns user
  (:require [clojure.java.shell :as shell]
            [cider-nrepl.main]
            [figwheel-sidecar.repl-api :as repl]
            [build]
            [clojure.string :as str]))

(defn repl []
  (println "Starting nrepl...")
  (cider-nrepl.main/init
   ["cider.nrepl/cider-middleware"
    "cider.piggieback/wrap-cljs-repl"]))

(def dev-output-dir "build.dev/public")

(defn dev-build [opts]
  (merge-with
   merge
   {:source-paths ["src"]
    :compiler
    {:pretty-print  true
     :source-map true
     :asset-path "js"
     :output-dir (str dev-output-dir "/js")
     :optimizations :none}}
   opts))


(def figwheel-options
  {:figwheel-options {:builds-to-start ["app"]}
   :all-builds
   [(dev-build
     {:id "app"
      :compiler
      {:main "app.dev"
       :output-to (str dev-output-dir "/app.js")}})]})

(defn figwheel []
  (build/link-files dev-output-dir)
  (repl/start-figwheel! figwheel-options)

  (build/shell "rm -f build")
  (build/shell "ln -s build.dev build"))

(defn cljs-repl []
  (repl/cljs-repl))


(comment
  (figwheel)
  (repl/stop-figwheel!)

  (cljs-repl)
  )
