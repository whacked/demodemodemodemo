(ns cljs-xstate-example.core
  (:require
    [reagent.core :as r]
    [reagent.dom :as rdom]
    [goog.dom :as gdom]
    [cljs-xstate-example.env-loader :as eld]
    ["xstate" :as xstate
     :refer (Machine assign)]))

;; ref
;; https://dev.to/robertbroersma/undo-redo-in-react-using-xstate-23j8
;; https://codesandbox.io/s/xstate-undo-redo-i3eiu?file=/src/App.js

(def editorMachine
  (Machine
   (clj->js
    {:id "editor"
     :context {:past []
               :items []
               :future []}
     :initial "normal"
     :states {:normal {:on {:TOGGLE_MODE "turbo"
                            :ADD_SHAPE {:actions ["updatePast" "addShape"]}}}
              :turbo {:on {:TOGGLE_MODE "normal"
                           :ADD_SHAPE {:actions ["updatePast" "addThreeShapes"]}}}}
     :on {:DELETE_SHAPE {:actions ["updatePast" "deleteShape"]}
          :UNDO {:actions ["undo"]}
          :REDO {:actions ["redo"]}}})
   (let [>assign
         (fn >assign [clj-arg]
           (assign (clj->js clj-arg)))]
     (clj->js
      {:actions {:addShape (>assign
                            {:items (fn [ctx e]
                                      (js/console.log "ADD SHAPE" ctx e)
                                      (-> (aget ctx "items")
                                          (js->clj)
                                          (vec)
                                          (conj (aget e "shape"))
                                          (clj->js)))})
                 :addThreeShapes (>assign
                                  {:items (fn [ctx e]
                                            (js/console.log "add3shape" ctx e)
                                            (-> (aget ctx "items")
                                                (js->clj)
                                                (vec)
                                                (conj (aget e "shape"))
                                                (conj (aget e "shape"))
                                                (conj (aget e "shape"))
                                                (clj->js)))})
                 :deleteShape (>assign
                               {:items (fn [ctx e]
                                         (js/console.log "deleteshape" ctx e)
                                         (let [cur-items
                                               (-> (aget ctx "items")
                                                   (js->clj)
                                                   (vec))
                                               click-index (aget e "index")]
                                           (-> (concat (take click-index cur-items)
                                                       (drop (inc click-index) cur-items))
                                               (clj->js))))
                                })
                 :updatePast (>assign
                              {:past (fn [ctx]
                                       (js/console.log "updatepast" ctx)
                                       (-> (aget ctx "past")
                                           (js->clj)
                                           (conj (aget ctx "items"))
                                           (clj->js)))
                               :future []})
                 :undo (>assign
                        (fn [ctx]
                          (js/console.log "undo" ctx)
                          (let [past-length (count (aget ctx "past"))
                                previous (aget ctx "past" (dec past-length))
                                new-past (->> (aget ctx "past")
                                              (array-seq)
                                              (take (dec past-length)))]
                            (clj->js
                             {:past new-past
                              :items previous
                              :future (-> [(aget ctx "items")]
                                          (concat (aget ctx "future")))}))))
                 :redo (>assign
                        (fn [ctx]
                          (js/console.log "redo" ctx)
                          (let [new-future (->> (aget ctx "future")
                                                (array-seq)
                                                (drop 1))]
                            (clj->js
                             {:past (-> (aget ctx "past")
                                        (array-seq)
                                        (concat [(aget ctx "items")]))
                              :items (aget ctx "future" 0)
                              :future new-future}))))}}))))

(defn component [world]
  (let [{:keys [-state -send]} @world
        {:keys [items future past]}
        (js->clj (aget -state "context") :keywordize-keys true)
        ]
    [:div
     [:h1 (str "latest event "
               (:t @world))]
     [:div
      (->> ["blue" "red"]
           (map (fn [color]
                  ^{:key ["button" color]}
                  [:button
                   {:on-click (fn []
                                (-send :ADD_SHAPE {:shape color}))}
                   color])))
      [:button
       {:on-click (fn []
                    (-send :UNDO))
        :disabled (empty? past)}
       "undo"]
      [:button
       {:on-click (fn []
                    (-send :REDO))
        :disabled (empty? future)}
       "redo"]]
     [:div
      {:style {:display "flex"
               :height "100vh"
               :width "100vw"
               :flex-dir "column"
               :border "1px solid red"}}

      [:div
       {:style {:border "1px solid blue"
                :width "10em"
                :display "flex"}}
       ]
      
      [:div
       {:style {:border "2px solid green"
                :display "flex"}}
       [:h4
        (count items)]
      
       [:ol
        {:style {:margin-left "2em"}}
        (->> items
             (map-indexed
              (fn [i item]
                ^{:key [i item]}
                [:li
                 {:style {:border (str "2px solid " item)}
                  :on-click (fn [] (-send :DELETE_SHAPE {:index i}))}
                 [:pre (pr-str item)]])))]]]]))

(defn init! []
  (let [main-component-key
        (-> (eld/get :project-config)
            (get-in [:shadow-cljs :builds])
            (keys)
            (first))

        world (r/atom {:t nil})]
    
    (swap! world assoc
           :-state (aget editorMachine "initialState")
           :-send (fn send [action-type & [payload]]
                    (let [cur-state (:-state @world)]
                      (swap! world assoc
                             :t (str (js/Date.))
                             :-state (.transition
                                      editorMachine
                                      cur-state
                                      (clj->js (merge {:type action-type}
                                                      payload)))))))
    
    (rdom/render
     [component world]
     (gdom/getElement
      (name main-component-key)))))

(init!)
