(ns space-invaders-clj.system
  (:require
    [clojure.java.io :as io]
    [editscript.core :as ec]
    [i8080-clj.core :as i8080]
    [i8080-clj.rom :as rom]
    [integrant.core :as ig]
    [quil.core :as q]
    [quil.middleware :as qm]
    [space-invaders-clj.machine :as mach]
    [space-invaders-clj.sketch :as sketch])
  (:import [processing.core PApplet]))

(def config
  {:machine {:rom-file (io/resource "invaders.rom")}
   :sketch {:machine (ig/ref :machine), :scale 2}})

(def debug-config
  (assoc config :log {:machine (ig/ref :machine)}))

;; -----------------------------------------------------------------------------

(defmethod ig/init-key :machine [_ {:keys [rom-file]}]
  (let [cpu (i8080/cpu)]
    (atom (assoc (merge cpu mach/initial-machine)
                 :cpu/mem (rom/load-rom (:cpu/mem cpu) rom-file)))))

(defmethod ig/init-key :log [_ {:keys [machine]}]
  (let [log (atom {:cpu-diffs [] :mem-diffs []})
        f (fn [_ _ old-m new-m]
            (swap! log #(-> %
                            (update :cpu-diffs conj (ec/diff old-m new-m {:algo :quick}))
                            (update :mem-diffs conj (:cpu/last-mem new-m)))))]
    (add-watch machine ::log f)
    log))

(defmethod ig/init-key :sketch [_ {:keys [machine scale]}]
  (q/sketch
    :size (map (partial * scale) sketch/dimensions)
    :setup #(sketch/setup scale machine)
    :draw sketch/draw
    :middleware [qm/fun-mode]
    :features [:no-bind-output]
    :key-pressed sketch/key-down
    :key-released sketch/key-up))

;; -----------------------------------------------------------------------------

(defmethod ig/halt-key! :sketch [_ ^PApplet applet]
  (.dispose applet))
