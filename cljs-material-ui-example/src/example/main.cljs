(ns example.main
  (:require
     [reagent.core :as r]
     [goog.dom :as gdom]
     ["@material-ui/core/Button" :default Button]
     ["@material-ui/core" :as mui]))

(defonce app-state
  (r/atom {:reloads []
           :button-click-counter 0
           :switch-toggled? false}))

(defn component []
  (let [reloads (get-in @app-state [:reloads])]
    [:div
     [:h1 "MUI interop example"]
     [:h2 "reloaded: " (count reloads) " times."]
     (when-not (empty? reloads)
       [:h3 "last reloaded: "
        (str (last reloads))])
     [:h2 "material ui"]
     [:div
      [:> Button
       {:variant "contained"
        :color "primary"
        :on-click (fn []
                    (swap! app-state
                           update :button-click-counter inc))}
       (str
         "I have been clicked "
         (get-in @app-state [:button-click-counter])
         " times")]
      [:label
       [:> mui/Switch
        {:checked (get-in @app-state [:switch-toggled?])
         :on-change (fn []
                      (swap! app-state
                             update :switch-toggled? not))}]
       [:code
        "toggle state: "
        [:b (str (get-in @app-state [:switch-toggled?]))]]]]]))

(defn setup-ui! []
  (r/render
    [component]
    (gdom/getElement "app")))

(defn reload! []
  (js/console.warn "RELOADING app...")
  (swap! app-state update :reloads conj (js/Date.))
  (swap! app-state update :switch-toggled? not))

(defn stop! []
  (js/console.warn "STOPPING app..."))

(defn start! []
  (js/console.warn "STARTING app...")
  (setup-ui!))

(defn ^:export init []
  (start!))

