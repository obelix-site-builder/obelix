(ns obelix.plugins.output
  (:require fs
            path
            [taoensso.timbre :as log]))

(defn write-node
  "Writes a node in the `:routes` map into the `out` directory."
  [out node]
  (if (or (not= (:type node) "data")
          (get-in node [:metadata :render]))
    (let [output-path (path/resolve out (:name node))]
      (log/debug "Writing output file" output-path)
      (fs/mkdirSync (path/dirname output-path) #js {:recursive true})
      (let [content (:content node)]
        (if content
          (do (fs/writeFileSync output-path content)
              (assoc-in node [:metadata :rendered] output-path))
          node)))
    node))

(defn write-site
  "Writes the site in `routes` to the `out` directory."
  [out routes]
  (fs/rmdirSync (path/resolve out) #js {:recursive true})
  (fs/mkdirSync (path/resolve out))
  (map (partial write-node out) routes))

(defn plugin
  "Writes the site to the output directory `out`."
  [{:keys [out]}]
  (fn [site-data]
    (log/debug "Writing output files")
    (-> site-data
        (update :routes (comp doall (partial write-site out)))
        (assoc-in [:metadata :rendered] out))))
