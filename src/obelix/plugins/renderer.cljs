(ns obelix.plugins.renderer
  (:require fs
            path
            [hiccups.runtime :as hiccup]))

(defn add-head
  "Adds the proper <meta> attributes to the <head> of `content`,
  creating it if necessary."
  [content]
  (let [head (or (first (filter #(= (first %) :head) content)) [:head])
        head (if (empty? (filter #(and (= (first %) :meta) (:charset (second %))) (rest head)))
               (conj head [:meta {:charset "UTF-8"}])
               head)
        head (if (empty? (filter #(and (= (first %) :meta) (= (:name (second %)) "viewport"))
                                 (rest head)))
               (conj head [:meta {:name "viewport"
                                  :content "width=device-width, initial-scale=1.0"}])

               head)]
    (into [head] (remove #(= (first %) :head) content))))

;; TODO update this to work with new routes shape

(defn render-node
  "Recursively renders a node in the `:routes` map."
  [out node current-path]
  (cond
    (and (:type node) (= (:type node) :page))
    (fs/writeFileSync (str (apply path/resolve out current-path) ".html")
                      (str
                       "<!DOCTYPE html>\n"
                       (hiccup/render-html `[:html ~@(add-head (:content node))])))
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
