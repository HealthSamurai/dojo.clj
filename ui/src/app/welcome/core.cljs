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
    [:.item {:display "block"
             :cursor "pointer"}
     [:table {:width "100%"}]
     [:td {:vertical-align "top"
           :border "1px solid #ddd"}]
     [:pre {:padding "20px" 
            :background-color "#f9f9f9"}]
     [:.title {:padding "5px 10px"
               :border-bottom "1px solid #ddd"}
      [:.dim {:font-size "10px" :color "gray"}]
      [:&:hover {:background-color "#f9f9f9"}]]]]))


(defn index [params]
  (let [m (rf/subscribe [:welcome/index])]
    (fn []
      (let [*m @m]
        [:div#welcome.centered-content
         styles
         [:h1 "Hello"]]))))

(pages/reg-page :welcome/index index)
