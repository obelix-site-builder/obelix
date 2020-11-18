(ns obelix.plugin-loader
  (:require path
            [taoensso.timbre :as log]))

(defn hook-fn
  "Given the plugin list and a hook name, generates a function that
  transforms the site-data map."
  [plugins hook]
  (let [plugin-fns (->> plugins
                        (map #(get-in % [:hooks hook]))
                        (filter (complement nil?))
                        (reverse)
                        (apply comp))]
    (fn [site-data]
      (-> site-data
          (clj->js)
          (plugin-fns)
          (js->clj :keywordize-keys true)))))

(defn load-plugin
  [config name]
  (when-let [plugin (or (try
                          (js/require (path/resolve "plugins" name))
                          (catch js/Error _e))
                        (try
                          (js/require (path/resolve "node_modules" name))
                          (catch js/Error _e))
                        (try
                          (js/require name)
                          (catch js/Error _e)))]
    (js->clj (plugin (clj->js config)) :keywordize-keys true)))

(defn load-plugins
  [config]
  (->> (:plugins config)
       (map (fn [[plugin opts]]
              (let [plug (load-plugin opts (name plugin))]
                (if plug
                  plug
                  (log/warn "Could not find plugin" (name plugin))))))
       (filter (complement nil?))))
