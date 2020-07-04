(ns clj-firefox-extension.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[clj-firefox-extension started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[clj-firefox-extension has shut down successfully]=-"))
   :middleware identity})
