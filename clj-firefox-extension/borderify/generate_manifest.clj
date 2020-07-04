(require '[clojure.data.json :as json])

(def $manifest-base
  {:manifest_version 2
   :icons {:48 "icons/border-48.png"}
   :content_scripts
   [{:matches ["*://*.mozilla.org/*"]
     :js ["borderify.js"]}]})

(let [project-clj-tokens (->> (slurp "project.clj")
                              (read-string))
      project (->> project-clj-tokens
                   (drop 3)
                   (apply hash-map)
                   (merge {:name (nth project-clj-tokens 2)
                           :version (nth project-clj-tokens 3)}))

      manifest-json (with-out-str
                      (-> $manifest-base
                          (merge (select-keys
                                  project
                                  [:name :version :description]))
                          (json/pprint)))]

  (println manifest-json)
  (spit "borderify/manifest.json" manifest-json))
