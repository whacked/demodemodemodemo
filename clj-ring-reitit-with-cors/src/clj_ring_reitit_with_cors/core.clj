(ns clj-ring-reitit-with-cors.core
  (:require [reitit.http :as http]
            [reitit.ring :as ring]
            [reitit.interceptor.sieppari]
            [ring.adapter.jetty :as jetty]
            [hiccup.core :as h]))

(def cors-allowed-headers
  {"Access-Control-Allow-Headers" (->> #{:get :post :options}
                                       (map name)
                                       (map clojure.string/upper-case)
                                       (interpose ", ")
                                       (apply str))
   "Access-Control-Allow-Origin" "*"})

(def app
  (http/ring-handler
   (http/router
    ["/"
     (let [handler
           (fn [ctx]
             {:status 200
              :headers cors-allowed-headers
              :body (h/html
                     [:div
                      [:h3 (str "hello "
                                (name (get-in ctx [:request-method])))]])})
           
           cors-interceptor
           {:enter (fn [{:keys [request] :as ctx}]
                     (assoc ctx
                            ;; this terminates the request chain
                            ;; :queue nil
                            :response {:status 200
                                       :body "hello interceptor"
                                       :headers cors-allowed-headers}))}]
      {:get {:handler handler
             :interceptors [cors-interceptor]}
      
       :post {:handler handler
              :interceptors [cors-interceptor]}})])
    
   (ring/create-default-handler)
   {:executor reitit.interceptor.sieppari/executor}))

(def server (atom nil))

(defn start []
  (reset! server
          (jetty/run-jetty #'app {:port 3000, :join? false, :async? true}))
  (println "server running in port 3000"))

(defn -main
  "entry point"
  [& args]
  (start))

(comment
  (start))

(when false
  (when @server
    (.stop @server)
    (start)))
