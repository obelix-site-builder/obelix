(ns obelix.plugins.template
  (:require handlebars
            [taoensso.timbre :as log]))

(defn handlebars-context
  [site-data node]
  (log/debug "Generating Handlebars context for" (:name node))
  (clj->js (assoc (:metadata node) :site (:metadata site-data))))

(defn apply-template
  [template context]
  (log/debug "Applying template with context" (js->clj context))
  (template context))

(defn template-mapper
  [site-data {:keys [type content] :as node}]
  (if (= type :page)
    (do
      (log/debug "Rendering template in" (:name node))
      (assoc node :content (-> content
                               (str)
                               (handlebars/compile #js {:noEscape true})
                               (apply-template (handlebars-context site-data node)))))
    node))

(defn plugin
  "Resolves Handlebars templates in the input files, but does not
  apply layout templates."
  [_config]
  (fn [site-data]
    (update site-data
            :routes
            (partial map (partial template-mapper site-data)))))
