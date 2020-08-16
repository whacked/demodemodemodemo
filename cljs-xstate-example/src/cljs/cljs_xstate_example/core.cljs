(ns cljs-xstate-example.core
  (:require-macros
   [odoyle.rules :as odr])
  (:require
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [goog.dom :as gdom]
   [cljs-xstate-example.env-loader :as eld]
   ["xstate" :as xstate
    :refer (Machine assign)]
   ["graphlib-dot" :as dot]
   ["dagre-d3-react" :default DagreGraph]
   [garden.core :as garden]
   [odoyle.rules :as odr]
   
   [schema.core :as s :include-macros true]

   
   ["color-hash" :as ColorHash]
   ["object-hash" :as ObjectHash]
   [thi.ng.color.core :as col]

   [reagent-material-ui.core.switch-component :refer [switch]]
   

   [reagent-material-ui.core.toolbar :refer [toolbar]]
   [reagent-material-ui.icons.add-box :refer [add-box]]
   [reagent-material-ui.icons.clear :refer [clear]]

   [reagent-material-ui.cljs-time-utils :refer [cljs-time-utils]]
   [reagent-material-ui.colors :as colors]
   [reagent-material-ui.core.button :refer [button]]
   [reagent-material-ui.core.chip :refer [chip]]
   [reagent-material-ui.core.css-baseline :refer [css-baseline]]
   [reagent-material-ui.core.grid :refer [grid]]
   [reagent-material-ui.core.menu-item :refer [menu-item]]
   [reagent-material-ui.core.text-field :refer [text-field]]
   [reagent-material-ui.core.textarea-autosize :refer [textarea-autosize]]
   [reagent-material-ui.core.toolbar :refer [toolbar]]
   [reagent-material-ui.icons.add-box :refer [add-box]]
   [reagent-material-ui.icons.clear :refer [clear]]
   [reagent-material-ui.icons.face :refer [face]]
   [reagent-material-ui.pickers.date-picker :refer [date-picker]]
   [reagent-material-ui.pickers.mui-pickers-utils-provider :refer [mui-pickers-utils-provider]]
   [reagent-material-ui.styles :as styles]
   )
  (:import (goog.i18n DateTimeSymbols_en_US)))




(defn to-hex-color [object]
  ;; dupe of wksymclj
  (let [hash (ObjectHash (clj->js object))]
    (-> (new ColorHash)
        (.hex hash))))

(defn to-hex-color-light [object]
  (let [hash (ObjectHash (clj->js object))]
    (-> (new ColorHash (clj->js {:lightness 0.9}))
        (.hex hash))))
(defn to-hex-color-dark [object]
  (let [hash (ObjectHash (clj->js object))]
    (-> (new ColorHash (clj->js {:lightness 0.3}))
        (.hex hash))))

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

(s/defrecord Setting
    [coordinate  :- s/Keyword
     value       :- s/Any
     description :- s/Str
     restriction :- s/Keyword
     ])

(def odr-AlphaNodeSchema
  (->> (odr/map->AlphaNode {})
       (map (fn [[k _]]
              [k
               (case k
                 :children    [s/Any]
                 :successors  [s/Any]
                 :facts       s/Any
                 :test-field  (s/enum :id :attr :value)
                 :path        s/Any
                 :test-value  s/Keyword)
               ]))
       (into {})))

(def odr-BindingSchema
  (->> (odr/map->Binding {})
       (map (fn [[k _]]
              [k s/Any]))
       (into {})))

(def odr-Condition
  (->> (odr/map->Condition {})
       (map (fn [[k _]]
              [k
               (case k
                 :nodes    [odr-AlphaNodeSchema]
                 :bindings [odr-BindingSchema]
                 :opts     s/Any)]))
       (into {})))

(def CustomRuleDefinition
  {:description              s/Str
   :odr.Conditions           [odr-Condition]
   (s/optional-key :handler) s/Any
   (s/optional-key :filter)  s/Any
   })

(def SettingCoordinateSchema
  [(s/one s/Keyword "id")
   (s/one s/Keyword "attr")])

(defn AlphaNodes->Coord [alpha-nodes]
  (when (= 2 (count alpha-nodes))
    [(:test-value (first alpha-nodes))
     (:test-value (second alpha-nodes))]))

