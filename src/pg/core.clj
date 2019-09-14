(ns pg.core
  (:require [promesa.core :as p])
  (:import [java.nio ByteBuffer]
           [java.net InetSocketAddress SocketAddress]
           [java.nio.channels CompletionHandler]
           [java.nio channels.AsynchronousSocketChannel]
           [java.nio.charset Charset]
           [java.security MessageDigest]))

(defn buf [step]
  (ByteBuffer/allocateDirect step))

(defn encode-hex [^bytes bs]
  (let [fmt (str "%0" (bit-shift-left (count bs) 1) "x")]
    (->> (BigInteger. 1 bs)
         (format fmt))))

(defn concat-ba [a b]
  (let [buf (byte-array (+ (count a) (count b)))]
    (System/arraycopy a 0 buf 0 (count a))
    (System/arraycopy b 0 buf (count a) (count b))
    buf))

(defn md5 [x]
  (let [d (MessageDigest/getInstance "MD5")]
    (.update d x)
    (-> d .digest
        encode-hex
        .getBytes)))

(defn password [user password salt]
  (md5 (concat-ba (md5 (concat-ba password user)) salt)))


(defn startup-msg [db user]
  (let [dbk (.getBytes "database")
        dbb (.getBytes db)
        usk (.getBytes "user")
        usb (.getBytes user)
        len (+ 4 4
               (count dbk) 1
               (count dbb) 1
               (count usk) 1
               (count usb) 1
               1)]
    (-> (buf len)
        (.putInt (int len))
        (.putShort (short 3))
        (.putShort (short 0))
        (.put dbk)
        (.put (byte 0))
        (.put dbb)
        (.put (byte 0))
        (.put usk)
        (.put (byte 0))
        (.put usb)
        (.put (byte 0))
        (.put (byte 0))
        (.flip))))

(defn md5-msg [user pwd salt]
  (let [secret (password (.getBytes user) (.getBytes pwd) salt)
        len (+ 1 4 3 (count secret) 1)]
    (-> (buf len)
     (.put (byte \p))
     (.putInt (int (dec len)))
     (.put (.getBytes "md5"))
     (.put secret)
     (.put (byte 0))
     (.flip))))

(def auth-msg-types
  {0 :auth-ok
   3 :text
   5 :md5})

(defn auth-msg [b]
  (let [tp (char (.get b))
        len (.getInt b)
        auth-tp (->> (.getInt b)
                     (get auth-msg-types))]
    (cond-> {:auth-type auth-tp
             :msg-type tp}
      (= :md5 auth-tp) (assoc :salt (let [msg (byte-array 4)] (.get b msg) msg)))))

(defn connect [cfg]
  (let [ch  (AsynchronousSocketChannel/open)]
    @(.connect ch (InetSocketAddress. (:host cfg) (:port cfg)))
    @(.write ch (startup-msg (:db cfg) (:user cfg)))
    (let [b (buf 1000)
          _ @(.read ch b)
          _ (.flip b)
          msg (auth-msg b)]
      (println "ath" msg)
      @(.write ch (md5-msg (:user cfg) (:password cfg) (:salt msg))))
    (let [b (buf 1000)
          _ @(.read ch b)
          _ (.flip b)]
      (println "Done " (auth-msg b)))
    ch))

(defn query-msg [sql]
  (let [bs (.getBytes sql)
        len (+ 5 (count bs))]
    (-> (buf (inc len))
        (.put (byte \Q))
        (.putInt len)
        (.put bs)
        (.put (byte 0))
        (.flip))))

(defn read-cstring [b]
  (let [pos (.position b)
        len (loop [i 0]
              (if (= (byte 0) (.get b))
                i
                (recur (inc i))))
        bs (byte-array len)
        _ (.position b pos)]
    (.get b bs)
    (.get b)
    (String. bs)))

(defn parse-row-desc [b]
  {:columns
   (loop [x (.getShort b)
          acc []]
     (if (> x 0)
       (->> {:col-name         (read-cstring b)
             :object-id        (.getInt b)
             :attribute-number (.getShort b)
             :type-object-id   (.getInt b)
             :type-size        (.getShort b)
             :type-mod         (.getInt b)
             :format-code      (.getShort b)}
            (conj acc)
            (recur (dec x)))
       acc))})


(defn parse-data [b]
  (let [cols-num (.getShort b)]
    (loop [x (int cols-num) res []]
      (if (> x 0)
        (let [len     (.getInt b)
              col (when (> len 0) (byte-array len))
              _ (.get b col)]
          (recur (dec x)
                 (if col
                   (conj res col)
                   res)))
        res))))


(defn parse-close [b]
  ;; Byte1('C')
  ;; Identifies the message as a Close command.

  ;; Int32
  ;; Length of message contents in bytes, including self.

  ;; Byte1
  ;; 'S' to close a prepared statement; or 'P' to close a portal.

  ;; String
  ;; The name of the prepared statement or portal to close (an empty string selects the unnamed prepared statement or portal).
  {:type :close
   :statement-name (read-cstring b)})

(def parsers
  { \T parse-row-desc
    \D parse-data
   ;; (byte \Z) parse-ready-for-query
   ;; (byte \S) parse-parameter-status
   ;; (byte \Q) parse-query
   ;; (byte \E) parse-error
   \C parse-close
   })

(defn parse-msg [b]
  (let [tp  (char (.get b))
        len (.getInt b)
        prs (get parsers tp)]
    (if prs
      (prs b)
      (println "No parser for " tp))))

(query-msg "Select * from informational_schema.tables")

(comment
  (def conn (connect {:db "postgres"
                      :user "postgres"
                      :host "localhost"
                      :port 5439
                      :password "postgres"}))

  (.close conn)

  conn

  (.isOpen conn)
  @(.write conn (query-msg "select 1 as a, 2 as b"))

  (def r 
    (let [b (buf 10000000)
          _ @(.read conn b)
          _ (.flip b)]
      b))

  (.position r 0)

  (println (parse-msg r))

  (String. (first m))
  (String. (second m))

  r

  (.getShort r)




  )


