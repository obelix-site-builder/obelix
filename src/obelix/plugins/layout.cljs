(ns obelix.plugins.layout
  (:require [clojure.set :as set]
            [clojure.string :as s]
            handlebars
            path
            [taoensso.timbre :as log]))

(def default-layout-templates #{"layout.html.hbs" "layout.html.handlebars"})

(defn layout-templates
  [config]
  (set (or (:layoutTemplates config) default-layout-templates)))

(defn all-layout-templates
  "All layout templates including custom ones"
  [config pages]
  (set/union (layout-templates config)
             (set
              (filter (complement nil?)
                      (map (comp :template :metadata)
                           pages)))))

(defn layout-template?
  "Predicate that returns `true` if the `page` is a layout template
  that should not be rendered on its own."
  [config pages page]
  (contains? (all-layout-templates config pages) (path/basename (:name page))))

(defn list-template?
  "Predicate to detect list templates: any Handlebars file that is not
  a layout template."
  [config pages page]
  (and (not (layout-template? config pages page))
       (contains? #{".hbs" ".handlebars"} (path/extname (:name page)))))

(defn layout-template-for
  "Returns the layout template that applies to the `page`."
  [config prefix-map page]
  (log/debug "Finding layout template for" (:name page))
  (let [find-layout
        (fn find-layout [prefix]
          (let [layout-templates (if (contains? (:metadata page) :template)
                                   #{(:template (:metadata page))}
                                   (layout-templates config))
                pages (get prefix-map prefix)
                layout (first (filter #(contains?
                                        layout-templates
                                        (path/basename (:name %)))
                                      pages))]
            (if (or layout (= prefix "."))
              layout
              (recur (path/dirname prefix)))))
        layout (find-layout (path/dirname (:name page)))]
    (when layout (log/debug "Chose layout" (:name layout) "for" (:name page)))
    layout))

(defn site-pages
  "Returns all pages in the site formatted for Handlebars
  consumption.

  Returns a hybrid array of pages with attributes for
  subdirectories. For example, if the site has top-level pages `a` and `b`
  and a subdirectory `blog` with pages `c` and `d`, the returned
  object would be an array with two items, `a` and `b`, and an
  attribute `blog` which would be another hybrid array containing `c`
  and `d`."
  [site-data]
  (let [pages #js []
        push-at-path! (fn push-at-path! [[first-path & rest-path] pages page]
                        (if (nil? first-path)
                          (.push pages (-> (:metadata page)
                                           (assoc :content (:content page))
                                           (clj->js)))
                          (do
                            (when (nil? (aget pages first-path))
                              (aset pages first-path #js []))
                            (push-at-path! rest-path
                                           (aget pages first-path)
                                           page))))]
    (doseq [page (:routes site-data)]
      (when (= (:type page) "page")
        (let [page-path (-> (path/dirname (:name page))
                            (s/replace #"^\." "")
                            (s/split path/sep)
                            (#(filter (complement s/blank?) %)))]
          (push-at-path! page-path pages page))))
    pages))

(defn site-data-files
  [site-data]
  (clj->js
   (reduce
    (fn [acc {:keys [type name] :as node}]
      (if (= type "data")
        (let [name-parts (s/split (path/join (path/dirname name)
                                             (path/basename name
                                                            (path/extname name)))
                                  path/sep)]
          (assoc-in acc (map keyword name-parts)
                    (:data node)))
        acc))
    {}
    (:routes site-data))))

(defn get-list-pages [config site-data prefix-map template-name]
  (->> (get prefix-map (path/dirname template-name))
       (filter #(and
                 (= (:type %) "page")
                 (not (or (layout-template? config (:routes site-data) %)
                          (list-template? config (:routes site-data) %)))))
       (map #(-> (:metadata %)
                 (assoc :content (:content %))
                 (assoc :site (assoc (:metadata site-data)
                                     :pages (site-pages site-data)
                                     :data (site-data-files site-data)))))))

(defn list-template-mapper
  [config site-data prefix-map {:keys [name renderedContent content] :as page}]
  (if (list-template? config (:routes site-data) page)
    (do
      (log/debug "Rendering list template" name)
      (let [pages (get-list-pages config site-data prefix-map name)
            template (handlebars/compile (str (or renderedContent content)))]
        (-> page
            (assoc :content
                   (template (clj->js {:site (assoc (:metadata site-data)
                                                    :pages (site-pages site-data)
                                                    :data (site-data-files site-data))
                                       :pages pages})))
            (assoc :name (path/join (path/dirname name)
                                    (path/basename (path/basename name ".hbs")
                                                   ".handlebars"))))))
    page))

(defn layout-mapper
  "If there is a template layout for this `page`, apply it."
  [config site-data prefix-map {:keys [name content] :as page}]
  (log/debug "Considering layouts for" name)
  (cond
    (not= (:type page) "page") page
    (layout-template? config (:routes site-data) page) page
    :else (if-let [layout-template (layout-template-for config prefix-map page)]
            (do
              (log/debug "Applying layout template" (:name layout-template) "to page" name)
              (let [template (handlebars/compile (str (:content layout-template)))]
                (assoc page :content
                       (template
                        (clj->js
                         (-> (:metadata page)
                             (assoc :content content)
                             (assoc :site
                                    (assoc (:metadata site-data)
                                           :pages (site-pages site-data)
                                           :data (site-data-files site-data)))))))))
            page)))

(defn routes-by-prefix
  "Transforms the routes list into a map keyed by directory prefix,
  e.g.  [{:name \"foo/bar/baz.md\"} {:name \"foo/baz/qux.md\"}]
  becomes {\"foo/bar\" [\"foo/bar/baz.md\" \"foo/bar/qux.md\"]}. This
  will let me look up files based on which directories they are in."
  [routes]
  (log/debug "Generating prefix map")
  (reduce (fn [prefix-map page]
            (let [prefix (path/dirname (:name page))]
              (assoc prefix-map
                     prefix
                     (conj (or (get prefix-map prefix) []) page))))
          {}
          routes))

(defn layout-plugin
  "Applies layout templates."
  [config]
  (fn [site-data]
    (log/debug "Applying layout templates")
    (let [prefix-map (routes-by-prefix (:routes site-data))]
      (-> site-data
          (update :routes
                  (comp doall
                        (partial filter
                                 (complement (partial layout-template?
                                                      config
                                                      (:routes site-data))))
                        (partial map
                                 (partial layout-mapper
                                          config
                                          site-data
                                          prefix-map))))))))

(defn list-template-plugin
  "Generates list templates."
  [config]
  (fn [site-data]
    (let [prefix-map (routes-by-prefix (:routes site-data))]
      (update site-data
              :routes
              (comp doall
                    (partial map
                             (partial list-template-mapper
                                      config
                                      site-data
                                      prefix-map)))))))
