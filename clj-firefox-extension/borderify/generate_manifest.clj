(require '[clojure.data.json :as json])

(def $manifest-base
  {:manifest_version 2})

(let [project-clj-tokens (->> (slurp "project.clj")
                              (read-string))
      project (->> project-clj-tokens
                   (drop 3)
                   (apply hash-map)
                   (merge {:name (nth project-clj-tokens 2)
                           :version (nth project-clj-tokens 3)}))

      extension-edn (->> (slurp "incl/firefox/shadow-cljs.edn")
                         (read-string))
      
      extension-build-name "borderify"
      
      manifest-json (with-out-str
                      (-> $manifest-base
                          (merge
                           {:icons {:48 "icons/border-48.png"}
                            :content_scripts
                            [{:matches ["*://*.mozilla.org/*"]
                              :js [(str extension-build-name ".js")]}]})
                          (merge (select-keys
                                  project
                                  [:name :version :description]))
                          (json/pprint)))]

  (println manifest-json)
  (spit (str extension-build-name
             "/manifest.json")
        manifest-json))
