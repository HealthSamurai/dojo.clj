(ns app.db.model-test
  (:require [app.db.model :as sut]
            [re-frame.core :as rf]
            [matcho.core :as matcho]
            [clojure.test :refer :all]))

(defn not-empty? [x] (not (empty? x)))
(defn not-nil? [x] (not (nil? x)))

(deftest db-test

  (server/ensure-server)

  (rf/dispatch [:db/index :init {}])

  (def page (rf/subscribe [:db/index]))

  (matcho/match
   @page
   {:tables {:columns not-empty?
             :rows [{:id not-nil? :vals [{:id not-nil? :value not-nil?}]}]}}
   )


  (rf/dispatch [:db/index :params {:params {:q "pg_proc"}}])
  (matcho/match
   @page
   {:tables {:columns not-empty?
             :rows [{:id not-nil? :vals [{:value "pg_catalog.pg_proc"}]}]}}
   )

 )

