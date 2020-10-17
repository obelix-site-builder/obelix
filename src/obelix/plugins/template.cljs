(ns obelix.plugins.template
  (:require handlebars))

(defn handlebars-context
  [site-data node]
  (clj->js (assoc (:metadata node) :site (:metadata site-data))))

(defn template-mapper
  [site-data {:keys [type content] :as node}]
  (if (= type :page)
    (assoc node :content (-> content
                             (handlebars/compile)
                             (apply [(handlebars-context site-data node)])))
    node))

(defn plugin
  "Resolves Handlebars templates in the input files, but does not
  apply layout templates."
  [_config]
  (fn [handler]
    (fn [site-data]
      (let [site-data (handler site-data)]
        (update site-data
                :routes
                (partial map (partial template-mapper site-data)))))))
