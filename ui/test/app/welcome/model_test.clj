(ns ui.welcome.model-test
  (:require [app.welcome.model :as model]
            [libox.test :as libox]
            [clojure.test :refer :all]
            [re-frame.core :as rf]
            [headless-server :as hs]
            [matcho.core :as matcho]
            [clojure.java.io :as io]))

(deftest test-welcome
  (hs/ensure-server)
  (hs/reset-db)

  (libox/truncate :IncommingClaims)

  (def claims-url (.getPath (io/resource "myapp/data/medical_claims.csv.gz")))

  (libox/match
   {:uri "/$import-claims/cigna"
    :request-method :post
    :resource {:url claims-url}}
   {:status 200})

  (libox/create {:id "inc-1"
                 :type "claim"
                 :source "source"
                 :resourceType "IncommingClaims"
                 :message {:a 1}})

  (rf/dispatch [:welcome/index :init {}])

  (def m (rf/subscribe [:welcome/index]))

  (matcho/match
   @m
   {:items [{:id "110001111"
             :title "Madella Marflitt"}]})

  )

