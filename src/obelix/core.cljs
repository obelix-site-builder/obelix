(ns obelix.core
  (:require [obelix.plugin-loader :as plugins]
            [obelix.plugins.filesystem :as filesystem]
            [obelix.plugins.format :as format]
            [obelix.plugins.layout :as layout]
            [obelix.plugins.markdown :as markdown]
            [obelix.plugins.output :as output]
            [obelix.plugins.template :as template]
            [obelix.plugins.url :as url]))

(defn plugin-pipeline
  "The pipeline of functions run to build the site."
  [plugins config]
  [(plugins/hook-fn plugins :source)
   (filesystem/plugin config)
   (markdown/plugin config)
   (url/plugin config)
   (template/plugin config)
   (layout/layout-plugin config)
   (plugins/hook-fn plugins :postLayoutTemplates)
   (layout/list-template-plugin config)
   (plugins/hook-fn plugins :postListTemplates)
   (format/plugin config)
   (plugins/hook-fn plugins :output)
   (output/plugin config)])

(defn build
  "Builds the static site configured via `config`."
  [config]
  (let [plugins (plugins/load-plugins config)
        handlers (->> (plugin-pipeline plugins config)
                      (reverse)
                      (apply comp))]
    (-> (handlers {:metadata (into {} (:site-metadata config)) :routes []})
        (update :routes doall))))
