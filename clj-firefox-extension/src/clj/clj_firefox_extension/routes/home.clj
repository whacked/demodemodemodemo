(ns clj-firefox-extension.routes.home
  (:require
   [clj-firefox-extension.layout :as layout]
   [clj-firefox-extension.db.core :as db]
   [clojure.java.io :as io]
   [clj-firefox-extension.middleware :as middleware]
   [ring.util.response]
   [ring.util.http-response :as response]))

(defn home-page [request]
  (layout/render request "home.html"))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/docs" {:get (fn [_]
                    (-> (response/ok (-> "docs/docs.md" io/resource slurp))
                        (response/header "Content-Type" "text/plain; charset=utf-8")))}]])

