(ns space-invaders-clj.machine-test
  (:require
    [clojure.test :refer :all]
    [space-invaders-clj.machine :as machine]))

(deftest handle-in-port3-test
  (let [m {:mach/sh-hi 0xff :mach/sh-lo 0}]
    (= 2r11111111 (:cpu/a (machine/handle-in (atom (assoc m :mach/sh-off 0)) 3)))
    (= 2r11111100 (:cpu/a (machine/handle-in (atom (assoc m :mach/sh-off 2)) 3)))
    (= 2r00000000 (:cpu/a (machine/handle-in (atom (assoc m :mach/sh-off 7)) 3)))))

(deftest handle-out-port2-test
  (let [{:keys [:mach/sh-off] :as new-m}
        (machine/handle-out (atom {:cpu/a 2r110}) 2)]
    (is (= 2r110 sh-off))))

(deftest handle-out-port4-test
  (reduce
    (fn [m [x [exp-hi exp-lo]]]
      (let [{:keys [:mach/sh-lo :mach/sh-hi :cpu/a] :as new-m}
            (machine/handle-out (atom (assoc m :cpu/a x)) 4)]
        (is (= exp-lo sh-lo))
        (is (= exp-hi sh-hi))
        (is (= x a))
        new-m))
    {:mach/sh-lo 0 :mach/sh-hi 0 :cpu/a 0}
    (map vector [0xaa 0xff 0x12] [[0xaa 0] [0xff 0xaa] [0x12 0xff]])))
