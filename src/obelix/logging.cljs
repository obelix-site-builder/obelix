(ns obelix.logging
  (:require [taoensso.timbre :as log]
            util))

(defn output-log [data]
  (util/format "%s [%s] - %s"
               (:instant data)
               (name (:level data))
               (force (:msg_ data))))

(defn configure! [{:keys [log-level] :or {log-level :info}}]
  (log/merge-config! {:min-level log-level
                      :output-fn output-log}))
