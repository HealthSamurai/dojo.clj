(ns app.events.core
  (:require [re-frame.core :as rf]
            [app.routes  :refer [href]]
            [app.styles  :as styles]
            [reagent.core :as r]
            [app.pages   :as pages]
            [app.helpers :as helpers]
            [app.events.model :as model]))

(def styles
  (styles/styles
   [:div#events
    ]))

(defn index [params]
  (let [m (rf/subscribe [model/page-key])]
    (fn []
      (let [*m @m]
        [:div#events.centered-content
         styles
         [:h1 "EVENTS:"]
         [:pre (pr-str *m)]]))))

(pages/reg-page model/page-key index)
