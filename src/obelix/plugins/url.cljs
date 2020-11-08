(ns obelix.plugins.url)

(defn url-mapper
  [node]
  (if (= (:type node) :page)
    (assoc-in node [:metadata :url] (str "/" (:name node)))
    node))

(defn plugin
  "Adds a :url attribute to all routes."
  [_config]
  (fn [site-data]
    (update site-data
            :routes
            (comp doall
                  (partial map url-mapper)))))
