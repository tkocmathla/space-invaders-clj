(ns space-invaders-clj.sketch
  (:require
    [datascript.core :as d]
    [i8080-clj.opfns :refer [>> get-byte]]
    [quil.core :as q]
    [space-invaders-clj.machine :as mach]))

(def w 224)
(def h 256)
(def dimensions [w h])
(def framebuffer 0x2400)

(defn color [byte-pos bit-offs bit-val]
  (cond
    (not (bit-test bit-val bit-offs)) (q/color 0)
    ; player's green transparency
    (< 0 (mod byte-pos 32) 10) (q/color 0 255 0)
    ; flying saucer's red transparency
    (< 24 (mod byte-pos 32) 28) (q/color 255 0 0)
    :else (q/color 255)))

(defn setup
  "Sketch setup function.

  The image is created with the orientation of the framebuffer in memory, and
  is rotated at draw time to appear correctly."
  [scale machine]
  (q/frame-rate 30)
  {:image (q/create-image h w :rgb)
   :scale scale
   :machine machine})

(defn draw
  "Draws values from the machine framebuffer to image pixels"
  [{:keys [image scale machine]}]
  ; translate to bottom of screen, then rotate 90 degrees counter-clockwise
  ; about the bottom-left corner to correct framebuffer orientation
  (q/translate 0 (q/height))
  (q/rotate (q/radians -90))
  (q/resize image h w)
  (let [pxs (q/pixels image)]
    (doseq [xy (range 7168) ; 7168 = 224 * 256 / 8
            :let [byt (get-byte @machine (+ framebuffer xy))]
            bit (range 8)]
      (aset-int pxs (+ (* xy 8) bit) (color xy bit byt))))
  (q/update-pixels image)
  (q/resize image (* h scale) 0)
  (q/image image 0 0))

(defn key-down [state e]
  (mach/key-down (:machine state) (:key e))
  state)

(defn key-up [state e]
  (mach/key-up (:machine state) (:key e))
  state)
