(ns app.events.model
  (:require
   [re-frame.core :as rf]
   [app.routes :refer [href]]))

(def page-key :events/index)

(rf/reg-event-fx page-key
 (fn [{db :db} [_ phase params]]
   (cond
     (= :init phase)
     {:db (assoc db page-key {:title "Events stream"
                              :items []})
      :socket {:uri "/events"
               :on-receive ::add-item}}

     (= :params phase)
     {:db db}

     (= :deinit phase)
     {:db db})))

(rf/reg-event-db
 ::add-item
 (fn [db [_ item]]
   (update-in db [page-key :items] conj item)))

(rf/reg-sub
 page-key
 (fn [db] (get db page-key)))

