(ns schema-server.schemas
  (:require
   [camel-snake-kebab.core :as csk]
   [schema-server.io :as sio]
   [schema.core :as s :include-macros true]
   [schema-generators.generators :as s-gen]
   ["ajv" :as Ajv]
   ["json-schema-faker" :as jsf]))

(def $SCHEMA-RESOURCES-DIRECTORY
  (sio/join-path
   (.cwd js/process)
   "resources"))

(def $prismatic-schema-mapping
  {:s/Str s/Str
   :s/Int s/Int
   :s/Num s/Num
   :s/Bool s/Bool
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

(defrecord SchemaFile [name format path source definition])
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
                      (keyword))

        source-data (case extension
                      :edn (cljs.reader/read-string file-content)
                      :json (js->clj (.parse js/JSON file-content))
                      file-content)]
    (SchemaFile.
     (csk/->PascalCase fbasename)
     extension
     (clojure.string/replace
      fpath
      (re-pattern (str "^" $SCHEMA-RESOURCES-DIRECTORY))
      "$resources")
     source-data
     (if (= extension :edn)
       (resolve-prismatic-schema-values
        source-data)
       source-data))))

(defn -plumatic-schema->json-schema [schema]                                                                                                               
  (let [primitive-type-mapping
        {s/Str {"type" "string"}
         s/Num {"type" "number"}
         s/Int {"type" "integer"}}]
    (cond (get primitive-type-mapping schema)
          (primitive-type-mapping schema)
          
          (sequential? schema)
          {"type" "array"
           "items" (-plumatic-schema->json-schema
                    (first schema))}
          
          (map? schema)
          {"type" "object"
           "properties" (->> schema
                             (map (fn [[k v]]
                                    [(-plumatic-schema->json-schema k)
                                     (-plumatic-schema->json-schema v)]))
                             (into {}))}
          
          :else
          schema)))

(defn plumatic-schema->json-schema [schema-mixed]
  (let [schema (case schema-mixed
                 s/Num s/Num
                 s/Int s/Int
                 s/Str s/Str
                 schema-mixed)]
    (-> schema
        (-plumatic-schema->json-schema)
        (merge {"$schema"
                "http://json-schema.org/draft-07/schema#"}))))

(def $json-schema->plumatic-mapping
  {"string" :s/Str
   "boolean" :s/Bool
   "number" :s/Num
   "integer" :s/Int})

(defn json-schema->plumatic-schema [js-data]
  (let [data-type (get js-data "type")]
    (case data-type
      "object"
      (->> (get js-data "properties")
           (map (fn [[property-name
                      property-schema]]
                  [property-name
                   (json-schema->plumatic-schema property-schema)]))
           (into {}))

      "array"
      [(json-schema->plumatic-schema
        (get js-data "items"))]

      ;; primitive
      ($json-schema->plumatic-mapping data-type "UNKNOWN"))))

(defn wrapped-g-generate [schema]
  (cond (or (instance? s/Predicate schema)
            (instance? s/EnumSchema schema))
        (s-gen/generate schema)
        
        (map? schema)
        (->> schema
             (map (fn [[k v]]
                    [k (wrapped-g-generate v)]))
             (into {}))

        :else
        schema))

(defn generate-data-for-schema [{:keys [definition format]
                                 :as schema-file}]
  (case format
    :edn (wrapped-g-generate definition)
    :json (.generate jsf (clj->js definition))
    nil))

(defn validate-js-data-with-schema [js-data js-schema]
  (let [ajv (Ajv.)
        is-valid? (.validate ajv js-schema js-data)]
    (if is-valid?
      {:status "ok"
       :errors []}
      {:status "error"
       :errors (js->clj (aget ajv "errors"))})))

(defn validate-data-with-schema [clj-data clj-schema]
  (let [js-schema (clj->js clj-schema)
        js-data (clj->js clj-data)
        ajv (Ajv.)
        is-valid? (.validate ajv js-schema js-data)]
    (if is-valid?
      {:status "ok"
       :errors []}
      {:status "error"
       :errors (-> (aget ajv "errors")
                   (js->clj))})))
