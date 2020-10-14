(ns anbaric.util)

(defn map-routes
  "Maps the function `f` over all nodes in the `routes` list."
  [f routes]
  (map (fn mapper [node]
         (let [node (f node)]
           (if (:children node)
             (assoc node :children (map mapper (:children node)))
             node)))
       routes))
