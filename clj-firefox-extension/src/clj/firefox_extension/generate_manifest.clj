(ns firefox-extension.generate-manifest
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [firefox-extension.common
             :refer [$extensions-base-dir
                     $manifest-base]]))

(when (empty? *command-line-args*)
  (println "ERROR: need a build name")
  (System/exit 1))

(defn -main [extension-build-name]
  
  (when-not (-> (io/file $extensions-base-dir
                         extension-build-name)
                (.exists))
    (println (str "ERROR: extension " extension-build-name " does not exist"))
    (System/exit 1))

  (println (str "USING BUILD: " extension-build-name "\n"))
  
  (let [project-clj-tokens (->> (slurp "project.clj")
                                (read-string))
        project (->> project-clj-tokens
                     (drop 3)
                     (apply hash-map)
                     (merge {:name (nth project-clj-tokens 2)
                             :version (nth project-clj-tokens 3)}))

        extension-edn (->> (slurp "incl/firefox/shadow-cljs.edn")
                           (read-string))
      
        manifest-json (with-out-str
                        (-> $manifest-base
                            (merge
                             (-> (io/file $extensions-base-dir
                                          extension-build-name
                                          "base-manifest-json.edn")
                                 (slurp)
                                 (read-string)))
                            (merge (select-keys
                                    project
                                    [:name :version :description]))
                            (json/pprint)))]
  
    (println manifest-json)
    (spit (io/file
           $extensions-base-dir
           extension-build-name
           "manifest.json")
          manifest-json)))
