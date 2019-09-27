(ns dojo.watchers
  (:require [clj-yaml.core :as yaml]))

(defn start [ctx]
  ctx)

(defn dispatch [ctx ev]
  (doall
   (for [[k w] (:watchers ctx)]
     (when-let [h (and (or (nil? (:ev w))
                           (contains? (:ev w) (:ev ev)))
                       (:fn w))]
       (let [res (h ctx (assoc w :id k) ev)]
         (when (map? res)
           (doseq [[fx-name arg] res]
             (cond
               (= fx-name :notify)
               (when-let [h (get-in ctx [:effects :notify])]
                 (h ctx (merge (or (get w fx-name) {}) arg)))))))))))


(defn report-from-file [ctx filename]
  (with-open [rdr (clojure.java.io/reader filename)]
    (doseq [ev (->> (line-seq rdr)
                    (map yaml/parse-string)
                    (map #(update % :ev keyword)))]
      (dispatch ctx ev))))

(comment

  (def filename "/Users/vganshin/hs/logs/logs.ndjson")

  (report-from-file ctx filename)

  )
