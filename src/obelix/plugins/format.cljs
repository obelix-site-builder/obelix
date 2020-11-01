(ns obelix.plugins.format
  (:require ["rehype-format" :as format]
            ["rehype-stringify" :as stringify]
            path
            rehype
            [clojure.string :as s]
            [taoensso.timbre :as log]))

(defn format-html
  [content]
  (-> (rehype)
      (.use format)
      (.use stringify)
      (.processSync content)
      (.toString)
      (s/trim)))

(defn format-mapper
  [page]
  (condp = (path/extname (:name page))
    ".html" (do
              (log/debug "Formatting" (:name page))
              (update page :content format-html))
    page))

(defn plugin
  "Formats output files that it knows how to handle."
  [_config]
  (fn [site-data]
    (log/debug "Formatting output files")
    (update site-data :routes (comp doall(partial map format-mapper)))))
