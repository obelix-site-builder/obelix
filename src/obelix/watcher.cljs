(ns obelix.watcher
  (:require chokidar
            livereload
            path))

(defn watch
  [dir callback]
  (let [watcher (chokidar/watch (path/resolve dir) #js {:ignoreInitial true})]
    (.on watcher "all" callback)))

(defn live-reload
  [dir callback]
  (let [server (livereload/createServer
                #js {:delay 1000}
                callback)]
    (.watch server (path/resolve dir))))
