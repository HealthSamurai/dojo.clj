(ns app.welcome.model
  (:require
   [re-frame.core :as rf]
   [app.routes :refer [href]]))

(rf/reg-event-fx
 :welcome/index
 (fn [{db :db} [_ phase params]]
   (cond
     (= :init phase)
     {:db db}

     (= :params phase)
     {:db db}

     (= :deinit phase)
     {:db db})))

(rf/reg-sub
 :welcome/index
 (fn [db] {:title "Hello"}))
