(ns app.renderer
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [reagent.core :as r]
            [wksymclj.ui.golden-layout :as wk-gl]
            [app.xterm :as axt]
            ["xterm" :as xterm]
            ["xterm-addon-fit" :as xterm-addon-fit]
            ["xterm-addon-web-links" :as xterm-addon-web-links]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [goog.dom :as gdom]))

(def fs (js/require "fs"))
(def path (js/require "path"))
(def chalk (js/require "chalk"))
(def spawn (aget (js/require "child_process") "spawn"))

(def $config (as-> "resources/config.edn" $
               (.readFileSync fs $ "utf-8")
               (cljs.reader/read-string $)))

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
                          (>! axt/$process-out-channel
                              (-> data str axt/nl2cr))))]
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
  (axt/setup-terminal!
   (gdom/getElement "panel-C")
   (fn [term cmd]
     (let [write-to-term!
           (fn [data]
             (.write term (axt/nl2cr (str data))))
           t0 (js/Date.)]
       (exec-process!
        cmd
        :stdout write-to-term!
        :stderr write-to-term!
        :exit
        (fn [code]
          (write-to-term!
           (str "complete: " cmd "\r\n"
                (/ (- (js/Date.) t0) 1000.0) "s\r\n"))
          (write-to-term!
           (str 
            (->> "[term-prefix]"
                 (.red chalk)
                 (.bold chalk)
                 (.italic chalk)) " $ "))))))
   (fn after-initialize [term]
     (let [ps1 (str
                "Hello from "
                (->> "xterm.js"
                     (.red chalk)
                     (.bold chalk)
                     (.italic chalk))
                " $ ")]
      (.write term ps1))))

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

