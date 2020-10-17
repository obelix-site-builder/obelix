(ns obelix.core
  (:require [obelix.plugins.filesystem :as filesystem]
            [obelix.plugins.markdown :as markdown]
            [obelix.plugins.output :as output]
            [obelix.plugins.template :as template]))

(defn built-in-plugins
  "The build-in plugins run for every build"
  [config]
  [(filesystem/plugin config)
   (markdown/plugin config)
   (template/plugin config)
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
                     (apply comp))
        handler (plugins identity)]
    (handler {:metadata (into {} (:site-metadata config)) :routes []})))
