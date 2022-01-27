(ns app.server
  (:require-macros
     [hiccups.core :as h])
  (:require
     ["fs" :as fs]
     ["path" :as path]
     [hiccups.runtime]
     [reitit.ring :as ring]
     [macchiato.server :as server]
     [garden.core :as garden]))

(defn path-exists? [fpath]
  (.existsSync fs fpath))

(defn join-path [& fragments]
  (apply (aget path "join") fragments))

(defn slurp [fpath]
  (.readFileSync fs fpath "utf-8"))

(def $shadow-config
  (-> (join-path js/__dirname ".." "shadow-cljs.edn")
    (slurp)
    (cljs.reader/read-string)))

(def $front-target-name
  (-> $shadow-config
    (get-in [:builds :main :modules])
    (keys)
    (first)
    (name)))
(def $resources-js-dir (get-in $shadow-config [:builds :main :output-dir]))
(def $public-resources-dir
  (-> $shadow-config
    (:dev-http)
    (vals)
    (first)))
(def $resources-css-dir (join-path $public-resources-dir "css"))

(defn text-response [body]
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body body})

(defn html-response [body]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body body})

(def $home-page-html
  (let [js-path (join-path
                  "/"
                  (clojure.string/replace
                    $resources-js-dir $public-resources-dir "")
                  (str $front-target-name ".js"))
        css-path (join-path
                   "/"
                   (clojure.string/replace
                     $resources-css-dir $public-resources-dir "")
                   (str $front-target-name ".css"))]
    (h/html5
      [:head
       [:meta {:content "text/html;charset=utf-8" :http-equiv "Content-Type"}]
       [:meta {:content "utf-8" :http-equiv "encoding"}]
       [:style
        (garden/css
          [[:* {:margin 0
                :padding 0}]])]
       [:link {:rel "stylesheet" :href css-path}]]
      [:body
       [:div
        {:id "app"}
        "waiting for app to initialize..."]
       [:script
        {:type "text/javascript"
         :src js-path}]])))

(def $home-page-css
  (garden/css
    (let [div-shared
          {:padding "1em"
           :border-radius "1em"}]
      [:div.world
       (merge
         div-shared
         {:background "#4C92CC"})
       [:h1
        (merge
          div-shared
          {:background "#FEDD7A"
           :border "4px solid #1D80C3"
           :color "#BE1D29"})]])))

(defn maybe-file-to-response [file-path content-type]
  (-> (if (path-exists? file-path)
        (text-response (slurp file-path))
        {:status 404
         :body (str "not found: " file-path)})
    (or 
      {:status 400
       :body (str "bad request")})
    (assoc-in [:headers "Content-Type"] content-type)))

(def $routes
  ["/"
   ["" {:get {:handler
              (fn [request respond _]
                (respond (html-response $home-page-html)))}}]

   ["css/"
    ["*.css"
     {:handler
      (fn [request respond _]
        (let [css-file-name (get-in request [:path-params :.css])]
          (if (= css-file-name "front.css")
            (-> (text-response $home-page-css)
              (assoc-in [:headers "Content-Type"] "text/css")
              (respond))
            (-> (maybe-file-to-response
                  (join-path $resources-css-dir
                             )
                  "text/css")
              (respond)))))}]]

   ["js/"
    ["*.js"
     {:handler
      (fn [request respond _]
        (-> (maybe-file-to-response
              (join-path $resources-js-dir
                         (get-in request [:path-params :.js]))
              "application/javascript")
          (respond)))}]]])

(defn setup-app-with-routes [routes]
  (ring/ring-handler
   (ring/router
     [routes]
     {})
    (ring/create-default-handler)))

(def app (setup-app-with-routes $routes))

(defn main []
  (let [host "localhost"
        port 8888]

    (-> {:handler app
         :host    host
         :port    port
         :on-success (fn [& args]
                       (js/console.info "server started on " host port))}
        (server/start))))

