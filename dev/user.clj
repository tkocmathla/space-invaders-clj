(ns user
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.repl :refer :all]
    [datascript.core :as d]
    [i8080-clj.core :as cpu]
    [i8080-clj.opfns :refer [<< >> |]]
    [integrant.core :as ig]
    [integrant.repl :as ir :refer [clear go halt prep init reset reset-all]]
    [integrant.repl.state :as irs]
    [space-invaders-clj.db :as db]
    [space-invaders-clj.machine :as machine]
    [space-invaders-clj.rom :as rom]
    [space-invaders-clj.system :as system]
    [taoensso.timbre :as log]
    [taoensso.timbre.appenders.core :as appenders]
    [taoensso.tufte :refer [p profile] :as tufte])
  (:use [cemerick.pomegranate :only [add-dependencies]]))

#_
(add-dependencies
  :coordinates '[[clj-audio "0.1.0"]]
  :repositories (merge cemerick.pomegranate.aether/maven-central {"clojars" "https://clojars.org/repo"}))


(tufte/add-basic-println-handler! {})
(log/merge-config!
  {:appenders {:println {:enabled? false}
               :spit (appenders/spit-appender {:fname "trace.log"})}
   :output-fn (fn [{:keys [timestamp_ msg_]}] (str (force timestamp_) " " (force msg_)))})


;; normal run
#_
(log/with-level :debug
  (ir/set-prep! (constantly system/config))
  (go))

#_#_
(halt)
(reset-all)


;; debug run
#_
(log/with-level :debug
  (ir/set-prep! (constantly system/debug-config))
  (go))

#_#_
(halt)
(reset-all)

; peek at last txn
#_
(pprint (:tx-data (last @(:log irs/system))))

; peek at last state
#_
(pprint (into (sorted-map) (dissoc (d/pull @(:conn irs/system) [:*] 1) :cpu/mem)))

; log based debugging
#_
(letfn [(take-until [p s] (transduce (halt-when p (fn [r h] (conj r h))) conj [] s))]
  (let [log (rseq @(:log irs/system))]
    (->> log

         #_
         (take-until
           (fn [{:keys [db-after]}]
             (let [{:keys [mach/last-op mach/last-args]} (d/pull db-after [:*] 1)]
               (and (= :IN last-op) (= [1] last-args)))))

         (take-until
           (fn [{:keys [db-before db-after tx-data]}]
             (not-empty
               (filter (fn [{:keys [a v added]}]
                         (and added (#{:mach/port1} a) (pos? v))) tx-data))))

         #_ ; finds where HL is in game ram
         (take-until
           (fn [{:keys [db-after]}]
             (let [{:keys [cpu/h cpu/l]} (d/pull db-after [:*] 1)]
               (< (bit-and (| (<< h 8) l) 0xffff) 0x4000))))

         #_ ; find where cpu/mem has a negative number
         (take-until
           (fn [{{:keys [db/current-tx]} :tempids, :keys [db-after]}]
             (let [{:keys [cpu/mem]} (d/pull db-after [:cpu/mem] 1)]
               (not-any? neg? mem))))

         (map :tx-data)
         (map #(remove (fn [{:keys [a]}] (#{:cpu/mem} a)) %))
         reverse
         (take 20)
         pprint)))
