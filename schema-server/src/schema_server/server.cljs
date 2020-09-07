(ns schema-server.server
  (:require-macros
   [hiccups.core :as h])
  (:require
   [hiccups.runtime]
   ["chalk" :as chalk]
   [schema-server.io :as sio]
   [cljs.core.async :refer [put! chan <! >! timeout close!]]
   [reitit.ring :as ring]
   [reitit.ring.coercion :as rrc]
   [macchiato.middleware.params :refer [wrap-params]]
   [macchiato.middleware.keyword-params :refer [wrap-keyword-params]]
   [macchiato.middleware.restful-format :as rf]
   [macchiato.server :as server]
   [clojure.string]
   [schema-server.schemas :as schemas]
   [taoensso.timbre :refer [info]]
   [garden.core :as garden]))

(defn html-response [body]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body body})

(defn json-response [body]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body body})

(defn hiccup-response [& hiccup-forms]
  (html-response (h/html5 hiccup-forms)))

(defn discover-schemas []
  (->> (sio/join-path
        schemas/$SCHEMA-RESOURCES-DIRECTORY
        "schemas" "*.{edn,json}")
       (sio/glob)
       (map schemas/read-to-SchemaFile)))

(defn get-matching-schema [schema-name]
  (some->> (discover-schemas)
           (filter (fn [schema]
                     (= schema-name (:name schema))))
           (first)))

(defn to-json [clj-object]
  (.stringify js/JSON (clj->js clj-object) nil 2))

(defn extract-route-structure
  ([route-def]
   (extract-route-structure route-def []))
  ([route-def out]
   (if (sequential? route-def)
     (concat
      out
      (let [route-parent (first route-def)]
        (if (= route-parent "")
          nil
          (->> (rest route-def)
               (map (fn [route-child]
                      (let [subroutes
                            (extract-route-structure route-child)]
                        (if (empty? subroutes)
                          [route-parent]
                          [route-parent subroutes])))))))))))

(defn expand-nested-routes
  ([route-structure]
   (expand-nested-routes
    []
    route-structure))
  ([prefix
    route-structure]
   (if (empty? route-structure)
     nil
     
     (let [head (first route-structure)
           tail (rest route-structure)]
       (if (string? head)
         (if (empty? tail)
           [(conj prefix head)]
           (->> tail
                (map
                 (fn [subroute]
                   (expand-nested-routes
                    (conj prefix head)
                    subroute)))
                (apply concat)))

         (concat
          (expand-nested-routes prefix head)
          (expand-nested-routes prefix tail)))))))

(defn add-help-route [routes]
  (conj
   routes
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
                [:h3 "ROUTES:"]
                [:div
                 [:ul
                  (->> routes
                       (extract-route-structure)
                       (expand-nested-routes)
                       (map (fn [expanded-route]
                              (let [path
                                    (clojure.string/replace
                                     (->> expanded-route
                                          (remove empty?)
                                          (interpose "/")
                                          (apply str "/"))
                                     #"/+" "/")]
                                [:li
                                 [:a {:href path}
                                  path]]))))]]])))}}]))

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

   ["schemas"
    ["/"
     [""
      {:get {:handler
             (fn [request respond _]
               (respond
                (json-response
                 (-> (discover-schemas)
                     (to-json)))))}}]
     [":name"
      [""
       {:get {:handler
              (fn [request respond _]
                (let [schema-name (get-in request [:path-params :name])]
                  (if-let [matching-schema
                           (some-> (get-matching-schema schema-name))]
                    (respond
                     (json-response
                      (-> matching-schema
                          (clj->js)
                          (to-json))))
                    (respond
                     {:status 404
                      :headers {"Content-Type" "text/plain"}
                      :body (str "not found: " schema-name)}))))}}]

      ["/generate-samples"
       {:get {:handler
              (fn [request respond _]
                (if-let [matching-schema
                         (some-> (get-in request [:path-params :name])
                                 (get-matching-schema)
                                 )]
                  (respond
                   (json-response
                    (->> (range 10)
                         (map (fn [i]
                                (:definition
                                 matching-schema)))
                         (clj->js)
                         (to-json))))
                  (respond
                   {:status 404
                    :headers {"Content-Type" "text/plain"}
                    :body "no matching schema"})))}}]

      ["/validate"
       {:post {:handler
               (fn [request respond _]
                 (let [schema-name (get-in request [:path-params :name])]
                   (respond
                    {:status 200
                     :headers {"Content-Type" "text/plain"}
                     :body (str "validator for "
                                schema-name)})))}}]]]]
   
   ["js/"
    ["*.js"
     {:handler
      (fn [request respond _]
        (-> (or (when-let [target-file-name
                           (get-in request [:path-params :.js])]
                  (let [js-path (sio/join-path "app" "js" target-file-name)]
                    (if (sio/path-exists? js-path)
                      (html-response (sio/slurp js-path))
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
    [(add-help-route $routes)]
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
