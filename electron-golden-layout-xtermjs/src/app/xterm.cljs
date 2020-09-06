(ns app.xterm
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [reagent.core :as r]
            ["chalk" :as chalk]
            ["xterm" :as xterm]
            ["xterm-addon-fit" :as xterm-addon-fit]
            ["xterm-addon-web-links" :as xterm-addon-web-links]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [goog.dom :as gdom]))

(defn nl2cr [s]
  (clojure.string/replace s "\n" "\r\n"))

(def $process-out-channel (chan))

(defn clear-terminal! [term & [ps1]]
  ;; (js/console.clear)
  (.write term "\u001Bc") ;; clears out dangling "clear" text in the terminal
  (.clear term)
  (when ps1
    (.write term ps1)))

(defn setup-terminal! [container
                       command-executor
                       & [after-initialize]]
  (gdom/removeChildren container)
  (let [term (new (aget xterm "Terminal"))
        fit-addon (new (aget xterm-addon-fit "FitAddon"))
        web-links-addon (new (aget xterm-addon-web-links "WebLinksAddon"))

        input-buffer (atom nil)]
    (doto ^js/Object term
      (.setOption "disableStdin" true)
      (.loadAddon fit-addon)
      (.loadAddon web-links-addon)
      (.open container)
      (.onKey
       (fn [ev]
         (try
           (let [key-code (aget ev "domEvent" "which")
                 key-char (aget ev "key")]
             (if (= key-code 13)
               (let [cur-command (some-> @input-buffer
                                         (clojure.string/trim)
                                         (or ""))]
                 (reset! input-buffer "")
                 (if (= cur-command "clear")
                   (clear-terminal! term)
                   (do
                     (command-executor term cur-command)
                     (.writeln term "\n"))))
               (do
                 (.write term key-char)
                 (swap! input-buffer str key-char))
               ))
           (catch js/Object error
             (js/console.warn "onKey error:")
             (js/console.warn error)
             (js/console.info ev))))))
    (.fit ^js/Object fit-addon)
    (go-loop
        []
        (when-let [data (<! $process-out-channel)]
          (.write term (nl2cr (str data)))
          (recur)))
    (when after-initialize
      (after-initialize term))))
