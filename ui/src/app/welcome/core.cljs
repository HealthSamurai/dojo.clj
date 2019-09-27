(ns app.welcome.core
  (:require [re-frame.core :as rf]
            [app.routes  :refer [href]]
            [app.styles  :as styles]
            [reagent.core :as r]
            [app.pages   :as pages]
            [app.helpers :as helpers]
            [app.welcome.model :as model]))

(def styles
  (styles/styles
   [:div#welcome
    [:.card {:width "30rem"
             :margin "1em"
             :display "inline-block"}]]))

(defn index [params]
  (let [m (rf/subscribe [:welcome/index])]
    (fn []
      (let [*m @m]
        [:div#welcome.centered-content
         styles
         (for [b (:blocks *m)]
           [:div.card {:key (:id b)}
            [:h3 (:title b)]])]))))

(pages/reg-page :welcome/index index)
