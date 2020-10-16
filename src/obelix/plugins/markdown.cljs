(ns obelix.plugins.markdown
  (:require ["remark-parse" :as remark]
            ["remark-rehype" :as remark->rehype]
            ["rehype-format" :as format]
            ["rehype-stringify" :as html]
            ["unified-stream" :as stream]
            [clojure.string :as s]
            fs
            path
            unified))

(def md-re #".*(\.md|\.markdown)")

(defn parse-markdown [content]
  (-> (unified)
      (.use remark)
      (.use remark->rehype)
      (.use format)
      (.use html)
      (.processSync content)
      (.toString)))

(defn markdown-mapper
  [{:keys [type name] :as node}]
  (if (and (= type :asset) (re-matches md-re name))
    (let [content (parse-markdown (:content node))]
      (-> node
          (assoc :content content)
          (assoc :type :page)
          (assoc :name (str (path/join (path/dirname name)
                                       (path/basename
                                        name
                                        (path/extname name)))
                            ".html"))))
    node))

(defn plugin
  "Parses Markdown files in the :routes map and turns them into page
  nodes."
  [_config]
  (fn [handler]
    (fn [site-map]
      (let [site-map (handler site-map)]
        (update site-map :routes (partial map markdown-mapper))))))
