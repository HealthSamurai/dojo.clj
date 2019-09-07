(ns app.organizations.form-test
  (:require [app.organizations.form :as sut]
            [libox.core :as libox]
            [headless-server :as hs]
            [re-frame.core :as rf]
            [clojure.test :refer :all]
            [matcho.core :as match]))


(defn input [path value]
  (rf/dispatch [:zf/set-value sut/form-path path value]))

(deftest test-org-form
  (hs/ensure-server)
  (libox/truncate :Organization)

  (is (empty? (libox/query "select * from organization")))

  (hs/reset-db)
  
  (rf/dispatch [:organizations/new :init {}])

  (def m (rf/subscribe [:organizations/new]))

  (input [:name] "Clinic 2")

  (rf/dispatch [::sut/submit])

  (is (= (:status @m) :error))
  
  (input [:name] "verylongnameveryvery")

  (rf/dispatch [::sut/submit])

  (is (empty? (:errors @m)))

  (first (libox/query "select * from organization"))

  )


(comment
  (libox/truncate :Organization)

  (time
   (doseq [i (range 100)]
     (libox/create {:resourceType "Organization"
                    :name (str "Organization 1")})))

  (libox/query "select count(*) from organization")



  )
