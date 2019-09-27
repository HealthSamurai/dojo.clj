(ns app.db.model-test
  (:require [app.db.model :as sut]
            [re-frame.core :as rf]
            [matcho.core :as matcho]
            [clojure.test :refer :all]))

(defn not-empty? [x] (not (empty? x)))
(defn not-nil? [x] (not (nil? x)))

(deftest db-test
  (rf/dispatch [:db/index :init {}])

  @server/app-db

  (def page (rf/subscribe [:db/index]))

  (matcho/match
   @page
   {:tables {:columns not-empty?
             :rows [{:id not-nil? :vals [{:id not-nil? :value not-nil?}]}]}}
   )


 )

