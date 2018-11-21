(ns space-invaders-clj.rom
  (:require [clojure.java.io :as io]))

(defn read-binary-file
  "Reads slurpable file into a vector of bytes"
  [file]
  (with-open [in (io/input-stream file)]
    (let [len (.length (io/file file))
          buf (byte-array len)]
      (.read in buf 0 len)
      (mapv (partial bit-and 0xff) buf))))

(defn load-rom
  [state rom]
  (let [mem-len (count (:cpu/mem state))
        rom (read-binary-file (io/resource rom))
        rom-len (count rom)]
    (vec (concat rom (subvec (:cpu/mem state) rom-len)))))
