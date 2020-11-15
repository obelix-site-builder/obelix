(ns obelix.core
  (:require [obelix.plugins.filesystem :as filesystem]
            [obelix.plugins.format :as format]
            [obelix.plugins.layout :as layout]
            [obelix.plugins.markdown :as markdown]
            [obelix.plugins.output :as output]
            [obelix.plugins.template :as template]
            [obelix.plugins.url :as url]))

(defn built-in-plugins
  "The built-in plugins run for every build"
  [config]
  [(filesystem/plugin config)
   (markdown/plugin config)
   (url/plugin config)
   (template/plugin config)
   (layout/layout-plugin config)
   (layout/list-template-plugin config)
   (format/plugin config)
   (output/plugin config)])

(defn plugin-pipeline
  "Returns the list of handlers representing the site plugin
  pipeline."
  [config]
  ;; TODO support loading third-party plugins from node_modules
  ;; via something like (let [plug (js/require "plugin")] (plug/plugin (clj->js config)))
  (built-in-plugins config))

(defn build
  "Builds the static site configured via `config`."
  [config]
  (let [plugins (->> (plugin-pipeline config)
                     (reverse)
                     (apply comp))]
    (-> (plugins {:metadata (into {} (:site-metadata config)) :routes []})
        (update :routes doall))))
