(ns clj-firefox-extension.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [clj-firefox-extension.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[clj-firefox-extension started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[clj-firefox-extension has shut down successfully]=-"))
   :middleware wrap-dev})
