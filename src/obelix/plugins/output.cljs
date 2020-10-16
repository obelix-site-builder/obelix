(ns obelix.plugins.output
  (:require fs
            path))

(defn write-node
  "Writes a node in the `:routes` map into the `out` directory."
  [out node]
  (let [output-path (path/resolve out (:name node))]
    (fs/mkdirSync (path/dirname output-path) #js {:recursive true})
    (if (:content node)
      (do (fs/writeFileSync output-path (:content node))
          (assoc-in node [:metadata :rendered] output-path))
      node)))

(defn write-site
  "Writes the site in `routes` to the `out` directory."
  [out routes]
  (fs/rmdirSync (path/resolve out) #js {:recursive true})
  (fs/mkdirSync (path/resolve out))
  (map (partial write-node out) routes))

(defn plugin
  "Writes the site to the output directory `out`."
  [{:keys [out]}]
  (fn [handler]
    (fn [site-data]
      (let [site-data (handler site-data)]
        (-> site-data
            (update :routes (partial write-site out))
            (assoc-in [:metadata :rendered] out))))))
