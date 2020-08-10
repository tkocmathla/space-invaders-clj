(defproject space-invaders-clj "0.1.0-SNAPSHOT"
  :description "Space Invaders emulation in Clojure"
  :url "https://github.com/tkocmathla/space-invaders-clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[com.taoensso/timbre "4.10.0"]
                 [com.taoensso/tufte "2.0.1"]
                 [i8080-clj "0.4.0"]
                 [integrant "0.7.0"]
                 [juji/editscript "0.4.3"]
                 [org.clojure/clojure "1.9.0"]
                 [quil "3.1.0"]]

  :target-path "target/%s"

  :global-vars {*warn-on-reflection* true}

  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[integrant/repl "0.3.1"]]}}

  :main space-invaders-clj.core)
