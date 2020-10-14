(ns anbaric.plugins.filesystem
  (:require [clojure.string :as s]
            fs
            path))

(def sep-re (re-pattern path/sep))

(defn get-files
  "Returns a list of all files in `src`, searching recursively."
  [src]
  (let [src (path/resolve src)
        stat (fs/statSync src)]
    (cond
      (.isDirectory stat) (mapcat get-files
                                  (map (partial path/resolve src)
                                       (fs/readdirSync src)))
      (.isFile stat) [src])))

(defn plugin
  "Reads raw data from the files in `src` into the site map."
  [{:keys [src]}]
  (fn [handler]
    (fn [site-map]
      (let [files (get-files src)]
        (reduce (fn [site-map filepath]
                  (let [path (s/split (path/relative src filepath) sep-re)
                        data (fs/readFileSync (path/resolve src filepath))]
                    (assoc-in site-map (into [:routes] path)
                              {:type :asset
                               :data data})))
                site-map
                files)))))
