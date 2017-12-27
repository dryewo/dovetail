(defproject cyrus/logging "0.1.0"
  :description "Companion logging library for Cyrus Leiningen template."
  :url "https://github.com/dryewo/cyrus-logging"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [com.taoensso/timbre "4.10.0"]
                 [com.fzakaria/slf4j-timbre "0.3.8"]
                 [org.clojure/tools.logging "0.4.0"]]
  :plugins [[lein-cloverage "1.0.9"]
            [lein-shell "0.5.0"]
            [lein-ancient "0.6.15"]]
  :deploy-repositories [["releases" :clojars]]
  :aliases {"update-readme-version" ["shell" "sed" "-i" "s/\\[cyrus\\/logging \"[0-9.]*\"\\]/[cyrus\\\\/logging \"${:version}\"]/" "README.md"]}
  :release-tasks [["shell" "git" "diff" "--exit-code"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["update-readme-version"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["deploy"]
                  ["vcs" "push"]])
