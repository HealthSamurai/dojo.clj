(ns atomix.config
  (:import [io.atomix.protocols.raft.storage RaftStorage]
           (io.atomix.storage StorageLevel)))

(def *node-conf1 (atom nil))

(def *node-conf2 (atom nil))

(def *node-conf3 (atom nil))

(defn read-config
  "return edn config loaded from file"
  [conf-name]
  (-> conf-name slurp clojure.edn/read-string))

(defn storage
  "configure storage for atomix"
  [{:keys [:storage/type :storage/folder]}]
  (case type
    :disk (-> (RaftStorage/builder)
              (.withDirectory folder)
              (.withStorageLevel (StorageLevel/DISK))
              (.build))
    :mmap-file (-> (RaftStorage/builder)
                   (.withDirectory folder)
                   (.withStorageLevel (StorageLevel/MAPPED))
                   (.build))
    :memory (-> (RaftStorage/builder)
                (.withStorageLevel (StorageLevel/MEMORY))
                (.build))))

(comment
  )