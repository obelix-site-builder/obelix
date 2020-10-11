(ns anbaric.plugins.markdown
  (:require ["remark-parse" :as remark]
            ["remark-rehype" :as remark->rehype]
            ["to-vfile" :as vfile]
            ["unified-stream" :as stream]
            fs
            path
            unified
            [clojure.string :as s]))

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

(defn parse-markdown [vfile]
  (let [processor (-> (unified)
                      (.use remark)
                      (.use remark->rehype))
        ast (.parse processor vfile)
        content (transform-ast ast)]
    {:type :page
     :content `[[:body ~@content]]}))

(defn get-md-files
  "Recursively searches `src` for .md and .markdown files."
  [src]
  (let [src (path/resolve src)
        iter (fn iter [acc path]
               (let [stat (fs/statSync path)]
                 (cond
                   (and (.isFile stat) (re-matches md-re path))
                   (conj acc (path/relative src path))
                   (.isDirectory stat) (let [children (fs/readdirSync path)]
                                         (into acc (mapcat (partial iter acc)
                                                           (map #(path/resolve path %)
                                                                children)))))))]
    (iter [] src)))

(defn plugin
  "Reads Markdown files from disk, parses them and puts them in
  the :routes map."
  [{:keys [src]}]
  (fn [handler]
    (let [pages (get-md-files src)]
      (fn [site-map]
        (let [site-map (handler site-map)]
          (reduce (fn [site-map page-path]
                    (let [path (map #(path/basename (path/basename % ".md")
                                                    ".markdown")
                                    (s/split page-path sep-re))
                          parsed (-> (path/resolve src page-path)
                                     (vfile/readSync)
                                     (parse-markdown))]
                      (assoc-in site-map (into [:routes] path) parsed)))
                  site-map
                  pages))))))
