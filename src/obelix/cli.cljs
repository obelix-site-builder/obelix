(ns obelix.cli
  (:require [clojure.edn :as edn]
            [clojure.tools.cli :as cli]
            fs
            [obelix.core :as obelix]
            [obelix.logging :as logging]
            path
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
              (ok (util/format "Built site to %s" (:out config)))))))

(defn main-cmd-help [opts-summary]
  (util/format "Usage: obelix [OPTIONS] SUBCOMMAND

SUBCOMMANDS
build    Compile the site to static files

OPTIONS
%s" opts-summary))

(def main-opts-spec
  [["-h" "--help" "Display this help and exit"]
   [nil "--debug" "Enable debug logging"]])

(defn main-cmd
  [args]
  (let [opts (cli/parse-opts args main-opts-spec :in-order true)
        [subcmd & subargs] (:arguments opts)]
    (logging/configure! {:log-level (when (:debug (:options opts)) :debug)})
    (cond
      (:help (:options opts)) (ok (main-cmd-help (:summary opts)))
      :else (condp = subcmd
              "build" (build-cmd subargs)
              (error (str "Unrecognized command: " subcmd
                          "\n\n"
                          (main-cmd-help (:summary opts))))))))

(defn -main
  [& args]
  (let [exit-code (main-cmd args)]
    (.exit js/process exit-code)))

(set! *main-cli-fn* -main)
