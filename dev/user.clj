(ns user
  (:require
    [clojure.java.io :as io]
    [clojure.pprint :refer [pprint]]
    [clojure.repl :refer :all]
    [editscript.core :as ec]
    [i8080-clj.core :as cpu]
    [i8080-clj.opfns :refer [<< >> | write-byte]]
    [i8080-clj.rom :as rom]
    [integrant.core :as ig]
    [integrant.repl :as ir :refer [clear go halt prep init reset reset-all]]
    [integrant.repl.state :as irs]
    [space-invaders-clj.machine :as machine]
    [space-invaders-clj.sprites :as sprites]
    [space-invaders-clj.system :as system]
    [taoensso.timbre :as log]
    [taoensso.timbre.appenders.core :as appenders]
    [taoensso.tufte :refer [p profile] :as tufte]))

; set up logger and profiler
(tufte/add-basic-println-handler! {})
(log/merge-config!
  {:appenders {:println {:enabled? false}
               :spit (appenders/spit-appender {:fname "trace.log"})}
   :output-fn (fn [{:keys [timestamp_ msg_]}] (str (force timestamp_) " " (force msg_)))})

; unpacking sprites
;
; 0x1d64 24 flying saucer
; 0x1cc0 16 alien exploding
; 0x1c10  8 alien 2
#_
(let [mem (rom/load-rom (:cpu/mem (cpu/cpu)) (io/resource "invaders.rom"))
      sprite (sprites/unpack-sprite mem 0x1c10 16)]
  (sprites/draw-sprite sprite {:scale 8}))

; ------------------------------------------------------------------------------
; normal run
#_
(log/with-level :debug
  (ir/set-prep! (constantly system/config))
  (go)
  (future (dorun (repeatedly #(machine/step-cpu (:machine irs/system))))))
#_(halt)
#_(reset-all)

; ------------------------------------------------------------------------------
; debug run
#_
(log/with-level :debug
  (ir/set-prep! (constantly system/debug-config))
  (go))
#_(halt)
#_(reset-all)

; peek at last machine state
#_(count (:cpu-diffs @(:log irs/system)))
#_(count (:mem-diffs @(:log irs/system)))
#_(pprint (take-last 5 (:cpu-diffs @(:log irs/system))))
#_(pprint (take-last 25 (:mem-diffs @(:log irs/system))))
#_(pprint (into (sorted-map) (dissoc @(:machine irs/system) :cpu/mem)))

; ------------------------------------------------------------------------------
; time traveling
; TODO var to limit history depth
; TODO var to store a complete machine copy every X states
; TODO ui controls like slider to time travel backwards and pause/resume buttons
(defn restore-state! [system n]
  (let [cpu-diffs (subvec (:cpu-diffs @(:log system)) 0 n)
        mem-diffs (subvec (:mem-diffs @(:log system)) 0 n)]
    (reset! (:machine system) @(:machine (ig/init system/debug-config [:machine])))
    (doseq [diff cpu-diffs]
      (swap! (:machine system) ec/patch diff))
    (doseq [[addr data :as diff] mem-diffs :when diff]
      (swap! (:machine system) write-byte addr data))
    (reset! (:log system) {:cpu-diffs cpu-diffs, :mem-diffs mem-diffs})
    nil))

(defn advance-state! [system n]
  (dorun (repeatedly n #(machine/step-cpu (:machine system)))))

; drawing starts around 40K
#_(advance-state! irs/system 45000)
#_(restore-state! irs/system 40000)


; FIXME rework all this to use new log format
; log based debugging
;(letfn [(take-until [p s] (transduce (halt-when p (fn [r h] (conj r h))) conj [] s))]
;  (let [log (rseq @(:log irs/system))]
;    (->> log
;
;         ; find the first input interrupt for port 1
;         #_
;         (take-until
;           (fn [{:keys [db-after]}]
;             (let [{:keys [mach/last-op mach/last-args]} (d/pull db-after [:*] 1)]
;               (and (= :IN last-op) (= [1] last-args)))))
;
;         #_
;         (take-until
;           (fn [{:keys [db-before db-after tx-data]}]
;             (not-empty
;               (filter (fn [{:keys [a v added]}]
;                         (and added (#{:mach/port1} a) (pos? v))) tx-data))))
;
;         ; find where HL is in game ram
;         #_
;         (take-until
;           (fn [{:keys [db-after]}]
;             (let [{:keys [cpu/h cpu/l]} (d/pull db-after [:*] 1)]
;               (< (bit-and (| (<< h 8) l) 0xffff) 0x4000))))
;
;         ; find where cpu/mem has a negative number
;         #_
;         (take-until
;           (fn [{:keys [db-after]}]
;             (let [{:keys [cpu/mem]} (d/pull db-after [:cpu/mem] 1)]
;               (not-any? neg? mem))))
;
;         (map :tx-data)
;         (map #(remove (fn [{:keys [a]}] (#{:cpu/mem} a)) %))
;         reverse
;
;         (take 20)
;         pprint)))
