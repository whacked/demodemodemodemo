(defproject cljs-xstate-example "0.1.0-SNAPSHOT"

  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies ~(->> ["incl/luminus/dependencies.edn"
                       "incl/proj/dependencies.edn"]
                      (map (comp read-string slurp))
                      (apply concat))

  :min-lein-version "2.0.0"
  
  :source-paths ["src/clj" "src/cljs" "src/cljc"]
  :test-paths ["test/clj"]
  :resource-paths ["resources" "target/cljsbuild"]
  :target-path "target/%s/"
  :main ^:skip-aot cljs-xstate-example.core

  :plugins ~(->> ["incl/luminus/plugins.edn"]
                 (map (comp read-string slurp))
                 (apply concat))
  :clean-targets ^{:protect false}
  [:target-path "target/cljsbuild"]

  :shadow-cljs ~(-> (slurp "incl/luminus/shadow-cljs.edn")
                    (read-string))
  :npm-deps ~(->> ["incl/luminus/npm-deps.edn"]
                  (map (comp read-string slurp))
                  (apply concat))
  :npm-dev-deps ~(->> ["incl/luminus/npm-deps.edn"]
                      (map (comp read-string slurp))
                      (apply concat))
  
  :profiles
  {:dev           [:project/dev :profiles/dev]
   :test          [:project/dev :project/test :profiles/test]

   :project/dev  {:jvm-opts ["-Dconf=dev-config.edn" ]
                  :dependencies ~(->> ["incl/luminus/profiles-project-dev-dependencies.edn"]
                                      (map (comp read-string slurp))
                                      (apply concat))
                  :plugins      ~(->> ["incl/luminus/profiles-project-dev-plugins.edn"]
                                      (map (comp read-string slurp))
                                      (apply concat)) 
                  
                  :source-paths ["env/dev/clj"  "env/dev/cljs" "test/cljs" ]
                  :resource-paths ["env/dev/resources"]
                  :repl-options {:init-ns user
                                 :timeout 120000}
                  :injections [(require 'pjstadig.humane-test-output)
                               (pjstadig.humane-test-output/activate!)]}
   :profiles/dev {}})
