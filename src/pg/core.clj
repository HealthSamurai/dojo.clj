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
              col (when (> len 0) (let [c (byte-array len)]
                                    (.get b c) c))]
          (recur (dec x)
                 (if col
                   (conj res col)
                   res)))
        res))))


(def statuses
  {\I :idle
   \T :transaction
   \E :failed-tx})

(defn parse-ready-for-query [b]
  ;; Byte1('Z')
  ;; Identifies the message type. ReadyForQuery is sent whenever the backend is ready for a new query cycle.

  ;; Int32(5)
  ;; Length of message contents in bytes, including self.

  ;; Byte1
  ;; Current backend transaction status indicator. Possible values are 'I' if
  ;; idle (not in a transaction block); 'T' if in a transaction block; or 'E' if
  ;; in a failed transaction block (queries will be rejected until block is
  ;; ended).

  (let [status (get statuses (char (.get b)))]
    {:type :ready-for-query
     :status status}))


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

(defn parse-query [b]
  (let [query (read-cstring b)]
    {:type :query
     :query query}))

(def error-fields
  {
   \S :local-severity
   ;; Severity: the field contents are ERROR, FATAL, or PANIC (in an error
   ;; message), or WARNING, NOTICE, DEBUG, INFO, or LOG (in a notice message),
   ;; or a localized translation of one of these. Always present.
   \V :severity
   ;; Severity: the field contents are ERROR, FATAL, or PANIC (in an error
   ;; message), or WARNING, NOTICE, DEBUG, INFO, or LOG (in a notice message).
   ;; This is identical to the S field except that the contents are never
   ;; localized. This is present only in messages generated by PostgreSQL
   ;; versions 9.6 and later.

   \C :code
   ;; Code: the SQLSTATE code for the error (see Appendix A). Not localizable.
   ;; Always present.

   \M :message
   ;; Message: the primary human-readable error message. This should be accurate
   ;; but terse (typically one line). Always present.

   \D :details
   ;; Detail: an optional secondary error message carrying more detail about the
   ;; problem. Might run to multiple lines.

   \H :hint
   ;; Hint: an optional suggestion what to do about the problem. This is
   ;; intended to differ from Detail in that it offers advice (potentially
   ;; inappropriate) rather than hard facts. Might run to multiple lines.

   \P :position
   ;; Position: the field value is a decimal ASCII integer, indicating an error
   ;; cursor position as an index into the original query string. The first
   ;; character has index 1, and positions are measured in characters not bytes.

   \p :internal-position
   ;; Internal position: this is defined the same as the P field, but it is used
   ;; when the cursor position refers to an internally generated command rather
   ;; than the one submitted by the client. The q field will always appear when
   ;; this field appears.

   \q :internal-query
   ;; Internal query: the text of a failed internally-generated command. This
   ;; could be, for example, a SQL query issued by a PL/pgSQL function.

   \W :where
   ;; Where: an indication of the context in which the error occurred. Presently
   ;; this includes a call stack traceback of active procedural language
   ;; functions and internally-generated queries. The trace is one entry per
   ;; line, most recent first.

   \s :schema
   ;; Schema name: if the error was associated with a specific database object,
   ;; the name of the schema containing that object, if any.

   \t :table
   ;; Table name: if the error was associated with a specific table, the name of
   ;; the table. (Refer to the schema name field for the name of the table's
   ;; schema.)

   \c :column
   ;; Column name: if the error was associated with a specific table column, the
   ;; name of the column. (Refer to the schema and table name fields to identify
   ;; the table.)

   \d :data-type
   ;; Data type name: if the error was associated with a specific data type, the
   ;; name of the data type. (Refer to the schema name field for the name of the
   ;; data type's schema.)

   \n :constraint
   ;; Constraint name: if the error was associated with a specific constraint,
   ;; the name of the constraint. Refer to fields listed above for the
   ;; associated table or domain. (For this purpose, indexes are treated as
   ;; constraints, even if they weren't created with constraint syntax.)

   \F :file
   ;; File: the file name of the source-code location where the error was
   ;; reported.

   \L :line
   ;; Line: the line number of the source-code location where the error was
   ;; reported.

   \R :routine
   ;; Routine: the name of the source-code routine reporting the error.
   })


(defn parse-error [b]

  ;; ErrorResponse (B)
  ;; Byte1('E')
  ;; Identifies the message as an error.

  ;; Int32
  ;; Length of message contents in bytes, including self.

  ;; The message body consists of one or more identified fields, followed by a
  ;; zero byte as a terminator. Fields can appear in any order. For each field
  ;; there is the following:

  ;; Byte1
  ;; A code identifying the field type; if zero, this is the message terminator
  ;; and no string follows. The presently defined field types are listed in
  ;; Section 52.8. Since more field types might be added in future, frontends
  ;; should silently ignore fields of unrecognized type.

  ;; String
  ;; The field value.

  (let [error (loop [acc {}]
                (let [code (.get b)]
                  (if (= 0 code)
                    acc
                    (recur (assoc acc (get error-fields (char code)) (read-cstring b))))))]
    {:type :error
     :error error}))


(defn  parse-parameter-status [b]
  ;; ParameterStatus (B)
  ;; Byte1('S')
  ;; Identifies the message as a run-time parameter status report.

  ;; Int32
  ;; Length of message contents in bytes, including self.

  ;; String
  ;; The name of the run-time parameter being reported.

  ;; String
  ;; The current value of the parameter.
  (let [params (loop [param {}]
                 (if (> (.remaining b) 0)
                   param
                   (let [k (read-cstring b)
                         v (read-cstring b)]
                     (recur (assoc param k v)))))]

    {:params params}))


(def parsers
  { \T parse-row-desc
   \D parse-data
   \Z parse-ready-for-query
   \Q parse-query
   \C parse-close
   \E parse-error
   \S parse-parameter-status})

(defn parse-msg [b]
  (let [tp  (char (.get b))
        len (.getInt b)
        prs (get parsers tp)]
    (if prs
      (prs b)
      (println "No parser for " tp))))

(defn read-msg [ch b]
  (.clear b)
  @(.read ch b)
  (.flip b))

(defn query [conn wb query]
  @(.write conn (query-msg query))
  (read-msg conn wb)
  (loop []
    (when (> (.remaining wb) 0)
      (println (parse-msg wb))
      (recur))))

(comment
  (def conn (connect {:db "postgres"
                      :user "postgres"
                      :host "localhost"
                      :port 5439
                      :password "postgres"}))

  (.close conn)

  conn

  (.isOpen conn)

  (def r (buf 100000))

  (.clear r)

  (query conn r "select 1; select 'a', 'b'")


  (doseq [x (range 100)]
    (println ">> " x)
    (query conn r "Select 1")

    (query conn r "Select ups"))

  (query conn r "Select * from information_schema.tables")

  (query conn r "Select * from generate_series(0,10000) i, information_schema.tables")

  r

  (.clear r)

  (parse-msg r)

  )


