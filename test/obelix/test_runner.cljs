(ns obelix.test-runner
  (:require [cljs.test :as t :include-macros true]
            [obelix.test-build]))

(defn -main [& _args]
  (enable-console-print!)
  (t/run-tests 'obelix.test-build))
