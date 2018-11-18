(ns space-invaders.system
  (:require
    [datascript.core :as d]
    [i8080-clj.core :as cpu]
    [integrant.core :as ig]
    [quil.core :as q]
    [quil.middleware :as qm]
    [space-invaders.db :as db]
    [space-invaders.machine :as mach]
    [space-invaders.rom :as rom]
    [space-invaders.sketch :as sketch]))

(def config
  {:db/atom {:rom-file "invaders.rom"}
   :quil/sketch {:conn (ig/ref :db/atom)}})

(def debug-config
  {:db/conn {:schema db/schema, :rom-file "invaders.rom"}
   :quil/sketch {:conn (ig/ref :db/conn)}})

(defmethod ig/init-key :db/atom [_ {:keys [rom-file]}]
  (atom (assoc (merge cpu/cpu mach/machine)
               :cpu/mem (rom/load-rom cpu/cpu rom-file))))

(defmethod ig/init-key :db/conn [_ {:keys [schema rom-file]}]
  (let [conn (d/create-conn schema)]
    (d/transact! conn [(merge cpu/cpu mach/machine)])
    (d/transact! conn [{:db/id 1 :cpu/mem (rom/load-rom cpu/cpu rom-file)}])
    conn))

(defmethod ig/init-key :quil/sketch [_ {:keys [conn]}]
  (let [state-fn (if (instance? datascript.db.DB conn)
                   #(d/pull @conn [:*] 1)
                   #(deref conn))]
    (q/sketch
      :size sketch/dimensions
      :setup (partial sketch/setup state-fn)
      :draw sketch/draw
      :middleware [qm/fun-mode]
      :features [:no-bind-output])))
