(ns clj-firefox-extension.app
  (:require [clj-firefox-extension.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
