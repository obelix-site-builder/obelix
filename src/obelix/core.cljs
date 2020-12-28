(ns obelix.core
  (:require [obelix.handlebars :as hb]
            [obelix.plugin-loader :as plugins]
            [obelix.plugins.data :as data]
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

   (plugins/hook-fn plugins :markdown)
   (markdown/plugin config)

   (plugins/hook-fn plugins :url)
   (url/plugin config)

   (plugins/hook-fn plugins :template)
   (template/plugin config)

   (plugins/hook-fn plugins :data)
   (data/plugin config)

   (plugins/hook-fn plugins :listTemplate)
   (layout/list-template-plugin config)

   (plugins/hook-fn plugins :layout)
   (layout/layout-plugin config)

   (plugins/hook-fn plugins :format)
   (format/plugin config)

   (plugins/hook-fn plugins :output)
   (output/plugin config)])

(defn build
  "Builds the static site configured via `config`."
  [config]
  (hb/register-helpers! config)
  (let [plugins (plugins/load-plugins config)
        handlers (->> (plugin-pipeline plugins config)
                      (reverse)
                      (apply comp))]
    (-> (handlers {:metadata (into {} (:metadata config)) :routes []})
        (update :routes doall))))
