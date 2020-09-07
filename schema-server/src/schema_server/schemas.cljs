(ns schema-server.schemas
  (:require
   [camel-snake-kebab.core :as csk]
   [schema-server.io :as sio]
   [schema.core :as s :include-macros true]
   [schema-generators.generators :as g]))

(def $SCHEMA-RESOURCES-DIRECTORY
  (sio/join-path
   (.cwd js/process)
   "resources"))

(def $prismatic-schema-mapping
  {:s/Str s/Str
   :s/Int s/Int
   :s/Num s/Num
   :s/Keyword s/Keyword})

(defn resolve-prismatic-schema-values [v]
  ;; doesn't support complex keys in schemas;
  ;; this is enforced in this reader
  (cond (keyword? v)
        ($prismatic-schema-mapping v v)

        (sequential? v)
        (map resolve-prismatic-schema-values v)
        
        (map? v)
        (->> v
             (map (fn [[k vv]]
                    (assert (keyword? k))
                    [k (resolve-prismatic-schema-values vv)]))
             (into {}))))

(defn load-edn-schema [edn-string]
  (-> (cljs.reader/read-string edn-string)
      (resolve-prismatic-schema-values)))

(defn load-json-schema [json-string]
  (.parse js/JSON json-string))

(defrecord SchemaFile [name format path definition])
(defn read-to-SchemaFile [fpath]
  (let [fname (-> fpath
                  (clojure.string/split #"/")
                  (last))
        [fbasename
         extension-string]
        (clojure.string/split fname #"\." 2)

        file-content (sio/slurp fpath)

        extension (-> extension-string
                      (clojure.string/lower-case)
                      (keyword))]
    (SchemaFile.
     (csk/->PascalCase fbasename)
     extension
     (clojure.string/replace
      fpath
      (re-pattern (str "^" $SCHEMA-RESOURCES-DIRECTORY))
      "$resources")
     (case extension
       :edn (load-edn-schema file-content)
       :json (load-json-schema file-content)
       file-content))))

(defn generate-data-for-schema [schema-file]
  (case (:format schema-file)
    :edn {"EDN" 1}
    :json {"JSON" 1}
    nil))
