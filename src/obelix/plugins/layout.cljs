(ns obelix.plugins.layout
  (:require path))

(def default-layout-templates #{"layout.html.hbs" "layout.html.handlebars"})

(defn layout-template?
  "Predicate that returns `true` if the `page` is a layout template
  that should not be rendered on its own."
  [config page]
  (contains? (set (or (:layout-templates config)
                      default-layout-templates))
             (path/basename (:name page))))

(defn list-template?
  "Predicate to detect list templates: any Handlebars file that is not
  a layout template."
  [config page]
  (and (not (layout-template? config page))
       (contains? #{".hbs" ".handlebars"} (path/extname (:name page)))))

(defn layout-template-for
  "Returns the layout template that applies to the `page`."
  [site-data page])

(defn layout-mapper
  "If the `page` is a list template, render it. If there is a template
  layout for this `page`, apply it."
  [config site-data page]
  (if (list-template? config page)
    ()
    (if-let [layout-template (layout-template-for site-data page)]
      ()
      page)))

(defn plugin
  "Applies layout templates.

  Templates are files with a .hbs or .handlebars extension. Template
  type is determined from the template metadata - list type templates
  receive all nodes at the same directory level as them as an
  argument, while single type templates apply to individual posts and
  are passed a single node."
  [config]
  (fn [handler]
    (fn [site-data]
      (let [site-data (handler site-data)]
        (-> site-data
            (update :routes (partial map (partial layout-mapper config site-data)))
            (update :routes
                    (partial filter
                             (complement (partial layout-template? config)))))))))
