(ns obelix.plugins.filesystem
  (:require [clojure.string :as s]
            fs
            path))

(def sep-re (re-pattern path/sep))

(defn walk-files
  "Transforms the files in `src` into a routes tree."
  [src]
  (let [src (path/resolve src)
        stat (fs/statSync src)]
    (cond
      (.isDirectory stat) {:type :directory
                           :name (path/basename src)
                           :children (map walk-files
                                          (map (partial path/resolve src)
                                               (fs/readdirSync src)))}
      (.isFile stat) {:type :asset
                      :name (path/basename src)
                      :data (fs/readFileSync src)})))

(defn plugin
  "Reads raw data from the files in `src` into the site map."
  [{:keys [src]}]
  (fn [handler]
    (fn [site-map]
      ;; TODO once multiple source plugins exist, will this need to
      ;; merge the routes instead of overwriting them?
      (handler (assoc site-map :routes (:children (walk-files src)))))))
