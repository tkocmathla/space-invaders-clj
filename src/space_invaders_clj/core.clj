(ns space-invaders-clj.core
  (:gen-class)
  (:require
    [integrant.core :as ig]
    [space-invaders-clj.machine :as machine]
    [space-invaders-clj.system :as system]))

(defn -main []
  (let [system (ig/init system/config)]
    (future (dorun (repeatedly #(machine/step-cpu (:machine system)))))))
