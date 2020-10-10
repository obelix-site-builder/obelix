(ns anbaric.core)

(defn root-handler
  "The root handler, returning an empty site map."
  [site-map]
  (assoc site-map :metadata {} :routes {}))

(defn plugin-pipeline
  "Returns the list of handlers representing the site plugin
  pipeline."
  [config]
  ;; TODO
  [])

(defn build
  "Builds the static site configured via `config`."
  [config]
  (let [plugins (->> (plugin-pipeline config)
                     (apply comp))
        handler (plugins root-handler)]
    (handler {})))
