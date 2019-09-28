(ns atomix.config
  (:import [io.atomix.protocols.raft.storage RaftStorage]
           (io.atomix.storage StorageLevel)
           (io.atomix.core AtomixBuilder Atomix)
           (io.atomix.cluster.discovery BootstrapDiscoveryProvider BootstrapDiscoveryBuilder)
           (io.atomix.cluster Node MemberId)
           (java.util.concurrent CompletableFuture)))

(defonce *node-conf1 (atom nil))

(defonce *node-conf2 (atom nil))

(defonce *node-conf3 (atom nil))

(defn read-config
  "return edn config loaded from file"
  [conf-name]
  (-> conf-name slurp clojure.edn/read-string))

(defn storage
  "configure storage for atomix"
  [{:keys [storage/type storage/folder]}]
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

(defn create-discovery-provider
  "create discovery service for given cluster.
  provide this service for every node in a cluster
   return ^BootstrapDiscoveryBuilder instance with discovery service."
  ^BootstrapDiscoveryBuilder [cluster-node-list]
  (let [node-builder      (fn [host port] (-> (Node/builder)
                                            (.withAddress host port)
                                            .build))
        cluster-nodes     (reduce (fn [acc next-node]
                                    (conj acc (node-builder (:host next-node) (:port next-node))))
                            [] cluster-node-list)]
    (.build (.withNodes (BootstrapDiscoveryProvider/builder) cluster-nodes))))


(defn create-node
  "create Atomix node.
  return ^Atomix instance, ready to start."
  ^Atomix
  [{:keys [node/id node/address cluster/nodes]}]
  (-> (Atomix/builder)
    (.withMemberId id)
    (.withAddress (:host address) (:port address))
    (.withMembershipProvider (create-discovery-provider nodes))
    .build))



(comment
  (reset! *node-conf1 (read-config "dev/atomix-conf1.edn"))
  (reset! *node-conf2 (read-config "dev/atomix-conf2.edn"))
  (reset! *node-conf3 (read-config "dev/atomix-conf3.edn"))


  (def node1 (create-node @*node-conf1))
  (def node2 (create-node @*node-conf2))
  (def node3 (create-node @*node-conf3))

  (future (.join @(.start node1)))
  (future (.join @(.start node2)))
  (future (.join @(.start node3)))

  (.subscribe  (.getCommunicationService node2) "subj" (reify java.util.function.Function
                                                         (apply [this msg]
                                                           (CompletableFuture/completedFuture msg))))
  (.thenAccept
    (.send (.getCommunicationService node1) "subj" "hello frome node-1" (MemberId/from "node-1"))
    )


  )



