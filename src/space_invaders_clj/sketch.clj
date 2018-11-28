(ns space-invaders-clj.sketch
  (:require
    [datascript.core :as d]
    [i8080-clj.opfns :refer [>>]]
    [quil.core :as q]
    [space-invaders-clj.machine :as mach]
    [taoensso.timbre :as log]))

(def w 256)
(def h 224)
(def dimensions [h w])
(def framebuffer 0x2400)

(defn setup [machine]
  (q/frame-rate 60)
  {:image (q/create-image w h :rgb)
   :machine machine})

(defn step [{:keys [machine] :as state}]
  (let [start (System/currentTimeMillis)]
    (loop [m machine]
      (if (> (- (System/currentTimeMillis) start) 50)
        (assoc state :machine m)
        (recur (mach/step-cpu m))))))

(defn debug-step [conn {:keys [machine] :as state}]
  (let [start (System/currentTimeMillis)]
    (loop [m machine]
      (if (> (- (System/currentTimeMillis) start) 50)
        (assoc state :machine m)
        (let [new-m (mach/step-cpu m)]
          (d/transact! conn [(assoc new-m :db/id 1)])
          (recur new-m))))))

(defn draw [{:keys [image machine]}]
  ; rotate image 90 degrees counter-clockwise about the bottom-left corner
  (q/translate 0 (q/height))
  (q/rotate (q/radians -90))
  (q/image image 0 0)
  (let [{:keys [cpu/mem]} machine
        pxs (q/pixels image)]
    (doseq [xy (range 7168) ; 7168 = 224 * 256 / 8
            :let [b (get mem (+ framebuffer xy))]]
      (loop [b b, i 0]
        (when (< i 8)
          (aset-int pxs (+ (* xy 8) i) (q/color ({0 0, 1 255} (bit-and b 1))))
          (recur (>> b 1) (inc i))))))
  (q/update-pixels image))

(defn key-down [state e]
  (update state :machine mach/key-down (:key e)))

(defn key-up [state e]
  (update state :machine mach/key-up (:key e)))
