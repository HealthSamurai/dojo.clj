(ns app.welcome.model
  (:require
   [re-frame.core :as rf]
   [app.routes :refer [href]]))

(def page-key :welcome/index)

(rf/reg-event-fx
 page-key
 (fn [{db :db} [_ phase params]]
   (cond
     (= :init phase)
     {:db (assoc db page-key
                 {:title "Dashboard"
                  :blocks [{:id "db" :title "DB"}
                           {:id "rest" :title "REST"}
                           {:id "k8s" :title "K8S"}]})}

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
