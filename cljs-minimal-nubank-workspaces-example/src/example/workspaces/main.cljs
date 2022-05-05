(ns example.workspaces.main
  (:require [nubank.workspaces.core :as ws]
            [nubank.workspaces.model :as wsm]
            [nubank.workspaces.card-types.react :as ct.react]
            [reagent.core :as r]))

(defonce init (ws/mount))

(ws/defcard hello-world-example-card
  {::wsm/card-width 8
   ::wsm/card-height 12}
  (ct.react/react-card
   (r/as-element
    [:div [:h1 "hello from card"]])))

