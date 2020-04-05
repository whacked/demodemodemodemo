(ns app.pack
  (:require
    [shadow.cljs.devtools.api :as shadow]
    [clojure.java.shell :refer [sh]]))

(def $COMBINED-CSS-DIRECTORY (str "app/css"))
(def $COMBINED-CSS-FILE-PATH (str $COMBINED-CSS-DIRECTORY "/combined.css"))

(defn css []
  (sh "mkdir" "-p" $COMBINED-CSS-DIRECTORY)
  (->> ["node_modules/xterm/css/xterm.css"
        "node_modules/golden-layout/src/css/goldenlayout-base.css"
        "node_modules/golden-layout/src/css/goldenlayout-light-theme.css"
        ]
       (map slurp)
       (interpose "\n")
       (apply str)
       (spit $COMBINED-CSS-FILE-PATH)))
