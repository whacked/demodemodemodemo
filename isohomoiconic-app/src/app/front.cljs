(ns app.front
  (:require
   [garden.core :as garden]
   [goog.dom :as gdom]
   [reagent.dom :as rdom]
   [reagent.core :as r]))

(defonce $state (r/atom {}))

(defn main-component []
  [:div.world
   [:h1
    "shining shimmering splendid!"]])

(defn style-component []
  [:style
   {:dangerouslySetInnerHTML
    {:__html (garden/css
               [:h1
                (get-in @$state [:heading-style])])}}])

(defn render-all! []
  (rdom/render
   [:<>
    [style-component]
    [main-component]]
   (gdom/getElement "app")))

(defn reload! []
  (render-all!))

(defn rand-px [x]
  (str (- (rand-int x) (/ x 2)) "px"))

(defn init! []
  (render-all!)
  (js/setInterval
    (fn []
      (swap! $state assoc-in [:heading-style]
             {:font-style (rand-nth ["normal" "bold" "italic"])
              :text-decoration (rand-nth [nil "underline" "line-through" "underline wavy"])
              :text-shadow [(str (rand-px 22) " " (rand-px 22) " " (rand-int 22) "px #F3A61E")
                            (str (rand-px 9) " " (rand-px 9) " " (rand-int 9) "px #000")]
              }))
    1000))
