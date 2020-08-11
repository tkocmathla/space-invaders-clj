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

   ; players 1 and 2 keyboard input ports
   :mach/port1 0
   :mach/port2 0})

(def port1-keymap
  {:c     1    ; insert coin
   :2     2    ; 2 player game start
   :1     4    ; 1 player game start
   :space 0x10 ; player 1 shoot
   :left  0x20 ; player 1 move left
   :right 0x40 ; player 1 move right
   })

(def port2-keymap
  {:. 0x10 ; player 2 shoot
   :a 0x20 ; player 2 move left
   :d 0x40 ; player 2 move right
   })

(defn key-down
  [machine k]
  (cond
    (port1-keymap k)
    (swap! machine update :mach/port1 bit-or (port1-keymap k))

    (port2-keymap k)
    (swap! machine update :mach/port2 bit-or (port2-keymap k))))

(defn key-up
  [machine k]
  (cond
    (port1-keymap k)
    (swap! machine update :mach/port1 bit-and (- 0xff (port1-keymap k)))

    (port2-keymap k)
    (swap! machine update :mach/port2 bit-and (- 0xff (port2-keymap k)))))

(defn handle-in
  "- Reading from port 1 puts the value of port 1 into register a
   - Reading from port 2 puts the value of port 2 into register a
   - Reading from port 3 puts the shifted result into register a"
  [machine ^long port]
  (let [{:keys [mach/sh-lo mach/sh-hi ^long mach/sh-off mach/port1 mach/port2]} @machine]
    (case port
      0 (swap! machine assoc :cpu/a 1)
      1 (swap! machine assoc :cpu/a port1)
      2 (swap! machine assoc :cpu/a port2)
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
        interrupt? (and int-enable? (>= cycles refresh-rate))]
    (cond-> machine
      (and interrupt? (= 1 int-code))
      (swap! merge (apply dissoc (opfns/interrupt @machine 1) (keys initial-machine)) {:mach/cycles 0, :mach/int-code 2})

      (and interrupt? (= 2 int-code))
      (swap! merge (apply dissoc (opfns/interrupt @machine 2) (keys initial-machine)) {:mach/cycles 0, :mach/int-code 1}))))

(defn step-cpu
  ""
  [machine]
  (handle-interrupt machine)
  (let [{:keys [op args size ^long cycles] :as opmap} (cpu/disassemble-op @machine)]
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
                 (-> (assoc new-m :mach/last-op op, :mach/last-args (or args []))
                     (update :mach/cycles + cycles))))))))
