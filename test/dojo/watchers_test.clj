(ns dojo.watchers-test
  (:require [dojo.watchers :as sut]
            [clojure.test :refer :all]))

(defn conn-num [ctx opts ev]
  (let [limit (or (:max opts) 20)]
    (println "conn-num" (:conn_num ev) limit
             (> (:conn_num ev) limit))
    (when (> (:conn_num ev) limit)
      (println "Warn")
      {:notify {:id :pg/conn-num
                :msg (str "DB: Number of connections > " limit)}})))

(defn errs-w [ctx opts ev]
  (when (= "error" (:lvl ev))
    {:notify {:id (or (:errc ev) (:ev ev))
              :msg (str "Error " (:err ev))}}))


(defn notify [ctx msg]
  (println "NOTIFY:" msg))


(deftest test-watchers

  (def *db (atom []))

  (defn persist [ctx opts ev]
    (swap! *db conj ev))

  (def ctx
    {:watchers {:pg/conn-num {:ev #{:pg/conn-num}
                              :notify {:debounce :exp}
                              :fn conn-num}
                :persist {:fn persist}
                :errors  {:fn errs-w}}
     :effects {:notify notify}})


  (def sys (sut/start ctx))

  (sut/dispatch sys {:ev :pg/conn-num :conn_num 2})
  (sut/dispatch sys {:ev :pg/conn-num :conn_num 21})


  (sut/dispatch sys {:ev :db/q :lvl "error" :err "sql is wrong"})
  (sut/dispatch sys {:ev :w/resp :lvl "error" :err "Exception in response"})


  *db

  )



