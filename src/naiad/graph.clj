(ns naiad.graph)

(def ^:dynamic *graph*)

(defprotocol INode
  (transducer? [this] "Returns true if this node can be a transducer")
  (as-transducer [this] "Create a transducer by supplying a reducer function")
  (validate [this])
  (construct [this]))

(defprotocol IToEdge
  (insert-into-graph [this graph] "Insert this non-channel edge as a new node in the graph"))



(deftype Id []
  Object
  (hashCode [this]
    (System/identityHashCode this))
  (toString [this]
    (str "ID<" (System/identityHashCode this) ">")))


(def id? (partial instance? Id))

(defn gen-id []
  (->Id))

(defmethod print-method Id
  [v ^java.io.Writer w]
  (.write w (str v)))

(gen-id)

(defn add-node [graph {:keys [id type] :as node}]
  (assert type (str "Node"  node " must have a type"))
  (let [id (or id (gen-id))]
    (assoc graph id (assoc node :id id))))

(defn add-node! [node]
  (set! *graph* (add-node *graph* node)))


(defn ports [graph type]
  (assert #{:inputs :outputs} type)
  (for [[id node] graph
        [port-name link] (type node)]
    {:node id
     :port port-name
     :link link}))

(defn validate-graph [graph]
  (doseq [[k node] graph]
    (validate node)))

(defn links-by-class [graph class-kw]
  (for [[id node] graph
        :let [inputs (class-kw node)]
        [name link] inputs]
    link))

(defn links [graph]
  (set (concat (links-by-class graph :inputs)
               (links-by-class graph :outputs)
               (links-by-class graph :closes))))

(defn edges [graph]
  (let [outputs (ports graph :inputs)
        inputs (group-by :link (ports graph :outputs))]
    (for [{:keys [link] :as out} outputs
          in (inputs link)]
      {:link link
       :out out
       :in in})))


(defn nodes-by-link [graph type link]
  (filter #(= (:link %) link)
    (ports graph type)))


(defrecord GraphSource [link-id extra-nodes])