(ns space-invaders-clj.system
  (:require
    [clojure.java.io :as io]
    [datascript.core :as d]
    [i8080-clj.core :as i8080]
    [i8080-clj.rom :as rom]
    [integrant.core :as ig]
    [quil.core :as q]
    [quil.middleware :as qm]
    [space-invaders-clj.db :as db]
    [space-invaders-clj.machine :as mach]
    [space-invaders-clj.sketch :as sketch])
  (:import [processing.core PApplet]))

(def config
  {:machine {:rom-file (io/resource "invaders.rom")}
   :sketch {:machine (ig/ref :machine), :scale 2}})

(def debug-config
  {:machine {:rom-file (io/resource "invaders.rom")}
   :conn {:schema db/schema, :machine (ig/ref :machine)}
   :log {:conn (ig/ref :conn), :machine (ig/ref :machine)}
   :sketch {:machine (ig/ref :machine), :scale 2}})

;; -----------------------------------------------------------------------------

(defmethod ig/init-key :machine [_ {:keys [rom-file]}]
  (let [cpu (i8080/cpu)]
    (atom (assoc (merge cpu mach/initial-machine)
                 :cpu/mem (rom/load-rom (:cpu/mem cpu) rom-file)))))

(defmethod ig/init-key :conn [_ {:keys [schema machine]}]
  (let [conn (d/create-conn schema)]
    (d/transact! conn [@machine])
    conn))

(defmethod ig/init-key :log [_ {:keys [machine conn]}]
  (let [log (atom [])]
    (add-watch machine ::log (fn [_ _ _ m] (swap! log conj (d/transact! conn [(assoc m :db/id 1)]))))
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
