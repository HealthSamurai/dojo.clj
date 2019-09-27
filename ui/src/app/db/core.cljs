(ns app.db.core
  (:require [re-frame.core :as rf]
            [app.routes  :refer [href]]
            [app.styles  :as styles]
            [reagent.core :as r]
            [app.pages   :as pages]
            [app.helpers :as helpers]
            [app.db.model :as model]))

(def styles
  (styles/styles
   [:div#db
    ]))

(defn index [params]
  (let [m (rf/subscribe [model/page-key])]
    (fn []
      (let [*m @m]
        [:div#db.centered-content
         styles
         [:h1 "DB:"]
         [:input {:placeholder "search..."}]
         [:table.table
          (let [rows (:tables *m)
                cols (keys (first rows))]
            [:tbody 
             (for [r rows]
               (into
                [:tr {:key (:table_name r)}]
                (for [c cols]
                  [:td {:key c} (pr-str (get r c))]))

               )])]
         ]))))

(pages/reg-page model/page-key index)
