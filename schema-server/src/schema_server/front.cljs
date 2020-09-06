(ns schema-server.front
  (:require
   [goog.dom :as gdom]
   [reagent.dom :as rdom]
   [reagent.core :as r]))

(defn init! []
  (rdom/render
   [:div
    "initialization complete"]
   (gdom/getElement "app")))

