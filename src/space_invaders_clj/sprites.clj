(ns space-invaders-clj.sprites
  (:require
    [quil.core :as q]))

(defn unpack-sprite
  [^ints mem offs size]
  (vec (for [i (range offs (+ offs size))
             :let [b (aget mem i)]
             j (range 8)]
         (if (bit-test b j) 255 0))))

(defn draw-sprite
  [sprite & [{:keys [scale] :or {scale 1}}]]
  (let [spr-w 8
        spr-h (/ (count sprite) spr-w)
        w (* spr-h scale)
        h (* spr-w scale)]
  (q/sketch
    :features [:no-bind-output]
    :size [w h]
    :setup #(q/no-loop)
    :draw (fn []
            (q/translate 0 h)
            (q/rotate (q/radians -90))
            (let [img (q/create-image spr-w spr-h :rgb)
                  pxs (q/pixels img)]
              (dotimes [i (count sprite)]
                (aset-int pxs i (q/color (sprite i))))
              (q/update-pixels img)
              (q/resize img h w)
              (q/image img 0 0))))))