(defn SettingCoordinate->AlphaNodes [setting-coordinate]
  {:pre [(s/validate SettingCoordinateSchema setting-coordinate)]}
  (let [[node-id-key node-attr-key] setting-coordinate]
   [(odr/map->AlphaNode
     {:test-field :id
      :test-value node-id-key
      ;; dunno why, but this one in particular is necessary sometimes,
      ;; (possibly always for id)
      :children []})

    (odr/map->AlphaNode
     {:test-field :attr
      :test-value node-attr-key})]))

(defn odr-QuickBinding [sym]
  (odr/->Binding :value sym (keyword sym)))

(defn odr-QuickCondition
  ([coordinate bind-symbol]
   (odr-QuickCondition coordinate bind-symbol nil))
  ([coordinate bind-symbol opts]
   (odr/->Condition
    (SettingCoordinate->AlphaNodes coordinate)
    [(odr-QuickBinding bind-symbol)]
    opts)))

(def $world (r/atom {:t nil}))

(def GlobalConfig
  (->> [(->Setting [::ui-style ::dark-background] false "dark mode"
                   [:Boolean])
        (->Setting [::ui-style ::black-header] true "black header"
                   [:Boolean])
        (->Setting [::demo-panel ::list-limit] 10 "maximum number of allowed items"
                   [:MinMaxRange [0 20]])
        
        (->Setting [::ui-adaptive-mode ::optimize-for-speed] false "optimize for usage speed"
                   [:Boolean])
        (->Setting [::ui-adaptive-mode ::prefer-keyboard] false "prefer keyboard over mouse"
                   [:Boolean])
        (->Setting [::ui-adaptive-mode ::optimize-for-variety] false "optimize for learning new features"
                   [:Boolean])
        ]
       (map (fn [setting]
              [(:coordinate setting) setting]))
       (into {})
       (assoc {:custom-rules
               (->> [[::warn-low-contrast
                      (s/validate
                       CustomRuleDefinition
                       {:description
                        "cannot have matching background and foreground lightness because it will be hard to read"
                  
                        :odr.Conditions
                        [(odr-QuickCondition [::ui-style ::black-header] 'black-header?)
                         (odr-QuickCondition [::ui-style ::dark-background] 'dark-background?)]
                        
                        :handler (fn cljs-xstate-example-core-warn-low-contrast
                                   [{:keys [dark-background?
                                            black-header?]}]
                                   (js/console.log "CONFLICT")
                                   (swap! $world update :messages
                                          (fn [cur-messages]
                                            (conj (set cur-messages)
                                                  "CONFLICT BACKGROUNDzz"))))
                        
                        :filter (fn [{:keys [dark-background? black-header?]}]
                                  (and black-header? dark-background?))
                        })]

                     [::print-time
                      (s/validate
                       CustomRuleDefinition
                       {:description
                        "print the time!!!"
                        
                        :odr.Conditions
                        [(odr-QuickCondition [::time ::total] 'tt)]

                        :handler (fn [{:keys [tt]}] (println "NOW--" tt))})]

                     [::show-alert
                      (s/validate
                       CustomRuleDefinition
                       {:description
                        "show alert!!!"

                        :odr.Conditions
                        [(odr-QuickCondition [::alert ::message] 'msg)]
                        
                        :handler (fn cljs-xstate-example-core-show-alert
                                   [{:keys [msg]}]
                                   (js/alert msg))})]]
                    
                    (into {}))
               }
              :settings)
       (r/atom)))

(s/defrecord StateTree
    [id       :- s/Int
     state    :- s/Any
     children :- []])

(defn state-history-to-graph-state
  ([stree]
   (state-history-to-graph-state stree {:nodes [] :links []}))
  ([stree out]
   (if-not (:id stree)
     out
     (let [to-node (fn [t]
                     (assoc (select-keys t [:id])
                            :label (:id t)))
           next-out
           (apply
            merge-with
            concat
            (-> out
                (update :nodes conj (to-node stree))
                (update :links (fn [cur-links]
                                 (concat
                                  cur-links
                                  (->> (:children stree)
                                       (map (fn [sub-stree]
                                              {:source (:id stree)
                                               :target (:id sub-stree)})))))))
            (->> (:children stree)
                 (map state-history-to-graph-state)))]
       ;; (println "STREE" stree)
       ;; (println "OUT"
       ;;          "\n>  " out
       ;;          "\n>> "
       ;;          next-out)
       next-out))))

(def all-settings-state-history
  (r/atom (->StateTree
           1 (get-in @GlobalConfig [:settings]) [])))

(def graph-state-atom
  (r/atom
   (state-history-to-graph-state @all-settings-state-history)))

(defn push-settings-to-history! []

  (defn update-terminal-leaf
    ([state-tree next-id]
     (update-terminal-leaf state-tree next-id 1))
    ([state-tree next-id iter-count]
     (update
      state-tree :children
      (fn [cur-children]
        (if (empty? cur-children)
          (conj cur-children (->StateTree next-id (get-in @GlobalConfig [:settings]) []))
          (update
           cur-children
           (dec (count cur-children))
           update-terminal-leaf next-id (inc iter-count)))))))
  
  (let [cur-state-length (-> @all-settings-state-history
                             (state-history-to-graph-state)
                             (:nodes)
                             (count))
        next-id (inc cur-state-length)]
    (js/console.log cur-state-length)
    (swap! all-settings-state-history update-terminal-leaf next-id)
    (reset! graph-state-atom
            (state-history-to-graph-state
             @all-settings-state-history))))

(def demo-digraph
  (aget (.readMany dot "digraph { Ax -> B -> C; B -> D; }") 0))

(defn digraph-to-dagre-graph
  [digraph]
  {:nodes (->> (.nodes digraph)
               (array-seq)
               (map (fn [dot-node-name]
                      {:id dot-node-name
                       :label dot-node-name})))
   :links (->> (.edges digraph)
               (array-seq)
               (map (fn [dot-edge]
                      {:source (aget dot-edge "v")
                       :target (aget dot-edge "w")})))})

(defn get-global-setting-coord-value [coord]
  (get-in @GlobalConfig [:settings coord :value]))

(defn odr-insert-coord-setting-value [session coord]
  (apply odr/insert session (conj coord (get-global-setting-coord-value coord))))

(defn render-coordinate-tag [coordinate]
  [:code
   {:style {:font-size "small"
            :color "black"
            :padding "2px"
            :word-spacing "1em"
            :border-radius "4px"
            :border (str "2px solid " (to-hex-color-dark coordinate))
            :background (to-hex-color-light coordinate)}}
   (->> coordinate
        (map name)
        (interpose " ")
        (apply str))])

(defn component [world]
  (def *session
    (atom
     (->> (get-in @GlobalConfig [:custom-rules])
          (map (fn [[rule-key rule-def]]
                 (odr/->Rule rule-key
                             (get-in rule-def [:odr.Conditions])
                             (get-in rule-def [:handler])
                             (get-in rule-def [:filter]))))
          (reduce odr/add-rule
                  (odr/->session)))))
  
  (def SettingRestriction
    {:MinMaxRange (fn [min max rcursor]
                    {:range {:min min :max max}
                     :component [(fn []
                                   [text-field
                                    {:label "some numberal "
                                     :type "number"
                                     :value (get-in @rcursor [:value])
                                     :on-change (fn [evt]
                                                  (let [cur-val @rcursor]
                                                    (if-let [new-val (try
                                                                       (-> (aget evt "target" "value")
                                                                           (js/parseInt))
                                                                       (catch js/Error e
                                                                         (js/console.log "NEED INT!")
                                                                         nil))]
                                                      (swap! rcursor assoc :value new-val)
                                                      ;; probably redundant
                                                      (swap! rcursor assoc :value cur-val))))}])]
                     :validator (fn [x]
                                  (and (min <= x)
                                       (x <= max)))})

     :Boolean (fn [rcursor]
                {:component [(fn []
                               [switch
                                {:checked (get-in @rcursor [:value])
                                 :on-change (fn [evt]
                                              (->> (aget evt "target" "checked")
                                                   (swap! rcursor assoc :value))
                                              ;; (push-settings-to-history!)

                                              (swap!
                                               *session
                                               (fn [session]
                                                 (-> session
                                                     (odr/insert ::time ::total (.getTime (js/Date.)))
                                                     
                                                     (odr-insert-coord-setting-value [::ui-style ::black-header])
                                                     (odr-insert-coord-setting-value [::ui-style ::dark-background])
                                                     
                                                     (odr/fire-rules))))

                                              )}])]
                 :validator #{true false}})})

  (let [{:keys [-state -send]} @world
        {:keys [items future past]}
        (js->clj (aget -state "context") :keywordize-keys true)]
    
    [:div
     {:style {:background (if (get-global-setting-coord-value [::ui-style ::dark-background])
                            "#222"
                            "#fff")}}
     
     [:h1 (str "latest event "
               (:t @world))]
     
     [:div
      (->> ["blue" "red"]
           (map (fn [color]
                  ^{:key ["button" color]}
                  (let [limit (get-global-setting-coord-value [::demo-panel ::list-limit])]
                    [:button
                     {:style { ;; :background color
                              :color color}
                      :on-click (fn []
                                  (-send :ADD_SHAPE {:shape color}))
                      :disabled (>= (count items)
                                    limit)}
                     (str color " " (count items) "/" limit)]))))
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

     
       [:style
        (garden/css
         [:.dagre-d3-react
          [:.nodes
           {:fill "#FFC"
            :stroke-width "2px"
            :stroke "#555"}
           [:text
            {:fill "#333"
             :stroke-width 0
             :font-family "monospace"}]]
          [:path
           {:stroke-width "2px"
            :stroke "#555"
            :fill "#555"}]])]
       
       [:> DagreGraph
        (merge
         ;; (digraph-to-dagre-graph demo-digraph)
         @graph-state-atom
         {:width "100%"
          :height "100%"
          :zoomable true
          :config {}
          :shape "circle"
          :fitBoundaries true
          :onNodeClick (fn [evt]
                         (js/console.log evt)
                         (swap!
                          *session
                          (fn [session]
                            (-> session
                                (odr/insert ::time ::total (.getTime (js/Date.)))
                                (odr/fire-rules)))))})]
       ]
      
      [:div
       {:style {:border "2px solid orange"
                :display "flex"}}
       [:div
        [button
         {:variant  "contained"
          :color    "primary"
          :on-click (fn [evt]
                      (swap!
                       *session
                       (fn [session]
                         (-> session
                             (odr/insert ::alert ::message (str "FOO THE BARF "
                                                                (js/Date.)))
                             (odr/fire-rules)))))
          }
         "hellz yeah"]]
       
       ]
      
      [:div
       {:style {:border "2px solid green"
                :display "flex"}}
       [:h4
        (count items)]
      
       [:ol
        (->> items
             (map-indexed
              (fn [i item]
                ^{:key [i item]}
                [:li
                 [button
                  {:style {:background (str "dark" item)
                           :color "white"
                           :font-weight "bold"}
                   :on-click (fn [] (-send :DELETE_SHAPE {:index i}))}
                  item]
                 ])))]]

      [:div
       {:style {:border "2px solid magenta"
                :display "flex"}}

       [:style
        (garden/css
         [:table
          {:border "1px solid gray"
           :border-collapse "collapse"}

          [:th
           {:border "1px solid #333"
            :background "#EEE"
            :color (if (get-global-setting-coord-value [::ui-style ::black-header])
                     "black"
                     "#AAA")}]

          [:td
           {:border "1px solid #CCC"}]])]
       
       [grid
        {:container true
         :direction "column"
         :spacing 1}
        
        [grid
         {:item true}
         (when-let [messages (:messages @world)]
           (->> messages
                (map (fn [message]
                       ^{:key [:message message]}
                       [:h3 message]))))]
        
        [grid
         {:item true}
         [text-field
          {:style {:width "100%"}
           :on-change (fn [evt]
                        (let [value (aget evt "target" "value")]
                          (swap! world assoc :settings-filter-string value)))}]]

        [grid
         {:item true}
         
         (let [settings-filter-string (:settings-filter-string @world)
               settings-filter (if (empty? settings-filter-string)
                                 identity
                                 (fn [setting]
                                   (or (some
                                        (fn [k]
                                          (clojure.string/includes?
                                           (name k)
                                           settings-filter-string))
                                        (:coordinate setting))
                                       (clojure.string/includes?
                                        (:description setting)
                                        settings-filter-string))))
               records (->> (get-in @GlobalConfig [:settings])
                            (vals)
                            (filter settings-filter))]
           (when-let [header-keys (some->> records
                                           (first)
                                           (keys)
                                           (remove #{:restriction}))]
             [:table
              {:style {:width "100%"
                       :display "flex"}}
              [:tbody
               [:tr
                [:th "number"]
                (->> header-keys
                     (map name)
                     (map-indexed
                      (fn [i key]
                        ^{:key [i key]}
                        [:th (name key)])))]
               (->> records
                    (map-indexed
                     (fn [i setting]
                       ^{:key [i setting]}
                       [:tr
                        [:td
                         (inc i)]
                        (->> header-keys
                             (map
                              (fn [key]
                                ^{:key [i key setting]}
                                [:td
                                 (let [val (key setting)]
                                   (case key

                                     :coordinate
                                     (render-coordinate-tag val)
                                   
                                     :value
                                     (if-let [[restriction-key args]
                                              (get-in setting [:restriction])]
                                       (some-> (get-in SettingRestriction [restriction-key])
                                               (apply (conj (vec args)
                                                            (r/cursor GlobalConfig
                                                                      [:settings (:coordinate setting)])))
                                               (:component))

                                       (str val))

                                     (str val)))
                                 ])))])))]]))]
        
        [grid
         {:item true}
         [:table
          {:style {:border "4px dashed green"
                   :border-collapse "collapse"
                   :width "calc(100% - 12px)"}}

          [:tbody
           [:tr
            [:th "#"]
            [:th "key"]
            [:th "description"]
            [:th "coordinates"]]

           (->> (:custom-rules @GlobalConfig)
                (map-indexed
                 (fn [i [key rule-row]]
                   ^{:key [i rule-row]}
                   [:tr
                    [:td
                     {:style {:width "4em"}}
                     (inc i)]
                    [:td
                     [:code (name key)]]
                    [:td
                     {:style {:width "20em"
                              :font-size "x-small"}}
                     (:description rule-row)]
                    [:td
                     {:style {:font-family "monospace"
                              :font-size "x-small"}}
                     (->> rule-row
                          (:odr.Conditions)
                          (map (fn [odr-cond]
                                 (->> (AlphaNodes->Coord (:nodes odr-cond))
                                      (map name)
                                      (vec))))
                          (map-indexed
                           (fn [j coord]
                             ^{:key [i j coord]}
                             [:div
                              {:style {:line-height "2.5em"}}
                              (render-coordinate-tag coord)])))]])))
           ]]]

        [grid
         {:item true}
         [textarea-autosize
          {:rows-min 20
           :style {:width "100%"
                   :height "100%"}
           :value (-> (->> (get-in @GlobalConfig [:settings])
                           (map (fn [[k setting]]
                                  [k (dissoc setting :restriction)]))
                           (into {})
                           (clj->js))
                      (js/JSON.stringify nil 2))}
          ]]

        [:h2 "graph state"]
        [grid
         {:item true}
         [textarea-autosize
          {:rows-min 9
           :style {:width "100%"
                   :height "4em"}
           :value (-> @all-settings-state-history
                      (clj->js)
                      (js/JSON.stringify nil 2))}
          ]]
        [:button
         {:on-click (fn []
                      (-> @all-settings-state-history
                          (state-history-to-graph-state)
                          (clj->js)
                          (js/console.log)))}
         "graph state?"]
        [grid
         {:item true}
         [textarea-autosize
          {:rows-min 9
           :style {:width "100%"
                   :height "4em"
                   :border-left "4px dashed red"}
           :value (-> (odr/map->AlphaNode {})
                      (pr-str))
           
           }
          ]]]]]]))

(defn init! []
  (let [main-component-key
        (-> (eld/get :project-config)
            (get-in [:shadow-cljs :builds])
            (keys)
            (first))

        ]
    
    (swap! $world assoc
           :-state (aget editorMachine "initialState")
           :-send (fn send [action-type & [payload]]
                    (let [cur-state (:-state @$world)]
                      (swap! $world assoc
                             :t (str (js/Date.))
                             :-state (.transition
                                      editorMachine
                                      cur-state
                                      (clj->js (merge {:type action-type}
                                                      payload)))))))
    
    (rdom/render
     [component $world]
     (gdom/getElement
      (name main-component-key)))))

(init!)
