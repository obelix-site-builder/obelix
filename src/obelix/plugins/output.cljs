(ns obelix.plugins.output
  (:require fs
            path
            [hiccups.runtime :as hiccup]))

(defn render-node
  "Recursively renders a node in the `:routes` map into the `out` directory."
  [out node]
  (cond
    (:content node) (fs/writeFileSync (path/resolve out (:name node))
                                      (:content node))
    (= (:type node) :directory) (do
                                  (fs/mkdirSync (path/resolve out (:name node)))
                                  (doseq [child (:children node)]
                                    (render-node (path/join out (:name node)) child)))))

(defn render-site
  "Renders the site in `site-data` to the `out` directory."
  [out site-data]
  (fs/rmdirSync (path/resolve out) #js {:recursive true})
  (fs/mkdirSync (path/resolve out))
  (doseq [node (:routes site-data)]
    (render-node out node)))

(defn plugin
  "Writes the site to the output directory `out`."
  [{:keys [out]}]
  (fn [handler]
    (fn [site-data]
      (let [site-data (handler site-data)]
        (render-site out site-data)
        (assoc-in site-data [:metadata :rendered] out)))))
