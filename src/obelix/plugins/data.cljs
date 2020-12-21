(ns obelix.plugins.data
  (:require ["js-yaml" :as yaml]
            path
            [taoensso.timbre :as log]))

(def data-file? #{".json" ".yaml" ".yml"})

(defn parse-data
  [page]
  (condp contains? (path/extname (:name page))
    #{".json"} (-> (.parse js/JSON (:content page))
                   (js->clj :keywordize-keys true))
    #{".yaml" ".yml"} (-> (yaml/safeLoad (:content page))
                          (js->clj :keywordize-keys true))))

(defn data-mapper
  [page]
  (if (data-file? (path/extname (:name page)))
    (assoc page
           :type "data"
           :data (parse-data page))
    page))

(defn plugin
  "Formats output files that it knows how to handle."
  [_config]
  (fn [site-data]
    (log/debug "Parsing data files")
    (update site-data :routes (comp doall (partial map data-mapper)))))
