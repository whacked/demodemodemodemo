(ns app.renderer
  (:require ["path" :as path]
            ["url" :as url]
			[reagent.core :as r]
            [wksymclj.ui.golden-layout :as wk-gl]
            ["xterm" :as xterm]
            ["xterm-addon-fit" :as xterm-addon-fit]
            ["xterm-addon-web-links" :as xterm-addon-web-links]
            [goog.dom :as gdom]))

(def fs (js/require "fs"))
(def spawn (aget (js/require "child_process") "spawn"))

(def $config (as-> "resources/config.edn" $
               (.readFileSync fs $ "utf-8")
               (cljs.reader/read-string $)))

(defn setup-terminal! [container]
  (gdom/removeChildren container)
  (let [term (new (aget xterm "Terminal"))
        fit-addon (new (aget xterm-addon-fit "FitAddon"))
        web-links-addon (new (aget xterm-addon-web-links "WebLinksAddon"))

        input-buffer (atom nil)
        ps1 (str "Hello from \u001B[1;3;31mxterm.js\u001B[0m $ ")
        nl2cr (fn [s] (clojure.string/replace s "\n" "\r\n"))
        exec! (fn [cmd]
                (let [t0 (js/Date.)
                      proc (spawn "bash" (clj->js ["-c" cmd]))
                      bind-data-stream!
                      (fn [stream func]
                        (js-invoke (aget proc stream)
                                   "on" "data" func))]
                  (bind-data-stream!
                    "stdout"
                    (fn [data]
                      (.write term (nl2cr (str data)))))
                  (bind-data-stream!
                    "stderr"
                    (fn [data]
                      (.write term (nl2cr (str data)))))
                  (.on proc "exit"
                       (fn [code]
                         (.write term
                                 (str "complete: " cmd "\r\n"
                                      (/ (- (js/Date.) t0) 1000.0) "s\r\n"))
                         (.write term ps1)))))
        ]
    (doto term
      (.setOption "disableStdin" true)
      (.loadAddon fit-addon)
      (.loadAddon web-links-addon)
      (.open container)
      (.write ps1)
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
                    (do
                      (js/console.clear)
                      (.write term "\u001Bc")  ;; clears out dangling "clear" text in the terminal
                      (.clear term)
                      (.write term ps1))
                    (do
                      (exec! cur-command)
                      (.writeln term "\n"))))
                (do
                  (.write term key-char)
                  (swap! input-buffer str key-char))
                ))
            (catch js/Object error
              (js/console.warn "onKey error:")
              (js/console.warn error)
              (js/console.info ev))))))
    (.fit fit-addon)))

(defn initialize-panels! []
  (let [state (r/atom {:acontent "apple"
                       :bcontent "banana"})]

    (r/render
      [(fn []
         [:h1 (get-in @state [:acontent])] )]
      (gdom/getElement "panel-A"))
    (r/render
      [(fn []
         [:h1 (get-in @state [:bcontent])] )]
      (gdom/getElement "panel-B"))
    (r/render
      [(fn []
         [:div
          [:input
           {:value (get-in @state [:acontent])
            :on-change (fn [evt]
                         (swap! state assoc :acontent (aget evt "target" "value")))}]
          [:input
           {:value (get-in @state [:bcontent])
            :on-change (fn [evt]
                         :value (get-in @state [:bcontent])
                         (swap! state assoc :bcontent (aget evt "target" "value")))}]]
         )]
      (gdom/getElement "panel-D")))
  (setup-terminal! (gdom/getElement "panel-C")))

(defn start []
  (js/console.log "renderer - start")
  (wk-gl/setup-react-layout!
    (gdom/getElement "main")
    :layout (:layout $config)
    :on-complete (fn []
                   (wk-gl/init-window-event-handers!)
                   (initialize-panels!))))

(defn init []
  (js/console.log "renderer - init")
  ;; init is only called once, live reload will call stop then start
  (start))

(defn stop []
  (js/console.log "renderer - stop"))

