(ns obelix.watcher
  (:require chokidar
            livereload
            path))

(defn watch
  [dir callback]
  (let [watcher (chokidar/watch (path/resolve dir) #js {:ignoreInitial true})]
    (.on watcher "all" callback)))

(defn live-reload
  [dir]
  (let [server (livereload/createServer)]
    (.watch server (path/resolve dir))))
