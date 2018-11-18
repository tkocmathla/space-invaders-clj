(defproject space-invaders "0.1.0-SNAPSHOT"
  :description ""
  :url ""
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[datascript "0.16.7"]
                 [i8080-clj "0.1.0-SNAPSHOT"]
                 [integrant "0.7.0"]
                 [org.clojure/clojure "1.9.0"]
                 [com.taoensso/tufte "2.0.1"]
                 [com.taoensso/timbre "4.10.0"]]

  :target-path "target/%s"

  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[com.cemerick/pomegranate "1.1.0"]
                                  [integrant/repl "0.3.1"]]}})
