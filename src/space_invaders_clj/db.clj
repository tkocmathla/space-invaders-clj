(ns space-invaders-clj.db)

(def schema
  {:cpu/a           {:db/cardinality :db.cardinality/one}
   :cpu/b           {:db/cardinality :db.cardinality/one}
   :cpu/c           {:db/cardinality :db.cardinality/one}
   :cpu/d           {:db/cardinality :db.cardinality/one}
   :cpu/e           {:db/cardinality :db.cardinality/one}
   :cpu/h           {:db/cardinality :db.cardinality/one}
   :cpu/l           {:db/cardinality :db.cardinality/one}
   :cpu/sp          {:db/cardinality :db.cardinality/one}
   :cpu/pc          {:db/cardinality :db.cardinality/one}
   :cpu/mem         {:db/cardinality :db.cardinality/one}
   :cpu/int-enable? {:db/cardinality :db.cardinality/one}
   :cpu/nopc?       {:db/cardinality :db.cardinality/one}

   :cpu/cc/z  {:db/cardinality :db.cardinality/one}
   :cpu/cc/s  {:db/cardinality :db.cardinality/one}
   :cpu/cc/p  {:db/cardinality :db.cardinality/one}
   :cpu/cc/cy {:db/cardinality :db.cardinality/one}
   :cpu/cc/ac {:db/cardinality :db.cardinality/one}

   :mach/int-code {:db/cardinality :db.cardinality/one}
   :mach/cycles   {:db/cardinality :db.cardinality/one}
   :mach/sh-lo    {:db/cardinality :db.cardinality/one}
   :mach/sh-hi    {:db/cardinality :db.cardinality/one}
   :mach/sh-off   {:db/cardinality :db.cardinality/one}})
