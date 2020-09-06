(ns schema-server.server
  (:require-macros
   [hiccups.core :as h])
  (:require
   [hiccups.runtime]
   ["chalk" :as chalk]
   ["fs" :as fs]
   ["path" :as path]
   ["glob" :as node-glob]
   [cljs.core.async :refer [put! chan <! >! timeout close!]]
   [reitit.ring :as ring]
   [reitit.ring.coercion :as rrc]
   [camel-snake-kebab.core :as csk]
   [macchiato.middleware.params :refer [wrap-params]]
   [macchiato.middleware.keyword-params :refer [wrap-keyword-params]]
   [macchiato.middleware.restful-format :as rf]
   [macchiato.server :as server]
   [clojure.string]
   [schema-server.schemas :as schemas]
   [taoensso.timbre :refer [info]]
   [garden.core :as garden]))

(def $SCHEMA-RESOURCES-DIRECTORY
  (.join path
         (.cwd js/process)
         "resources"))

(defn html-response [body]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body body})

(defn hiccup-response [& hiccup-forms]
  (html-response (h/html5 hiccup-forms)))

(defn join-path [& fragments]
  (apply (aget path "join") fragments))

(defn glob [pattern]
  (-> (.sync node-glob pattern)
      (array-seq)))

(defn path-exists? [fpath]
  (.existsSync fs fpath))

(defn slurp [fpath]
  (.readFileSync fs fpath "utf-8"))

(defrecord SchemaFile [name format path definition])
(defn read-to-SchemaFile [fpath]
  (let [fname (-> fpath
                  (clojure.string/split #"/")
                  (last))
        [fbasename
         extension-string]
        (clojure.string/split fname #"\." 2)

        file-content (slurp fpath)

        extension (-> extension-string
                      (clojure.string/lower-case)
                      (keyword))]
    (SchemaFile.
     (csk/->PascalCase fbasename)
     extension
     (clojure.string/replace
      fpath
      (re-pattern (str "^" $SCHEMA-RESOURCES-DIRECTORY))
      "$resources")
     (case extension
       :edn (schemas/load-edn-schema file-content)
       :json (schemas/load-json-schema file-content)
       file-content))))

(defn discover-schemas []
  (->> (join-path
        $SCHEMA-RESOURCES-DIRECTORY "schemas" "*.{edn,json}")
       (glob)
       (map read-to-SchemaFile)))

(def $routes
  ["/"
   ["" {:get {:handler
              (fn [request respond _]
                (respond
                 (hiccup-response
                  [:head
                   [:meta {:content "text/html;charset=utf-8" :http-equiv "Content-Type"}]
                   [:meta {:content "utf-8" :http-equiv "encoding"}]
                   [:style
                    (garden/css
                     [[:* {:margin 0
                           :padding 0}]])]]
                  [:body
                   [:div
                    {:id "app"}
                    "waiting for app to initialize..."]
                   [:script
                    {:type "text/javascript"
                     :src "/js/front.js"}]])))}}]

   ["help"
    {:get {:handler
           (fn [request respond _]
             (respond
              (hiccup-response
               [:head
                [:meta {:content "text/html;charset=utf-8" :http-equiv "Content-Type"}]
                [:meta {:content "utf-8" :http-equiv "encoding"}]
                [:style
                 (garden/css
                  [[:* {:margin 0
                        :padding 0}]])]]
               [:body
                [:div
                 (let [schemas (discover-schemas)]
                   (when-let [entry-keys
                              (some-> (first schemas)
                                      (keys))]
                     [:table
                      {:border 1
                       :width "100%"
                       :style "font-size:small;font-family:monospace;"}
                      [:tbody
                       [:tr
                        (->> entry-keys
                             (map (fn [k]
                                    [:th (name k)])))]
                       (->> schemas
                            (map
                             (fn [schema-entry]
                               [:tr
                                (->> entry-keys
                                     (map
                                      (fn [k]
                                        [:td
                                         (let [value (k schema-entry)]
                                           (case k
                                             :definition
                                             [:textarea
                                              {:readonly "readonly"
                                               :style "width: 100%"}
                                              (case (:format schema-entry)
                                                :edn
                                                (with-out-str
                                                  (cljs.pprint/pprint value))

                                                :json
                                                (.stringify
                                                 js/JSON
                                                 (clj->js value)
                                                 nil 2)

                                                nil)]
                                           
                                             value))])))])))]]))]])))}}]
   
   ["js/"
    ["*.js"
     {:handler
      (fn [request respond _]
        (-> (or (when-let [target-file-name
                           (get-in request [:path-params :.js])]
                  (let [js-path (.join path "app" "js" target-file-name)]
                    (if (path-exists? js-path)
                      (html-response (slurp js-path))
                      {:status 404
                       :body (str "not found: " js-path)})))
                {:status 400
                 :body (str "bad request")})
            (assoc-in [:headers :content-type] "application/javascript")
            (respond)))}]]])

(defn wrap-body-to-params
  [handler]
  (fn [request respond raise]
    (handler
     (-> request
         (assoc-in [:params :body-params] (:body request))
         (assoc :body-params (:body request))) respond raise)))

(def app
  (ring/ring-handler
   (ring/router
    [$routes]
    {:data {:middleware
            [wrap-params
             wrap-keyword-params
             wrap-body-to-params
             rrc/coerce-request-middleware
             rrc/coerce-response-middleware]}})
   (ring/create-default-handler)))

(defn main []
  (js/console.log (.yellow
                   chalk
                   (str
                    (apply str (take 50 (repeat "=")))
                    " OK "
                    (apply str (take 50 (repeat "="))))))

  (let [host "127.0.0.1"
        port 7000]

    (-> {:handler app
         :host    host
         :port    port
         :on-success (fn [& args]
                       (info "server started on "
                             host port))}
        (server/start))))
