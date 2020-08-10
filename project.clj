(defproject space-invaders-clj "0.1.0-SNAPSHOT"
  :description "Space Invaders emulation in Clojure"
  :url "https://github.com/tkocmathla/space-invaders-clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[com.taoensso/timbre "4.10.0"]
                 [com.taoensso/tufte "2.0.1"]
                 [juji/editscript "0.4.3"]
                 [integrant "0.7.0"]
                 [org.clojure/clojure "1.9.0"]
                 [quil "3.1.0"]
                 [i8080-clj "0.3.0-SNAPSHOT"]]

  :target-path "target/%s"

  :global-vars {*warn-on-reflection* true}

  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[com.cemerick/pomegranate "1.1.0"]
                                  [integrant/repl "0.3.1"]]}}

  :main space-invaders-clj.core)
