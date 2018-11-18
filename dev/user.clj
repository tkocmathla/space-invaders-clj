(ns user
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.repl :refer :all]
    [datascript.core :as d]
    [i8080-clj.core :as cpu]
    [integrant.core :as ig]
    [integrant.repl :as ir :refer [clear go halt prep init reset reset-all]]
    [integrant.repl.state :as irs]
    [space-invaders-clj.db :as db]
    [space-invaders-clj.machine :as machine]
    [space-invaders-clj.rom :as rom]
    [space-invaders-clj.system :as system]
    [taoensso.tufte :refer [p profile] :as tufte])
  (:use [cemerick.pomegranate :only [add-dependencies]]))

#_
(add-dependencies
  :coordinates '[[com.taoensso/timbre "4.10.0"]]
  :repositories (merge cemerick.pomegranate.aether/maven-central {"clojars" "https://clojars.org/repo"}))


(tufte/add-basic-println-handler! {})
(ir/set-prep! (constantly system/config))

#_
(reset)

(def debug-state-fn #(d/pull @(:db/conn irs/system) [:*] 1))
(def state-fn #(deref (:db/atom irs/system)))

(def debug-step
  #(p :transact
      (d/transact!
        (:db/conn irs/system)
        [(p :step (machine/step-cpu (p :pull (debug-state-fn))))])))

(def step
  #(reset! (:db/atom irs/system) (machine/step-cpu (state-fn))))

#_
(do (doall (repeatedly 100000 step))
    (state-fn))
