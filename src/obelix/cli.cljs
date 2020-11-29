(ns obelix.cli
  (:require [clojure.edn :as edn]
            [clojure.tools.cli :as cli]
            fs
            [obelix.core :as obelix]
            [obelix.logging :as logging]
            [obelix.server :as server]
            [obelix.watcher :as watcher]
            path
            [taoensso.timbre :as log]
            util))

(defn error [msg]
  (println msg)
  1)

(defn ok [msg]
  (println msg)
  0)

(defn read-config
  [file]
  ;; TODO support JSON config file as well
  (-> (fs/readFileSync file "utf-8")
      (edn/read-string)))

(defn default-config []
  (path/resolve "obelix.edn"))

(defn serve-cmd-help [opts-summary]
  (util/format "Usage: obelix serve [OPTIONS]

Starts a hot-reloading web server to serve the site.
For development only, do not use in production!

OPTIONS
%s" opts-summary))

(def serve-opts-spec
  [["-a" "--host HOST" "Host to serve the site on"
    :default "0.0.0.0"]
   ["-p" "--port PORT" "Port to serve the site on"
    :default 8080]
   ["-c" "--config FILE" "Configuration file"
    :default (default-config)]
   ["-h" "--help" "Display this help and exit"]])

(defn serve-cmd
  [args]
  (let [opts (cli/parse-opts args serve-opts-spec)]
    (cond
      (:help (:options opts)) (ok (serve-cmd-help (:summary opts)))
      :else (let [config (read-config (:config (:options opts)))]
              (watcher/watch (:src config)
                             (fn [event path]
                               (log/info
                                (util/format "Detected change for %s, rebuilding site" path))
                               (obelix/build config)
                               (log/info "Built site to" (:out config))))
              (watcher/live-reload (:out config))
              (log/info "Watching" (:src config) "for changes")
              (obelix/build config)
              (log/info "Built site to" (:out config))
              (server/serve (:out config)
                            (:port (:options opts))
                            (:host (:options opts)))
              :no-exit))))

(defn build-cmd-help [opts-summary]
  (util/format "Usage: obelix build [OPTIONS]

Compiles the site to static files.

OPTIONS
%s" opts-summary))

(def build-opts-spec
  [["-c" "--config FILE" "Configuration file"
    :default (default-config)]
   ["-h" "--help" "Display this help and exit"]])

(defn build-cmd
  [args]
  (let [opts (cli/parse-opts args build-opts-spec)]
    (cond
      (:help (:options opts)) (ok (build-cmd-help (:summary opts)))
      :else (let [config (read-config (:config (:options opts)))]
              (obelix/build config)
              (log/info "Built site to" (:out config))
              0))))

(defn main-cmd-help [opts-summary]
  (util/format "Usage: obelix [OPTIONS] SUBCOMMAND

SUBCOMMANDS
build    Compile the site to static files
serve    Start a hot-reloading development server

OPTIONS
%s" opts-summary))

(def main-opts-spec
  [["-h" "--help" "Display this help and exit"]
   [nil "--debug" "Enable debug logging"]])

(defn main-cmd
  [args]
  (let [opts (cli/parse-opts args main-opts-spec :in-order true)
        [subcmd & subargs] (:arguments opts)]
    (logging/configure! {:log-level (if (:debug (:options opts)) :debug :info)})
    (cond
      (:help (:options opts)) (ok (main-cmd-help (:summary opts)))
      :else (condp = subcmd
              "build" (build-cmd subargs)
              "serve" (serve-cmd subargs)
              (error (str "Unrecognized command: " subcmd
                          "\n\n"
                          (main-cmd-help (:summary opts))))))))

(defn -main
  [& args]
  (let [exit-code (main-cmd args)]
    (when-not (= exit-code :no-exit)
      (.exit js/process exit-code))))

(set! *main-cli-fn* -main)
