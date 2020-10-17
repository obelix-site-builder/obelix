(ns obelix.plugins.filesystem
  (:require ["js-yaml" :as yaml]
            fs
            path))

(def frontmatter-marker "---\n")

(defn content-and-frontmatter
  "Returns the contents of `file`, parsing frontmatter if it exists."
  [file]
  (let [content (fs/readFileSync file)]
    (if (= frontmatter-marker (-> content
                                  (.subarray 0 4)
                                  (.toString "utf-8")))
      (let [frontmatter-end (.indexOf content
                                      frontmatter-marker
                                      (count frontmatter-marker))]
        (if (not= frontmatter-end -1)
          {:content (.subarray content
                               (+ frontmatter-end (count frontmatter-marker)))
           :metadata (-> content
                         (.subarray (count frontmatter-marker)
                                    frontmatter-end)
                         (.toString "utf-8")
                         (yaml/safeLoad)
                         (js->clj :keywordize-keys true))}
          {:content content
           :metadata {}}))
      {:content content
       :metadata {}})))

(defn walk-files
  "Transforms the files in `src` into a routes list."
  [src path]
  (let [path (path/resolve src path)
        stat (fs/statSync path)]
    (cond
      (.isDirectory stat) (mapcat (partial walk-files src)
                                  (map (partial path/resolve path)
                                       (fs/readdirSync path)))
      (.isFile stat) [(assoc (content-and-frontmatter path)
                             :type :asset
                             :name (path/relative src path))])))

(defn plugin
  "Reads raw data from the files in `src` into the site map."
  [{:keys [src]}]
  (fn [handler]
    (fn [site-map]
      (handler (update site-map :routes #(concat % (walk-files src "")))))))
