(ns schema-server.front
  (:require
   [goog.dom :as gdom]
   [reagent.dom :as rdom]
   [reagent.core :as r]
   [ajax.core :as ajax]))

(defonce state (r/atom {}))

(defn retrieve-schemas! []
  (ajax/GET
   "/schemas/"
   {:response-format :json
    :keywords? true
    :handler (fn [result]
               (swap! state assoc-in [:schemas] result))}))

(defn main-component []
  [:div
   (when-let [schemas (get-in @state [:schemas])]
     (let [schema-keys (-> schemas
                           (first)
                           (keys))]
       [:table
        {:style {:font-size "small"
                 :font-family "monospace"
                 :width "100%"}}
        [:tbody
         [:tr
          (->> schema-keys
               (map (fn [k]
                      ^{:key [:th k]}
                      [:th (name k)])))]
         (some->> schemas
                  (map-indexed
                   (fn [i schema-entry]
                     ^{:key [:schema i]}
                     [:tr
                      (->> schema-keys
                           (map-indexed
                            (fn [j k]
                              ^{:key [:schema i j]}
                              [:td
                               (let [value (k schema-entry)]
                                 (case k
                                   :name
                                   [:a
                                    {:href (str "/schemas/" value)}
                                    value]

                                   :definition
                                   [:textarea
                                    {:read-only "readonly"
                                     :style {:width "100%"}
                                     :value
                                     (case (:format schema-entry)
                                       "edn"
                                       (with-out-str
                                         (cljs.pprint/pprint value))

                                       "json"
                                       (.stringify
                                        js/JSON
                                        (clj->js value)
                                        nil 2)
                                       
                                       nil)}]
                                   
                                   value))])))])))]]))])

(defn reload! []
  (retrieve-schemas!)
  (rdom/render
   [main-component]
   (gdom/getElement "app")))

(defn init! []
  (js/setInterval
   (fn []
     (swap! state assoc-in [:blah] (str (js/Date.))))
   1000)
  (retrieve-schemas!)

  (rdom/render
   [main-component]
   (gdom/getElement "app")))
