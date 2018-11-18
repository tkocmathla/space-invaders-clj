(ns space-invaders.sketch
  (:require
    [i8080-clj.opfns :refer [>>]]
    [quil.core :as q]))

(def w 256)
(def h 224)
(def dimensions [h w])

(defn setup [state-fn]
  (q/frame-rate 60)
  {:image (q/create-image w h :rgb)
   :state-fn state-fn})

(defn draw [{:keys [image state-fn]}]
  ; rotate image 90 degrees counter-clockwise about the bottom-left corner
  (q/translate 0 (q/height))
  (q/rotate (q/radians -90))
  (q/image image 0 0)
  (let [{:keys [cpu/mem]} (state-fn)
        pxs (q/pixels image)]
    (doseq [xy (range 7168) ; 7168 = 224 * 256 / 8
            :let [b (get mem (+ 0x2400 xy))]]
      (loop [b b, i 0]
        (when (< i 8)
          (aset-int pxs (+ (* xy 8) i) (q/color ({0 0, 1 255} (bit-and b 1))))
          (recur (>> b 1) (inc i))))))
  (q/update-pixels image))
