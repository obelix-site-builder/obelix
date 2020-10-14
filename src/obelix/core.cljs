(ns obelix.core
  (:require [obelix.plugins.filesystem :as filesystem]
            [obelix.plugins.markdown :as markdown]
            [obelix.plugins.renderer :as renderer]))

(defn root-handler
  "The root handler, returning an empty site map."
  [site-map]
  (assoc site-map :metadata {} :routes {}))

;; TODO update markdown plugin to handle data from fs plugin
;;      and figure out how config will be passed to built-in plugins

(def built-in-plugins
  "The build-in plugins run for every build"
  [filesystem/plugin
   markdown/plugin
   renderer/plugin])

(defn plugin-pipeline
  "Returns the list of handlers representing the site plugin
  pipeline."
  [_config]
  ;; TODO support loading third-party plugins from node_modules
  ;; via something like (let [plug (js/require "plugin")] (plug/plugin (clj->js config)))
  built-in-plugins)

(defn build
  "Builds the static site configured via `config`."
  [config]
  (let [plugins (->> (plugin-pipeline config)
                     (reverse)
                     (apply comp))
        handler (plugins root-handler)]
    (handler {})))
