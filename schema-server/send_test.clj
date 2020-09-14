#!/usr/bin/env bb

;; bb send_test.clj

(ns send-test
  (:require [babashka.curl :as curl]
            [clojure.edn :as edn]
            [cheshire.core :as json]))

(def $config
  (-> (slurp "config.edn")
      (edn/read-string)))

(def $SERVER-BASE-URL
  (str "http://" (get-in $config [:server :host]) ":" (get-in $config [:server :port])))

(def $demo-schema
  (-> (slurp "resources/schemas/simple-file.edn")
      (edn/read-string)))

(let [test-endpoint (str $SERVER-BASE-URL "/schemas/SimpleFile/validate")
      resp (curl/post
            test-endpoint
            {:body (-> $demo-schema
                       (assoc :path "foo-path"
                              :size 1234
                              :mtime 9999
                              :permissions [2])
                       (json/generate-string))})]
  (-> (:body resp)
      (println)))
