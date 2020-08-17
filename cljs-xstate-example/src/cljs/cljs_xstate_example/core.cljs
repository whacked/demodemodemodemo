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
   ["muuri-react" :refer [MuuriComponent]]
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
   [reagent-material-ui.core.checkbox :refer [checkbox]]
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
  (let [app-config-map
        {:app-config
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
              (into {}))}
        
        custom-rules-map
        {:custom-rules
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
              
              (into {}))}

        user-input-config-map
        {:action-learning-strategy
         {[::ui-adaptive-mode ::optimize-for-speed]
          {:checked? false
           :description "optimize for speed (fitts's law: frequent use gains prominence)"}
          
          [::ui-adaptive-mode ::optimize-for-retention]
          {:checked? false
           :description "optimize for retention (infrequent use gains prominence)"}
          
          [::ui-adaptive-mode ::optimize-for-variety]
          {:checked? false
           :description "optimize for variety (LTM engagement: unused gain prominence)"}

          [::ui-adaptive-mode ::stm-buffer-size]
          {:number-of-items 4
           :description "user acting STM buffer size"}
          }

         :action-buttons (->> [
                               "action1"
                               "action2"
                               "action3"
                               "action4"
                               "action5"
                               "action6"
                               "action7"
                               "action8"
                               ]
                              (map (fn [action-name]
                                     {:id action-name
                                      :checked? false})))
         :action-history []}]
    
    (r/atom
     (merge
      app-config-map
      custom-rules-map
      user-input-config-map))))

(s/defrecord StateTree
    [id       :- s/Int
     state    :- s/Any
     children :- []])

(defn get-node-in-level
  ;; nil means no limit
  ([state-tree]
   (get-node-in-level state-tree nil))
  ([state-tree level-limit]
   (if (int? level-limit)
     (if (= 0 level-limit)
       state-tree
       (-> (:children state-tree)
           (last)
           (get-node-in-level (dec level-limit))))
     (let [children (:children state-tree)]
       (if (empty? children)
         state-tree
         (get-node-in-level (last children) nil))))))

