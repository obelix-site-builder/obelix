(ns anbaric.plugins.renderer
  (:require fs
            path
            [hiccups.runtime :as hiccup]))

(defn render-node
  "Recursively renders a node in the `:routes` map."
  [out node current-path]
  (cond
    (and (:type node) (= (:type node) :page))
    (fs/writeFileSync (str (apply path/resolve out current-path) ".html")
                      (str
                       "<!DOCTYPE html>\n"
                       (hiccup/render-html `[:html ~@(:content node)])))
    (and (:type node) (= (:type node) :asset))
    (fs/writeFileSync (apply path/resolve out current-path) (:data node))
    (map? node) (do
                  (fs/mkdirSync (apply path/resolve out current-path))
                  (doseq [path-item (keys node)]
                    (render-node out
                                 (get node path-item)
                                 (conj current-path (name path-item)))))))

(defn render-site
  "Renders the site in `site-data` to the `out` directory."
  [out site-data]
  (fs/rmdirSync (path/resolve out) #js {:recursive true})
  (render-node out (:routes site-data) []))

(defn plugin
  "Renders the site to the output directory `out`."
  [{:keys [out]}]
  (fn [handler]
    (fn [site-data]
      (let [site-data (handler site-data)]
        (render-site out site-data)
        (assoc-in site-data [:metadata :rendered] out)))))
