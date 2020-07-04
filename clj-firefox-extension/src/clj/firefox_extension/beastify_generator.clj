(ns firefox-extension.beastify-generator
  (:require [hiccup.core :as hc]
            [hiccup.page :as hp]
            [clojure.java.io :as io]
            [clojure.data.xml :as xml]
            [firefox-extension.common
             :refer [$extensions-base-dir]]
            [garden.core :as gc]))

(def $extension-name "beastify")

(def $beastify-manifest
  (-> (slurp (io/file
              $extensions-base-dir
              $extension-name
              "base-manifest-json.edn"))
      (read-string)))

(defn generate-html []
  (->> (hc/html
        [:html
         [:head
          [:meta {:charset "utf-8"}]
          [:link {:rel "stylesheet"
                  :href "choose_beast.css"}]]
         [:body
          [:div {:id "popup-content"}
           (->> ["Frog" "Turtle" "Snake"]
                (map (fn [beast]
                       [:div.button.beast beast])))
           [:div.button.reset
            "Reset"]]
          [:div
           {:id "error-content"
            :class "hidden"}
           [:p
            "Can't beastify this web page."]
           [:p
            "Try a different page."]]
          [:script
           {:src "choose_beast.js"}]]])
       (java.io.StringReader.)
       (xml/parse)
       (xml/indent-str)
       (str (:html5 hp/doctype))))

(defn generate-css []
  (gc/css
   [:html :body
    {:width "100px"}]

   [:.hidden
    {:display "none"}]

   [:.button
    {:margin "3% auto"
     :padding "4px"
     :text-align "center"
     :font-size "1.5em"
     :cursor "pointer"}]

   [:.beast:hover
    {:background-color "#CFF2F2"}]

   [:.beast
    {:background-color "#E5F2F2"}]

   [:.reset
    {:background-color "#FBFBC9"}]

   [:.reset:hover
    {:background-color "#EAEA9D"}]))

(defn -main [& _]

  (let [html (generate-html)]
    (println html)
    (spit (io/file
           $extensions-base-dir
           $extension-name
           (get-in $beastify-manifest [:browser_action :default_popup]))
          html))

  (let [css (generate-css)]
    (println css)
    (spit (io/file
           $extensions-base-dir
           $extension-name
           "popup"
           "choose_beast.css")
          css)))


