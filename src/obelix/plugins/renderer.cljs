(ns obelix.plugins.renderer
  (:require [hiccups.runtime :as hiccup]
            [obelix.util :as util]))

(defn render
  [routes]
  (util/map-routes
   (fn [node]
     (if (and (= (:type node) :page) (list? (:content node)))
       (update node :content hiccup/render-html)
       node))
   routes))

(defn plugin
  "Renders all Hiccup content in the site map."
  [_config]
  (fn [handler]
    (fn [site-data]
      (let [site-data (handler site-data)]
        (update site-data :routes render)))))
