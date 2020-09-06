(ns app.webapp-front
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]])
  (:require [reagent.core :as r]
            [app.xterm :as axt]
            ["cli-color" :as clc]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [goog.dom :as gdom]
            [app.ws-protocol :as proto]
            [clojure.string]
            [goog.string :as gstring]
            [wscljs.client :as ws]
            [wscljs.format :as fmt]))


(def $websocket-server-address "ws://localhost:7000")

(defn make-ps1-string
  ([] (make-ps1-string nil))
  ([previous-exit-code]
   (let [prefix "xterm.js"
         formatter
         (comp
          #(.bold clc %)
          (case previous-exit-code
            nil #(.blue clc %)
            0   #(.green clc %)

            #(.red clc %)))]
     
     (str (formatter "xterm.js") " $ "))))

(defn setup-socket-terminal [element]
  (let [socket (atom nil)]
    (axt/setup-terminal!
     element
     (fn [term cmd]
       (when @socket
         (ws/send
          @socket
          (doto
              (proto/encode
               [:command_input cmd])
              (js/console.log))
          fmt/identity)))
     (fn after-initialize [term]
       (.write term (make-ps1-string))
       (reset!
        socket
        (ws/create
         $websocket-server-address
         {:on-message (fn [js-data]
                        (when-let [message (aget js-data "data")]
                          (try
                            (when-let [[op payload]
                                       (proto/decode message)]
                              (case op
                                :command_output
                                (.writeln
                                 term
                                 (axt/nl2cr
                                  (clojure.string/trim payload)))
                              
                                :command_exit_code
                                (.write term (make-ps1-string payload))
                              
                                (js/console.warn (str "NOOP: " op))))
                            (catch js/Object e
                              (js/console.warn
                               (str "unhandled message:\n"
                                    message))
                              (js/console.info e)))))
          :on-open    #(prn "opening socket connection")
          :on-close   #(prn "closing socket connection")}))))))

(defn ^:export init! []
  (let []
    (r/render
     [:div
      [:div
       {:style {:width "100%"
                :height "50em"}
        :ref (fn [element]
               (setup-socket-terminal element))}]]
     (gdom/getElement "app"))))

