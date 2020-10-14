(ns obelix.plugins.markdown
  (:require ["remark-parse" :as remark]
            ["remark-rehype" :as remark->rehype]
            ["unified-stream" :as stream]
            [obelix.util :as util]
            [clojure.string :as s]
            fs
            path
            unified
            vfile))

(def md-re #".*(\.md|\.markdown)")
(def sep-re (re-pattern path/sep))

(declare transform-ast)

(defn make-element
  ([tag children]
   `[~tag ~@(map transform-ast children)])
  ([tag attrs children]
   `[~tag ~attrs ~@(map transform-ast children)]))

(defn code->hiccup [code]
  `[:pre [:code {:class ~(.-lang code)} ~(.-value code)]])

(defn transform-ast
  "Parses the AST returned from remark into a Hiccup vector."
  [ast]
  ;; TODO make this an exhaustive list of all possible node types
  (condp = (.-type ast)
    "root" (into [] (map transform-ast (.-children ast)))
    "heading" (let [tag (keyword (str "h" (.-depth ast)))]
                (make-element tag (.-children ast)))
    "paragraph" (make-element :p (.-children ast))
    "code" (code->hiccup ast)
    "inlineCode" `[:code ~(.-value ast)]
    "link" (make-element :a {:href (.-url ast)} (.-children ast))
    "text" (.-value ast)))

(defn parse-markdown [file]
  (let [processor (-> (unified)
                      (.use remark)
                      (.use remark->rehype))
        ast (.parse processor file)
        content (transform-ast ast)]
    content))

(defn markdown-mapper
  [{:keys [type name] :as node}]
  (if (and (= type :assset) (re-matches md-re name))
    (let [file (vfile (:data node))
          content (parse-markdown vfile)]
      (-> node
          (assoc :content content)
          (dissoc :data)
          (assoc :type :page)
          (assoc :name (str (path/basename
                             (path/basename (:name node) ".md")
                             ".markdown")
                            ".html"))))
    node))

(defn plugin
  "Parses Markdown files in the :routes map and turns them into page
  nodes."
  [{:keys [src]}]
  (fn [handler]
    (fn [site-map]
      (let [site-map (handler site-map)]
        (update site-map :routes
                (partial util/map-routes markdown-mapper))))))
