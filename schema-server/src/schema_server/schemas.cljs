(ns schema-server.schemas
  (:require
   [schema.core :as s :include-macros true]
   [schema-generators.generators :as g]))

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
