(ns cljs-xstate-example.env
  (:require
    [clojure.tools.logging :as log]
    [cljs-xstate-example.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[cljs-xstate-example started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[cljs-xstate-example has shut down successfully]=-"))
   :middleware wrap-dev})
