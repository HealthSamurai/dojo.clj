(ns app.rest.model
  (:require
   [re-frame.core :as rf]
   [app.routes :refer [href]]))

(def page-key :rest/index)

(rf/reg-event-fx
 page-key
 (fn [{db :db} [_ phase params]]
   (cond
     (= :init phase)
     {:db (assoc db page-key
                 {:title "DB"})}

     (= :params phase)
     {:db db}

     (= :deinit phase)
     {:db db})))

(rf/reg-sub
 page-key
 (fn [db] (get db page-key)))

(comment
  (println "Here")

  (rf/dispatch [page-key :init {}])





  )
