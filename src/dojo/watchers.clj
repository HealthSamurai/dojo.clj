(ns dojo.watchers)


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


