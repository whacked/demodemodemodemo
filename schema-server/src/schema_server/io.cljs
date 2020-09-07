(ns schema-server.io
  (:require
   ["fs" :as fs]
   ["path" :as path]
   ["glob" :as node-glob]))

(defn join-path [& fragments]
  (apply (aget path "join") fragments))

(defn glob [pattern]
  (-> (.sync node-glob pattern)
      (array-seq)))

(defn path-exists? [fpath]
  (.existsSync fs fpath))

(defn slurp [fpath]
  (.readFileSync fs fpath "utf-8"))
