(ns space-invaders-clj.machine
  (:require
    [i8080-clj.api :as cpu]
    [i8080-clj.opfns :refer [>> << |] :as opfns]
    [taoensso.timbre :as log]
    [taoensso.tufte :refer [p]]))

(def refresh-rate 16667)

(def initial-machine
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
   :space 0x10 ; player 1 shoot
   :left  0x20 ; player 1 move left
   :right 0x40 ; player 1 move right

   ; https://github.com/quil/quil/pull/264
   (keyword " ") 0x10 ; player 1 shoot
   })

(defn key-down
  [machine k]
  (when (keymap k)
    (swap! machine update :mach/port1 bit-or (keymap k))))

(defn key-up
  [machine k]
  (when (keymap k)
    (swap! machine update :mach/port1 bit-and (- 0xff (long (keymap k))))))

(defn handle-in
  "- Reading from port 1 puts the value of port 1 into register a
   - Reading from port 3 puts the shifted result into register a"
  [machine ^long port]
  (let [{:keys [mach/sh-lo mach/sh-hi ^long mach/sh-off mach/port1]} @machine]
    (case port
      0 (swap! machine assoc :cpu/a 1)
      1 (swap! machine assoc :cpu/a port1)
      3 (let [x (| (<< sh-hi 8) sh-lo)
              sh-x (bit-and (long (>> x (- 8 sh-off))) 0xff)]
          (swap! machine assoc :cpu/a sh-x))
      ; else
      nil)))

(defn handle-out
  "- Writing to port 2 stores the 3-bit shift offset from register a
   - Writing to port 4 performs the shift operation"
  [machine ^long port]
  (let [{:keys [mach/sh-hi ^long cpu/a]} @machine]
    (case port
      2 (swap! machine assoc :mach/sh-off (bit-and a 0x7))
      4 (swap! machine assoc :mach/sh-lo sh-hi, :mach/sh-hi a)
      ; else
      nil)))

(defn handle-interrupt
  [machine]
  (let [{:keys [^long mach/cycles mach/int-code cpu/int-enable?]} @machine
        interrupt? (and int-enable? (>= cycles (long refresh-rate)))]
    (cond-> machine
      (and interrupt? (= 1 int-code))
      (swap! merge (apply dissoc (opfns/interrupt @machine 1) (keys initial-machine)) {:mach/cycles 0, :mach/int-code 2})

      (and interrupt? (= 2 int-code))
      (swap! merge (apply dissoc (opfns/interrupt @machine 2) (keys initial-machine)) {:mach/cycles 0, :mach/int-code 1}))))

(defn step-cpu
  ""
  [machine]
  (handle-interrupt machine)
  (let [{:keys [op args size cycles] :as opmap} (cpu/disassemble-op @machine)]
    (log/trace (format "%04x" (:cpu/pc @machine)) op (mapv (partial format "0x%02x") args))
    (cond
      (= :IN op)
      (do (handle-in machine (first args))
          (swap! machine
                 (fn [m] (-> (assoc m :mach/last-op op, :mach/last-args args)
                             (update :cpu/pc + size)
                             (update :mach/cycles + cycles)))))

      (= :OUT op)
      (do (handle-out machine (first args))
          (swap! machine
                 (fn [m] (-> (assoc m :mach/last-op op, :mach/last-args args)
                             (update :cpu/pc + size)
                             (update :mach/cycles + cycles)))))

      :else
      (swap! machine
             (fn [m]
               (let [new-m (cpu/execute-op m opmap)]
                 (-> m
                     (assoc
                       :cpu/a (:cpu/a new-m)
                       :cpu/b (:cpu/b new-m)
                       :cpu/c (:cpu/c new-m)
                       :cpu/d (:cpu/d new-m)
                       :cpu/e (:cpu/e new-m)
                       :cpu/h (:cpu/h new-m)
                       :cpu/l (:cpu/l new-m)
                       :cpu/pc (:cpu/pc new-m)
                       :cpu/sp (:cpu/sp new-m)
                       :cpu/mem (:cpu/mem new-m)
                       :cpu/int-enable? (:cpu/int-enable? new-m)
                       :cpu/nopc? (:cpu/nopc? new-m)
                       :cpu/cc/ac (:cpu/cc/ac new-m)
                       :cpu/cc/cy (:cpu/cc/cy new-m)
                       :cpu/cc/p (:cpu/cc/p new-m)
                       :cpu/cc/s (:cpu/cc/s new-m)
                       :cpu/cc/z (:cpu/cc/z new-m)
                       :mach/last-op op
                       :mach/last-args (or args []))
                     (update :mach/cycles + cycles))))))))