(defn state-history-to-graph-state
  ([stree]
   (state-history-to-graph-state stree {:nodes [] :links []}))
  ([stree out]
   (if-not (:id stree)
     out
     (let [terminal-node (get-node-in-level stree)
           
           to-node (fn [t]
                     (merge
                      (select-keys t [:id])
                      {:label (:id t)}
                      (when (= (:id t)
                               (:id terminal-node))
                        {:config {:style "fill: red"}})))
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
           1 (get-in @GlobalConfig [:app-config]) [])))

(def graph-state-atom
  (r/atom
   (state-history-to-graph-state @all-settings-state-history)))

(defn get-global-setting-coord-value [coord]
  (get-in @GlobalConfig [:app-config coord :value]))

(defn push-settings-to-history! []

  (defn update-terminal-leaf
    ([state-tree next-id]
     (update-terminal-leaf state-tree next-id 1))
    ([state-tree next-id iter-count]
     (update
      state-tree :children
      (fn [cur-children]
        (if (empty? cur-children)
          (conj cur-children (->StateTree next-id (get-in @GlobalConfig [:app-config]) []))
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
  (aget (.readMany dot
                   "digraph {
ROOT -> tutorial1;

tutorial1 -> skill1;
tutorial1 -> skill2;
tutorial1 -> skill3;
tutorial1 -> skill4;

skill4 [label=\"BAR\",THING=1];
skill1 [label=ONE];

}") 0))

(defn to-dot-style [style-map]
  (->> style-map
       (map (fn [[style-key style-value]]
              (str (name style-key) ": " style-value ";")))
       (apply str)))

(defn digraph-to-dagre-graph
  [digraph]
  {:nodes (->> (.nodes digraph)
               (array-seq)
               (map (fn [dot-node-id]
                      (let [node (.node digraph dot-node-id)]
                       {:id dot-node-id
                        :label (or (aget node "label")
                                   dot-node-id)}))))
   :links (->> (.edges digraph)
               (array-seq)
               (map (fn [dot-edge]
                      {:source (aget dot-edge "v")
                       :target (aget dot-edge "w")})))})

(defn digraph-to-skill-tree-dagre-graph
  [sk-digraph]
  (merge
   {:nodes (->> (.nodes sk-digraph)
                (array-seq)
                (map (fn [dot-node-id]
                       (let [node (.node sk-digraph dot-node-id)]
                         {:id dot-node-id
                          :config {:style "fill: lightblue"}
                          :label (or (aget node "label")
                                     dot-node-id)}))))
    :links (->> (.edges sk-digraph)
                (array-seq)
                (map (fn [dot-edge]
                       {:source (aget dot-edge "v")
                        :target (aget dot-edge "w")})))}
   {:width "100%"
    :height "100%"
    :zoomable true
    :shape "rect"
    :fitBoundaries true}))

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
                                              (push-settings-to-history!)

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
                  (let [limit (get-global-setting-coord-value [::demo-panel ::list-limit])]
                    ^{:key ["button" color]}
                    [:button
                     {:style { ;; :background color
                              :color color}
                      :on-click (fn []
                                  (-send :ADD_SHAPE {:shape color}))
                      :disabled (>= (count items)
                                    limit)}
                     (str color " " (count items) "/" limit)])))
           (doall))
      [:button
       {:on-click (fn []
                    (-send :UNDO))
        :disabled (empty? past)}
       "undo"]
      [:button
       {:on-click (fn []
                    (-send :REDO))
        :disabled (empty? future)}
       "redo"]

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
            :fill "#555"}]
          [:.edgeLabels
           {:font-family "monospace"}]])]
       
       [:> DagreGraph
        (merge
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
               records (->> (get-in @GlobalConfig [:app-config])
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
                                                                      [:app-config (:coordinate setting)])))
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

        (let [history (get-in @GlobalConfig [:action-history])
              history-by-action (group-by :id history)
              stm-buffer-size-cursor-path [:action-learning-strategy
                                           [::ui-adaptive-mode ::stm-buffer-size]
                                           :number-of-items]
              stm-buffer-size (get-in @GlobalConfig [:action-learning-strategy
                                                     [::ui-adaptive-mode ::stm-buffer-size]
                                                     :number-of-items])
              active-action-buttons (->> (get-in @GlobalConfig [:action-buttons])
                                         (take (or stm-buffer-size
                                                   99999)))
              now (-> (js/Date.)
                      (.getTime))]
          [grid
           {:item true}
           [:div
            {:style {:display "flex"}}
            [:div
             "strats"
             {:style {:display "flex"}}
             [:ol
              {:style {:padding-left "2em"}}
              [:li
               [text-field
                {:label "number of items"
                 :type "number"
                 :value (get-in @GlobalConfig stm-buffer-size-cursor-path)
                 :on-change (fn [evt]
                              (let [value (-> (aget evt "target" "value")
                                              (js/parseInt))]
                                (cond (= 0 value)
                                      (swap! GlobalConfig assoc-in stm-buffer-size-cursor-path nil)

                                      (< 0 value)
                                      (swap! GlobalConfig assoc-in stm-buffer-size-cursor-path value)

                                      :else
                                      nil)))}]
               "STM buffer size"]
              (->> (get-in @GlobalConfig
                           [:action-learning-strategy])
                   (filter
                    (fn [[_ props]]
                      (not (nil? (:checked? props)))))
                   (map-indexed
                    (fn [i [strategy-coordinate props]]
                      ^{:key [strategy-coordinate]}
                      [:li
                       [:label
                        [checkbox
                         {:checked (:checked? props)
                          :on-change
                          (fn [evt]
                            (swap! GlobalConfig
                                   assoc-in
                                   [:action-learning-strategy
                                    strategy-coordinate
                                    :checked?]
                                   (aget evt "target" "checked")))}]
                        (:description props)]])))]]
            [:div
             {:style {:display "flex"
                      :font-family "monospace"}}
             (let [calculate-stats
                   (fn [action]
                     (let [action-history (history-by-action (:id action))
                           latest-time (-> action-history
                                           (last)
                                           (:time))]
                       {:total (count action-history)
                        :earliest (-> action-history
                                      (first)
                                      (:time))
                        :latest latest-time
                        :familiarity-score (if latest-time
                                             (cond (-> (- now latest-time)
                                                       (/ 1000)
                                                       (< 120) ;; time after which it's considered out of TOM
                                                       )
                                                   1

                                                   )
                                             nil)}))

                   action-stats (->> (get-in @GlobalConfig [:action-buttons])
                                     (map (fn [action]
                                            [(:id action)
                                             (calculate-stats action)]))
                                     (into {}))]
               [:div
                [:ol
                 (->> (get-in @GlobalConfig [:action-buttons])
                      (map-indexed
                       (fn [i action]
                         ^{:key [:stat i action]}
                         [:li
                          [:b (:id action)]
                          [:ul
                           (->> (action-stats (:id action))
                                (map (fn [[k v]]
                                       ^{:key [:stat k v]}
                                       [:li (name k) ": " (str v)])))]])))]

                (when (< 0 (count active-action-buttons))
                  (let [num-buttons-in-use
                        (->> active-action-buttons
                             (map :id)
                             (map action-stats)
                             (map :total)
                             (remove (partial = 0))
                             (count))
                       
                        average-active-familiarity
                        (if (< 0 num-buttons-in-use)
                          (->> active-action-buttons
                               (map :id)
                               (map action-stats)
                               (map :familiarity-score)
                               (remove nil?)
                               (apply +)
                               (* (/ 1 num-buttons-in-use)))
                          nil)]

                    [:div
                     [:h3 "familiarity average: "]
                     average-active-familiarity
                    
                     (when (and
                            (= num-buttons-in-use (count active-action-buttons))
                            average-active-familiarity
                            (< 0.7 average-active-familiarity))
                       [:button
                        {:type "button"
                         :on-click (fn [_]
                                     (swap! GlobalConfig update-in stm-buffer-size-cursor-path inc))}
                        "increase STM size"])]))])
             ]
            ]
           [:div
            [:code "total: "
             (count history)]
            [:> MuuriComponent
             (->> active-action-buttons
                  (map-indexed
                   (fn [i action]
                     (let [action-id (:id action)
                           action-history (get history-by-action
                                               action-id)
                           click-count (count action-history)
                           base-width 5]
                       ^{:key [:button i action]}
                       [:div
                        {:class "card"
                         :on-click (fn [_]
                                     (swap! GlobalConfig
                                            update
                                            :action-history
                                            conj
                                            {:time (.getTime (js/Date.))
                                             :id action-id}))
                         :style {:border "2px solid blue"
                                 :border-radius "0.3em"
                                 :width (str base-width "em")
                                 :margin "0.3em"

                                 :font-family "monospace"

                                 :background (if (get-in @GlobalConfig [:action-learning-strategy
                                                                        [::ui-adaptive-mode ::optimize-for-variety]
                                                                        :checked?])
                                               (-> (col/rgba 1 0 0 (max 0 (- 1 (/ click-count 10))))
                                                   (col/as-css)
                                                   (:col))
                                               nil)

                                 
                                 :box-shadow (when (and (get-in @GlobalConfig [:action-learning-strategy
                                                                               [::ui-adaptive-mode ::optimize-for-retention]
                                                                               :checked?])
                                                        (< 0 click-count))
                                               (let [most-recent-click-time (-> action-history
                                                                                (last)
                                                                                (:time))

                                                     spread-size (min 20
                                                                      (-> (- now most-recent-click-time)
                                                                          (/ 1000)
                                                                          (/ 5)
                                                                          (Math/round)))]
                                                 (str
                                                  "0 0 "
                                                  (* 2 spread-size) "px "
                                                  spread-size "px green")))

                                 :height (if (get-in @GlobalConfig [:action-learning-strategy
                                                                    [::ui-adaptive-mode ::optimize-for-speed]
                                                                    :checked?])
                                           (str (* base-width (+ 1 (/ (* 5 click-count) 100))) "em")
                                           "100%")}}
                        
                        [:div
                         {:class "card-remove"
                          :on-click (fn []
                                      (js/console.clear))}
                         [:b "❌"]]
                        
                        (str action-id " (" click-count ")")])))
                  
                  (doall))]]])
        
        [grid
         {:item true
          :style {:border "1px dashed black"
                  :width "calc(100% - 2em)"
                  :height "30em"}}
         [:> DagreGraph
          #_(digraph-to-skill-tree-dagre-graph demo-digraph)
          (let [ROOT "ROOT"
                
                front-links ["link1"
                             "link2"
                             "link3"]

                fixed-pages ["admin-page"
                             "help-page"
                             "profile-page"]

                nodes (concat
                       [{:id ROOT
                         :label "ROOT"}]

                       [{:id "skills"
                         :label (->> ["skills"
                                      "condition: show 1 extra"
                                      "when average familiarity > 0.8"]
                                     (interpose "\n")
                                     (apply str))
                         :config {:style (to-dot-style
                                          {:stroke "#909"
                                           :fill "skyblue"})}
                         }
                        ]
                       
                       [{:id "front-links"
                         :label "links"}
                        ]
                       
                       (->> fixed-pages
                            (map (fn [page]
                                   (let [props {:position :fixed
                                                :disappears :never}]
                                     {:id page
                                      :label (->> props
                                                  (map (fn [[k v]]
                                                         (str (name k) ": " (name v))))
                                                  (concat
                                                   (let [header-length (+ 4 (count page))]
                                                     [(str "," (apply str (take (- header-length 2) (repeat "-"))) ".")
                                                      (str "| " page " |")
                                                      (str "`" (apply str (take (- header-length 2) (repeat "-"))) "'")]))
                                                  (interpose "\n")
                                                  (apply str))
                                      :config (if (empty? props)
                                                {}
                                                (if (= (:disappears props)
                                                       :never)
                                                  {:style 
                                                   (to-dot-style
                                                    {:stroke-width "5px"
                                                     :stroke "blue"})}))}))))
                       (->> front-links
                            (map (fn [child-node]
                                   {:id child-node
                                    :label child-node})))

                       [{:id "features"
                         :label "features"}
                        {:id "upload-video"
                         :label "upload video"}
                        {:id "check-timestamps"
                         :label "check\nevent\ntimestamps"}
                        {:id "annotate-video"
                         :label "annotate video"}])
                links (concat
                       [
                        [ROOT "front-links"]
                        ]

                       (->> fixed-pages
                            (map (fn [page]
                                   ["front-links" page])))
                       (->> front-links
                            (map (fn [child-node]
                                   ["front-links" child-node])))
                       
                       [
                        [ROOT "skills"]

                        [ROOT "features"]
                        ["features" "upload-video"
                         {:label (->> ["conditions:"
                                       "- points >= 5"]
                                      (interpose "\n")
                                      (apply str))
                          :config {:style
                                   (to-dot-style
                                    {:stroke "red"
                                     :stroke-width "5px"
                                     })}}]
                        ["upload-video" "annotate-video"
                         {:label "OR"}]
                        ["upload-video" "check-timestamps"
                         {:label "OR"}]
                        ])]
            {:width "100%"
             :height "100%"
             :nodes nodes
             :links (->> links
                         (map (fn [spec]
                                (let [[v w] (take 2 spec)
                                      options (if (= 3 (count spec))
                                                (last spec)
                                                nil)]
                                  (merge
                                   {:source v
                                    :target w}
                                   options)))))})]
         
         ]]]]]))

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
