(ns app.renderer
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require ["path" :as path]
            ["url" :as url]
			[reagent.core :as r]
            [wksymclj.ui.golden-layout :as wk-gl]
            ["xterm" :as xterm]
            ["xterm-addon-fit" :as xterm-addon-fit]
            ["xterm-addon-web-links" :as xterm-addon-web-links]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [goog.dom :as gdom]))

(def fs (js/require "fs"))
(def chalk (js/require "chalk"))
(def spawn (aget (js/require "child_process") "spawn"))

(def $config (as-> "resources/config.edn" $
               (.readFileSync fs $ "utf-8")
               (cljs.reader/read-string $)))

(def $process-out-channel (chan))

(defn nl2cr [s]
  (clojure.string/replace s "\n" "\r\n"))

(defn exec-process! [cmd & {:keys [stdout stderr exit]}]
  (let [proc (spawn "bash" (clj->js ["-c" cmd]))
        bind-data-stream!
        (fn [stream func]
          (js-invoke (aget proc stream)
                     "on" "data" func))]
    (when stdout
      (bind-data-stream! "stdout" stdout))
    (when stderr
      (bind-data-stream! "stderr" stdout))
    (when exit
      (.on proc "exit" exit))))

(defn execute-process-capture-stdout! [cmd]
  (let [t0 (js/Date.)
        push-to-term! (fn [data]
                        (go
                          (>! $process-out-channel
                              (-> data str nl2cr))))]
    (exec-process!
      cmd
      :stdout push-to-term!
      :exit (fn [code]
              (->> (str (.bold chalk "\n# finished ")
                        "in "
                        (.yellow chalk (-> (js/Date.)
                                         (- t0)
                                         (/ 1000.0)
                                         (str "s")))
                        ": "
                        (if (= code 0)
                          (.green chalk cmd)
                          (.red chalk cmd))
                        "\n")
                   (push-to-term!))))))

(defn setup-terminal! [container]
  (gdom/removeChildren container)
  (let [term (new (aget xterm "Terminal"))
        fit-addon (new (aget xterm-addon-fit "FitAddon"))
        web-links-addon (new (aget xterm-addon-web-links "WebLinksAddon"))

        input-buffer (atom nil)
        ps1 (str "Hello from " (->> "xterm.js"
                                 (.red chalk)
                                 (.bold chalk)
                                 (.italic chalk)) " $ ")
        exec! (fn [cmd]
                (let [t0 (js/Date.)
                      write-to-term! (fn [data] (.write term (nl2cr (str data))))]
                  (exec-process!
                    cmd
                    :stdout write-to-term!
                    :stderr write-to-term!
                    :exit
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
    (.fit fit-addon)
    (go-loop
      []
      (when-let [data (<! $process-out-channel)]
        (.write term (nl2cr (str data)))
        (recur)))))

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
  (setup-terminal! (gdom/getElement "panel-C"))

  (let [state (r/atom {:input nil})]
    (r/render
      [(fn []
         [:div
          {:style {:width "100%"
                   :padding "0.5em"}}
          [:input
           {:style {:width "100%"}
            :placeholder "type command and hit enter"
            :value (get-in @state [:input] "")
            :on-change (fn [ev]
                         (swap! state assoc-in [:input] (aget ev "target" "value")))
            :on-key-down (fn [ev]
                          (when (= (aget ev "keyCode") 13)
                            (execute-process-capture-stdout! (get-in @state [:input]))
                            (swap! state assoc-in [:input] "")))}]])]
      (gdom/getElement "panel-F"))))

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

