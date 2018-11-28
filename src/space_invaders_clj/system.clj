(ns space-invaders-clj.system
  (:require
    [datascript.core :as d]
    [i8080-clj.core :as cpu]
    [integrant.core :as ig]
    [quil.core :as q]
    [quil.middleware :as qm]
    [space-invaders-clj.db :as db]
    [space-invaders-clj.machine :as mach]
    [space-invaders-clj.rom :as rom]
    [space-invaders-clj.sketch :as sketch])
  (:import [processing.core PApplet]))

(def config
  {:machine {:rom-file "invaders.rom"}
   :sketch {:machine (ig/ref :machine)}})

(def debug-config
  {:machine {:rom-file "invaders.rom"}
   :conn {:schema db/schema, :machine (ig/ref :machine)}
   :log {:conn (ig/ref :conn)}
   :debug-sketch {:machine (ig/ref :machine), :conn (ig/ref :conn)}})


(defmethod ig/init-key :machine [_ {:keys [rom-file]}]
  (assoc (merge cpu/cpu mach/machine)
         :cpu/mem (rom/load-rom cpu/cpu rom-file)))

(defmethod ig/init-key :conn [_ {:keys [schema machine]}]
  (let [conn (d/create-conn schema)]
    (d/transact! conn [machine])
    conn))

(defmethod ig/init-key :log [_ {:keys [conn]}]
  (let [log (atom [])]
    (d/listen! conn #(swap! log conj %))
    log))

(defmethod ig/init-key :sketch [_ {:keys [machine]}]
  (q/sketch
    :size sketch/dimensions
    :setup #(sketch/setup machine)
    :update sketch/step
    :draw sketch/draw
    :middleware [qm/fun-mode]
    :features [:no-bind-output]
    :key-pressed sketch/key-down
    :key-released sketch/key-up))

(defmethod ig/init-key :debug-sketch [_ {:keys [conn machine]}]
  (q/sketch
    :size sketch/dimensions
    :setup #(sketch/setup machine)
    :update (partial sketch/debug-step conn)
    :draw sketch/draw
    :middleware [qm/fun-mode]
    :features [:no-bind-output]
    :key-pressed sketch/key-down
    :key-released sketch/key-up))


(defmethod ig/halt-key! :sketch [_ ^PApplet applet]
  (.dispose applet))

(defmethod ig/halt-key! :debug-sketch [_ ^PApplet applet]
  (.dispose applet))
