(ns space-invaders-clj.machine
  (:require
    [i8080-clj.core :as cpu]
    [i8080-clj.opfns :refer [>> << |] :as opfns]
    [taoensso.timbre :as log]))

(def refresh-rate 16667)

(def machine
  {; last op and args executed
   :mach/last-op :NIL
   :mach/last-args []

   ; interrupt code
   :mach/int-code 1

   ; count of cycles executed since last interrupt
   :mach/cycles 0

   ; lsb, msb, and offset of external shift hardware
   :mach/sh-lo 0
   :mach/sh-hi 0
   :mach/sh-off 0

   ; keyboard input port
   :mach/port1 0})

(def keymap
  {:c     1    ; insert coin
   :2     2    ; player 2 start
   :1     4    ; player 1 start
   ; TODO https://github.com/quil/quil/issues/262
   (keyword " ") 0x10 ; player 1 shoot
   :left  0x20 ; player 1 move left
   :right 0x40 ; player 1 move right
   })

(defn key-down
  [machine k]
  (cond-> machine
    (keymap k)
    (update :mach/port1 bit-or (keymap k))))

(defn key-up
  [machine k]
  (cond-> machine
    (keymap k)
    (update :mach/port1 bit-and (- 0xff (long (keymap k))))))

(defn handle-in
  "- Reading from port 1 puts the value of port 1 into register a
   - Reading from port 3 puts the shifted result into register a"
  [{:keys [mach/sh-lo mach/sh-hi ^long mach/sh-off mach/port1] :as machine} ^long port]
  (case port
    0 (assoc machine :cpu/a 1)
    1 (assoc machine :cpu/a port1)
    3 (let [x (| (<< sh-hi 8) sh-lo)
            sh-x (bit-and (long (>> x (- 8 sh-off))) 0xff)]
        (assoc machine :cpu/a sh-x))
    ; else
    machine))

(defn handle-out
  "- Writing to port 2 stores the 3-bit shift offset from register a
   - Writing to port 4 performs the shift operation"
  [{:keys [mach/sh-hi ^long cpu/a] :as machine} ^long port]
  (case port
    2 (assoc machine :mach/sh-off (bit-and a 0x7))
    4 (assoc machine :mach/sh-lo sh-hi, :mach/sh-hi a)
    ; else
    machine))

(defn handle-interrupt
  [machine]
  (let [{:keys [^long mach/cycles mach/int-code cpu/int-enable?]} machine
        interrupt? (and int-enable? (>= cycles (long refresh-rate)))]
    (cond-> machine
      (and interrupt? (= 1 int-code))
      (merge (cpu/interrupt machine 1) {:mach/cycles 0, :mach/int-code 2})

      (and interrupt? (= 2 int-code))
      (merge (cpu/interrupt machine 2) {:mach/cycles 0, :mach/int-code 1}))))

(defn step-cpu
  ""
  [machine]
  (let [m (handle-interrupt machine)
        {:keys [op args size cycles] :as opmap} (cpu/disassemble-op m)]
    (log/trace (format "%04x" (get m :cpu/pc)) op (mapv (partial format "0x%02x") args))
    (cond
      (= :IN op)
      (-> (handle-in m (first args))
          (assoc :mach/last-op op, :mach/last-args args)
          (update :cpu/pc + size)
          (update :mach/cycles + cycles))

      (= :OUT op)
      (-> (handle-out m (first args))
          (assoc :mach/last-op op, :mach/last-args args)
          (update :cpu/pc + size)
          (update :mach/cycles + cycles))

      :else
      (-> (cpu/execute-op m opmap)
          (assoc :mach/last-op op, :mach/last-args (or args []))
          (update :mach/cycles + cycles)))))
