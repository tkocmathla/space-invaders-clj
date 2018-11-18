(ns space-invaders-clj.machine
  (:require
    [i8080-clj.core :as cpu]
    [i8080-clj.opfns :refer [>> << |] :as opfns]))

(def ^:dynamic *debug* false)

(def machine
  {; interrupt code
   :mach/int-code 1

   ; count of cycles executed since last interrupt
   :mach/cycles 0

   ; lsb, msb, and offset of external shift hardware
   :mach/sh-lo 0
   :mach/sh-hi 0
   :mach/sh-off 0})

(defn handle-in
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
  [{:keys [mach/sh-hi] :as machine} ^long port]
  (let [a (get machine :cpu/a)]
    (case port
      2 (assoc machine :mach/sh-off (bit-and a 0x7))
      4 (assoc machine :mach/sh-lo sh-hi :mach/sh-hi a)
      ; else
      machine)))

(defn handle-interrupt
  [machine]
  (let [{:keys [mach/cycles mach/int-code cpu/int-enable?]} machine
        interrupt? (and int-enable? (>= cycles 16667))]
    (cond-> machine
      (and interrupt? (= 1 int-code))
      (merge (cpu/interrupt machine 1) {:mach/cycles 0, :mach/int-code 2})

      (and interrupt? (= 2 int-code))
      (merge (cpu/interrupt machine 2) {:mach/cycles 0, :mach/int-code 1}))))

(defn step-cpu
  ""
  [machine]
  (let [{:keys [mach/cycles] :as m} (handle-interrupt machine)
        op (cpu/disassemble-op m)]
    (when *debug*
      (println (format "%04x" (get m :cpu/pc)) (:op op) (:args op)))
    (cond-> (update m :mach/cycles + (:cycles op))
      (= :IN (:op op))
      (-> (handle-in (first (:args op)))
          (update :cpu/pc + 2))

      (= :OUT (:op op))
      (-> (handle-out (first (:args op)))
          (update :cpu/pc + 2))

      :else
      (merge (cpu/execute-op m op)))))
