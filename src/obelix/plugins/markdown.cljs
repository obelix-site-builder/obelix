(ns obelix.plugins.markdown
  (:require ["remark-parse" :as remark]
            ["remark-rehype" :as remark->rehype]
            ["rehype-stringify" :as html]
            ["unified-stream" :as stream]
            [clojure.string :as s]
            fs
            path
            unified
            [taoensso.timbre :as log]))

(def md-re #".*(\.md|\.markdown)")

(defn parse-markdown [content]
  (-> (unified)
      (.use remark)
      (.use remark->rehype #js {:allowDangerousHtml true})
      (.use html #js {:allowDangerousHtml true})
      (.processSync content)
      (.toString)))

(defn markdown-mapper
  [{:keys [type name] :as node}]
  (if (and (= type :page) (re-matches md-re name))
    (do
      (log/debug "Parsing markdown in" name)
      (let [content (parse-markdown (:content node))]
        (-> node
            (assoc :content content)
            (assoc :name (str (path/join (path/dirname name)
                                         (path/basename
                                          name
                                          (path/extname name)))
                              ".html")))))
    node))

(defn plugin
  "Parses Markdown files in the :routes map and turns them into page
  nodes."
  [_config]
  (fn [site-map]
    (update site-map :routes (comp doall (partial map markdown-mapper)))))
