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
   :mach/sh-off 0})

(defn handle-in
  "- Reading from port 3 returns the shifted result (into register a)"
  [{:keys [mach/sh-lo mach/sh-hi mach/sh-off] :as machine} ^long port]
  (case port
    0 (assoc machine :cpu/a 1)
    1 (assoc machine :cpu/a 0)
    3 (let [x (| (<< sh-hi 8) sh-lo)
            sh-x (bit-and (>> x (- 8 sh-off)) 0xff)]
        (assoc machine :cpu/a sh-x))
    ; else
    machine))

(defn handle-out
  "- Writing to port 2 stores the 3-bit shift offset from register a
   - Writing to port 4 performs the shift operation"
  [{:keys [mach/sh-hi] :as machine} ^long port]
  (let [a (get machine :cpu/a)]
    (case port
      2 (assoc machine :mach/sh-off (bit-and a 0x7))
      4 (assoc machine :mach/sh-lo sh-hi, :mach/sh-hi a)
      ; else
      machine)))

(defn handle-interrupt
  [machine]
  (let [{:keys [mach/cycles mach/int-code cpu/int-enable?]} machine
        interrupt? (and int-enable? (>= cycles refresh-rate))]
    (cond-> machine
      (and interrupt? (= 1 int-code))
      (merge (cpu/interrupt machine 1) {:mach/cycles 0, :mach/int-code 2})

      (and interrupt? (= 2 int-code))
      (merge (cpu/interrupt machine 2) {:mach/cycles 0, :mach/int-code 1}))))

(defn step-cpu
  ""
  [machine]
  (let [m (handle-interrupt machine)
        {:keys [op args cycles] :as opmap} (cpu/disassemble-op m)]
    (log/trace (format "%04x" (get m :cpu/pc)) op args)
    (-> m
        (assoc :mach/last-op op, :mach/last-args (or args []))
        (update :mach/cycles + cycles)
        (cond->
          (= :IN op)
          (-> (handle-in (first args)) (update :cpu/pc + 2))

          (= :OUT op)
          (-> (handle-out (first args)) (update :cpu/pc + 2))

          :else
          (merge (cpu/execute-op m opmap))))))
