(ns obelix.plugins.filesystem
  (:require fs
            path))

(defn walk-files
  "Transforms the files in `src` into a routes list."
  [src path]
  (let [path (path/resolve src path)
        stat (fs/statSync path)]
    (cond
      (.isDirectory stat) (mapcat (partial walk-files src)
                                  (map (partial path/resolve path)
                                       (fs/readdirSync path)))
      (.isFile stat) [{:type :asset
                       :name (path/relative src path)
                       :content (fs/readFileSync path)}])))

(defn plugin
  "Reads raw data from the files in `src` into the site map."
  [{:keys [src]}]
  (fn [handler]
    (fn [site-map]
      (handler (update site-map :routes #(concat % (walk-files src "")))))))
