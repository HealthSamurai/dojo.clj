(ns atomix.config)

(def *state (atom nil))

(defn read-config
  "return edn config loaded from file"
  [conf-name]
  (-> conf-name slurp clojure.edn/read-string))

(comment
  )