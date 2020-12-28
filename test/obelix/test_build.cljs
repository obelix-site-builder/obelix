(ns obelix.test-build
  (:require [cljs.test :as t :refer-macros [deftest testing is]]
            fs
            [obelix.core :as obelix]
            os
            path
            [taoensso.timbre :as log]))

(def string-file-extension #{".html"
                             ".md"
                             ".hbs"
                             ".handlebars"
                             ".js"
                             ".css"
                             ".xml"
                             ".json"
                             ".txt"})

(defn compare-files [actual expected]
  (testing (path/basename actual)
    (if (.isDirectory (fs/statSync actual))
      (let [actual-files (fs/readdirSync actual)
            expected-files (fs/readdirSync expected)]
        (doseq [file actual-files]
          (is (some #{file} expected-files))
          (compare-files (path/resolve actual file)
                         (path/resolve expected file))))
      (if (string-file-extension (path/extname actual))
        (is (= (fs/readFileSync actual "utf-8")
               (fs/readFileSync expected "utf-8")))
        (is (.equals (fs/readFileSync actual)
                     (fs/readFileSync expected)))))))

(defn do-build-test
  [{:keys [src-dir expected-output-dir build-opts]
    :or {build-opts {}}}]
  (let [output-dir (path/resolve
                    (path/join (os/tmpdir)
                               (str "obelix-test-" (js/Date.now))))]
    (obelix/build (assoc build-opts
                         :src (path/resolve src-dir)
                         :out output-dir))
    (compare-files (path/resolve expected-output-dir)
                   output-dir)))

(defn test-site [& paths]
  ;; TODO this requires that the tests be run from the project root
  (path/resolve (apply path/join "test" "test_sites" paths)))

(deftest test-build
  (log/set-level! :error)
  (testing "kitchen_sink"
    (do-build-test {:src-dir (test-site "kitchen_sink" "src")
                    :expected-output-dir (test-site "kitchen_sink"
                                                    "expected")
                    :build-opts {:metadata {:testing true}}})))
