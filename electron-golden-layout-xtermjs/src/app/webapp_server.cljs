(ns app.webapp-server
  (:require-macros
     [cljs.core.async.macros :refer [go go-loop]]
     [hiccups.core :as h])
  (:require
     [hiccups.runtime]
     ["chalk" :as chalk]
     ["fs" :as fs]
     ["path" :as path]
     [cljs.core.async :refer [put! chan <! >! timeout close!]]
     [reitit.ring :as ring]
     [reitit.ring.coercion :as rrc]
     [macchiato.middleware.params
      :refer [wrap-params]]
     [macchiato.middleware.keyword-params
      :refer [wrap-keyword-params]]
     [macchiato.middleware.restful-format :as rf]
     [macchiato.middleware.anti-forgery
      :refer [wrap-anti-forgery]]
     [macchiato.middleware.session
      :refer [wrap-session]]
     [macchiato.server :as server]
     [app.ws-protocol :as proto]
     [taoensso.timbre :refer [info]]
     [garden.core :as garden]

     ["child_process" :refer [spawn]]))

(defn exec-process! [cmd & {:keys [stdout stderr exit]}]
  (let [proc (spawn "bash" (clj->js ["-c" cmd]))
        bind-data-stream!
        (fn [stream func]
          (js-invoke (aget proc stream)
                     "on" "data" func))]
    (when stdout
      (bind-data-stream! "stdout" stdout))
    (when stderr
      (bind-data-stream! "stderr" stdout))
    (when exit
      (.on ^js/Object proc "exit" exit))))

(defn plain-text [body]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body body})

(def $routes
  ["/"
   ["" {:get {:handler
              (fn [request respond _]
                (respond
                 (plain-text
                  (h/html5
                   [:head
                    [:meta {:content "text/html;charset=utf-8" :http-equiv "Content-Type"}]
                    [:meta {:content "utf-8" :http-equiv "encoding"}]
                    [:style
                     (garden/css
                      [[:* {:margin 0
                            :padding 0}]])]]
                   (let []
                     [:body
                      [:link {:rel "stylesheet"
                              :href "/css/combined.css"}]
                      [:div
                       {:id "app"}
                       "waiting for app to initialize..."]
                      [:script
                       {:type "text/javascript"
                        :src "/js/front.js"}]])))))}}]
    
   ["favicon.ico"
    {:get (fn [_ respond _]
            (respond {:status 404
                      :body "no favicon"}))}]
   
   ["js/"
    ["*.js"
     {:handler
      (fn [request respond _]
        (-> (or (when-let [target-file-name
                           (get-in request [:path-params :.js])]
                  (let [js-path (.join path "app" "js" target-file-name)]
                    (if (.existsSync fs js-path)
                      (plain-text (.readFileSync fs js-path "utf-8"))
                      {:status 404
                       :body (str "not found: " js-path)})))
                {:status 400
                 :body (str "bad request")})
            (assoc-in [:headers :content-type] "application/javascript")
            (respond)))}]]

   ["css/combined.css"
    {:get {:handler
           (fn [request respond _]
             (respond
              {:status 200
               :headers {"Content-Type" "text/css"}
               :body (.readFileSync fs "app/css/combined.css" "utf-8")}))}}]])

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
            [wrap-session
             wrap-anti-forgery
             wrap-params
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
        (server/start)
        (server/start-ws
         (fn [{:keys [websocket uri] :as req} respond & _]
           ;; send a random message
           ;; (.send websocket "hello")
           (.on websocket
                "message"
                (fn [json-message]
                  (let [[op body]
                        (proto/decode json-message)]
                    (case op
                      :command_input
                      (do
                        (js/console.log "handling message!"
                                        body)
                        (exec-process!
                         body
                         :stdout (fn [data]
                                   (.send websocket
                                          (proto/encode
                                           [:command_output
                                            (str data)])))
                         :exit (fn [data]
                                 (.send websocket (proto/encode [:command_exit_code data])))))

                      (do (js/console.warn (str "NOOP: " op))))))))))))
