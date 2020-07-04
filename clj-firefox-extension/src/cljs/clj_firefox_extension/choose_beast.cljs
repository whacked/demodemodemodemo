(ns clj-firefox-extension.choose-beast)

(def hide-page "
body > :not(.beastify-image) {
    display: none;
}")

(def $browser js/browser)

(defn ext-get-url [url]
  (-> (aget $browser "extension")
      (js-invoke "getURL" url)))

(defn listen-for-clicks []
  (-> js/document
      (js-invoke "addEventListener"
                 "click"
                 (fn [e]
                   (defn beast-name-to-url [beast-name]
                     (case beast-name
                       "Frog" (ext-get-url "beasts/frog.jpg")
                       "Snake" (ext-get-url "beasts/snake.jpg")
                       "Turtle" (ext-get-url "beasts/turtle.jpg")))

                   ;; insert page-hiding css into active tab;
                   ;; then send "beastify" message
                   (defn beastify [tabs]
                     (-> (aget $browser "tabs")
                         (js-invoke "insertCSS"
                                    (clj->js {:code hide-page}))
                         (js-invoke "then"
                                    (fn []
                                      (let [url (beast-name-to-url
                                                 (aget e "target" "textContent"))]
                                        (-> (aget $browser "tabs")
                                            (js-invoke
                                             "sendMessage"
                                             (aget tabs 0 "id")
                                             (clj->js {:command "beastify"
                                                       :beastURL url}))))))))

                   ;; remove page-hiding css from active tab;
                   ;; send "reset" message
                   (defn reset [tabs]
                     (-> (aget $browser "tabs")
                         (js-invoke "removeCSS" (clj->js {:code hide-page}))
                         (js-invoke "then"
                                    (fn []
                                      (-> (aget $browser "tabs")
                                          (js-invoke
                                           "sendMessage"
                                           (aget tabs 0 "id")
                                           (clj->js {:command "reset"})))))))
                   
                   (defn report-error [error]
                     (js/console.error (str "could not beastify: " error)))
                   
                   (let [class-list (aget e "target" "classList")
                         tabs (aget $browser "tabs")]
                     (cond (js-invoke class-list "contains" "beast")
                           (-> tabs
                               (js-invoke "query"
                                          (clj->js {:active true
                                                    :currentWindow true}))
                               (js-invoke "then" beastify)
                               (js-invoke "catch" report-error))

                           (js-invoke class-list "contains" "reset")
                           (-> tabs
                               (js-invoke "query" (clj->js {:active true
                                                            :currentWindow true}))
                               (js-invoke "then" reset)
                               (js-invoke "catch" report-error))))))))

(defn report-execute-script-error [error]
  (-> (aget (-> js/document
                (js-invoke "querySelector" "#popup-content")) "classList")
      (js-invoke "add" "hidden"))
  (-> (aget (-> js/document
                (js-invoke "querySelector" "#error-content")) "classList")
      (js-invoke "remove" "hidden"))
  (js/console.error (str "Failed to execute beastify content script: " (aget error "message"))))

(-> (aget $browser "tabs")
    (js-invoke "executeScript"
               (clj->js {:file "/content_scripts/beastify.js"}))
    (js-invoke "then"
               listen-for-clicks)
    (js-invoke "catch"
               report-execute-script-error))
