(ns server
  (:require [dojo.core]
            [cheshire.core :as json]
            [re-frame.core :as rf]
            [re-frame.interop]
            [re-frame.router]
            [re-test]
            [clojure.string :as str])
  (:import java.io.PipedInputStream))

(def app-db re-test/app-db)

(defn reset-db []
  (reset! re-frame.db/app-db {}))


(defonce *server (atom nil))


(defn ensure-server []
  (when-not @*server
    (reset! *server (dojo.core/start {:db (dojo.core/db-from-env)}))))

(defn stop []
  (when-let [srv @*server]
    (dojo.core/stop srv)))

(defn dispatch [req]
  (let [{disp :dispatch} @@*server]
    (disp req)))

(defn json-fetch [{:keys [uri token headers is-fetching-path params success error] :as opts}]
  (if (vector? opts)
    (doseq [o opts] (json-fetch o))
    (let [_ (println "REQ:" (or (:method opts) :get) (:uri opts) (when-let [p (:params opts)] (str "?" p)))
          headers (cond-> {"accept" "application/json"}
                    token (assoc "authorization" (str "Bearer " token))
                    (nil? (:files opts)) (assoc "Content-Type" "application/json")
                    true (merge (or headers {})))
          request (-> opts
                      (dissoc :method)
                      (dissoc :body)
                      (assoc :resource (:body opts))
                      (assoc :headers headers)
                      (assoc :query-string (str/join "&" (map (fn [[k v]] (str (name k) "=" v)) (:params opts)))) ;; FIXME: Probably duplicate
                      (assoc :request-method
                             (if-let [m (:method opts)]
                               (keyword (str/lower-case (name m)))
                               :get)))
          resp (dispatch request)]
      (println "RESP:" resp)
      (if (< (:status resp) 299)
        (if-let [ev (:event success)]
          (rf/dispatch [ev (merge success {:request opts :response resp :data (:body resp)})])
          (throw (Exception. (str "[:success :event] is not provided: " opts))))
        (if-let [ev (:event error)]
          (rf/dispatch [ev (merge error {:request opts :response resp :data (:body resp)})])
          (throw (Exception. (str "[:error :event] is not provided: " opts))))))))

(def browser (atom {}))

(rf/reg-fx :json/fetch json-fetch)
(rf/reg-fx :zframes.redirect/redirect
           (fn [opts] (swap! browser assoc :location opts)))


(comment
  (ensure-server)
  (stop)

  (dojo.core/db-from-env)

  (reset! *server nil)

  @*server

  (dispatch {:uri "/db/tables"})
  (dispatch {:uri "/ups"})


  (def srv 
    (dojo.core/start {:db (dojo.core/db-from-env)
                      :web {}}))


  (dojo.core/stop srv)


  )
