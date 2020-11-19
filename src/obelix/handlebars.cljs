(ns obelix.handlebars
  (:require handlebars
            path))

(defn sort-helper
  [arr options]
  (if-let [key (:key (js->clj (aget options "hash") :keywordize-keys true))]
    (apply array (sort #(compare (aget %1 key) (aget %2 key)) arr))
    (apply array (sort arr))))

(defn reverse-helper
  [arr]
  (apply array (reverse arr)))

(defn register-helpers!
  [config]
  (handlebars/registerHelper "sort" sort-helper)
  (handlebars/registerHelper "reverse" reverse-helper)
  (when-let [custom-helpers (:handlebarsHelpers config)]
    (let [custom-helpers (js/require (path/resolve custom-helpers))]
      (doseq [[name func] (js->clj custom-helpers)]
        (handlebars/registerHelper name func)))))
