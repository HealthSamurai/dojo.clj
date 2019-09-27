(ns app.rest.core
  (:require [re-frame.core :as rf]
            [app.routes  :refer [href]]
            [app.styles  :as styles]
            [reagent.core :as r]
            [app.pages   :as pages]
            [app.helpers :as helpers]
            [app.rest.model :as model]))

(def styles
  (styles/styles
   [:div#rest
    ]))

(defn index [params]
  (let [m (rf/subscribe [model/page-key])]
    (fn []
      (let [*m @m]
        [:div#rest.centered-content
         styles
         [:h1 "REST"]
         ]))))

(pages/reg-page model/page-key index)
