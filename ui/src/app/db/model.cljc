(ns app.db.model
  (:require
   [re-frame.core :as rf]
   [app.routes :refer [href]]))

(def page-key :db/index)

(rf/reg-event-fx
 page-key
 (fn [{db :db} [_ phase params]]
   (cond
     (= :init phase)
     {:db (assoc db page-key
                 {:title "DB"})
      :json/fetch {:uri "/db/tables"
                   :success {:event :tables/loaded}}}

     (= :params phase)
     {:db db}

     (= :deinit phase)
     {:db db})))

(rf/reg-sub
 page-key
 (fn [db] (get db page-key)))

(rf/reg-event-db
 :tables/loaded
 (fn [db [_ {data :data}]]
   (println data)
   (assoc-in db [page-key :tables] data)))

(comment
  (println "Here")

  (rf/dispatch [page-key :init {}])





  )
