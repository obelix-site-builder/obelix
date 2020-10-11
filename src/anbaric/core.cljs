(ns anbaric.core
  (:require [anbaric.plugins.markdown :as markdown]
            [anbaric.plugins.renderer :as renderer]))

(defn root-handler
  "The root handler, returning an empty site map."
  [site-map]
  (assoc site-map :metadata {} :routes {}))

(def built-in-plugins {:markdown markdown/plugin
                       :renderer renderer/plugin})

(defn resolve-plugin
  "Tries to resolve the plugin and config into a plugin function."
  [{:keys [plugin config]}]
  ;; TODO support loading third-party plugins from node_modules
  ;; via something like (let [plug (js/require "plugin")] (plug/plugin (clj->js config)))
  (if (get built-in-plugins (keyword plugin))
    ((get built-in-plugins (keyword plugin)) config)))

(defn plugin-pipeline
  "Returns the list of handlers representing the site plugin
  pipeline."
  [{:keys [plugins]}]
  (map resolve-plugin plugins))

(defn build
  "Builds the static site configured via `config`."
  [config]
  (let [plugins (->> (plugin-pipeline config)
                     (reverse)
                     (apply comp))
        handler (plugins root-handler)]
    (handler {})))
