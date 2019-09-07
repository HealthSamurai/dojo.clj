(ns app.organizations.model-test
  (:require [app.organizations.model :as model]
             [clojure.test :refer :all]
             [re-frame.core :as rf]
             [re-frame.db]
             [headless-server :as hs]
             [world :as world]
             [matcho.core :as matcho]))

(deftest test-organizations
  (reset! re-frame.db/app-db {})

  (rf/dispatch [:organizations/index :init {}])

  (def m (rf/subscribe [:organizations/new]))

  (matcho/match @m 1))

