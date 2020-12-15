(ns obelix.server
  {:clj-kondo/config
   '{:linters
     {:unresolved-symbol {:exclude [express]}}}}
  (:require ["connect-livereload" :as livereload]
            express
            http
            path
            util
            [taoensso.timbre :as log]))

(defn serve
  [dir port host]
  (let [app (express)]
    (.use app (livereload))
    (.use app
          (fn [req res next]
            (.on res
                 "finish"
                 (fn []
                   (log/info (util/format "\"%s %s\" %s"
                                          (.-method req)
                                          (.-originalUrl req)
                                          (.-statusCode res)))))
            (next)))
    (.use app (.static express (path/resolve dir)))
    (let [server (http/createServer app)]
      (.listen server
               port
               host
               (fn []
                 (let [address (js->clj (.address server) :keywordize-keys true)]
                   (log/info
                    (util/format "Serving HTTP from %s on %s port %s (http://%s:%s/)"
                                 dir
                                 (:address address)
                                 (:port address)
                                 (:address address)
                                 (:port address))))))
      server)))
