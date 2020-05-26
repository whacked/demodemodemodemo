(ns clj-etaoin-screen-capture.core
  (:require [etaoin.api :as api]
            [etaoin.xpath :as xpath]
            [etaoin.keys :as k]
            [clojure.string])
  (:import [java.awt Graphics Canvas Color]
           [java.awt GraphicsDevice GraphicsEnvironment]
           [javax.swing JFrame]))

(comment

  ;; capability check
  (let [gd (-> (GraphicsEnvironment/getLocalGraphicsEnvironment)
               (.getDefaultScreenDevice))]

    (-> gd
        (.isWindowTranslucencySupported
         java.awt.GraphicsDevice$WindowTranslucency/TRANSLUCENT)
        (println))

    (-> gd
        (.isWindowTranslucencySupported
         java.awt.GraphicsDevice$WindowTranslucency/PERPIXEL_TRANSLUCENT)
        (println))

    (-> gd
        (.isWindowTranslucencySupported
         java.awt.GraphicsDevice$WindowTranslucency/PERPIXEL_TRANSPARENT)
        (println))))

(defn get-window-decoration-size []
  (let [test-width 200
        test-height 200
        test-frame (javax.swing.JFrame. "test window")]
  
    (doto (.getContentPane test-frame)
      (.setLayout (java.awt.BorderLayout.))
      (.add (doto (javax.swing.JPanel.)
              (.setPreferredSize (java.awt.Dimension. 200 200)))))
  
    (.pack test-frame)

    (let [bounds (.getBounds test-frame)
          border-size (-> (.getWidth bounds)
                          (- test-width)
                          (/ 2))
          
          output
          {:border-size border-size
           :title-bar-size (-> (.getHeight bounds)
                               (- (* 2 border-size))
                               (- test-height))}]
      (.dispose test-frame)
      
      output)))

(defn make-overlay-box! [x y w h]
  (let [frame (javax.swing.JFrame. "my window")
        color (java.awt.Color. 0 true)
        border-size 4
        coord [x y]
        width w  ;; 200
        height h ;; 150
        ]
    (doto frame
      (.setUndecorated true)
      (.setBackground color)
      (.setBounds (java.awt.Rectangle. 0 0 width height))
      (.setAlwaysOnTop true))
    ;; Without this, the window is draggable from any non transparent
    ;; point, including points  inside textboxes.
    ;; (.. frame (getRootPane) (putClientProperty "apple.awt.draggableWindowBackground" false))
    (.. frame
        (getRootPane)
        (setBorder (javax.swing.BorderFactory/createMatteBorder
                    border-size border-size border-size border-size
                    java.awt.Color/RED)))
    (doto (.getContentPane frame)
      (.setLayout (java.awt.BorderLayout.))
      (.add (let [panel (javax.swing.JPanel.)]
              (doto panel
                (.setOpaque false)
                (.setPreferredSize (java.awt.Dimension. width height))
                ))))
    (doto frame
      (.setVisible true)
      ;; center on x,y
      ;; (.setLocation (- (first coord) (/ width 2) border-size)
      ;;               (- (second coord) (/ height 2) border-size))
      ;; left-top on x,y
      (.setLocation (- (first coord) border-size)
                    (- (second coord) border-size))
      (.pack))))

(def $get-xpath-js
  ;; https://stackoverflow.com/a/32623171
  (-> "
function xpath(el) {
  if (typeof el == \"string\") return document.evaluate(el, document, null, 0, null);
  if (!el || el.nodeType != 1) return '';
  if (el.id) return \"//*[@id='\" + el.id + \"']\";
  var sames = [].filter.call(el.parentNode.children, function (x) { return x.tagName == el.tagName });
  return xpath(el.parentNode) + '/' + el.tagName.toLowerCase() + (sames.length > 1 ? '['+([].indexOf.call(sames, el)+1)+']' : '');
}
"
      (clojure.string/replace "\n" "")))

(def $window-decoration-size (get-window-decoration-size))

(defn highlight-element [driver xpath]
  (let [window-pos (api/get-window-position driver)
        window-scroll (api/get-scroll driver)
        screen-size (-> (select-keys
                         (api/js-execute
                          driver
                          "return screen")
                         [:width :height])
                        (assoc
                         :offsetWidth
                         (api/js-execute
                          driver
                          "return document.querySelector(\"html\").offsetWidth")
                         :innerHeight
                         (api/js-execute driver "return window.innerHeight")))

        window-size (api/get-window-size driver)
        element-box (api/get-element-box driver {:xpath xpath})

        element-y-offset (- (:y1 element-box)
                            (:y window-scroll))
        top-chrome-height (-> (:height window-size)
                              (- (:innerHeight screen-size)))]
    
    (if-not (< -5 element-y-offset)
      (println "cannot get y offset: " element-y-offset)
      (let [mybox
            (make-overlay-box!
             (+ (:x window-pos)
                (:border-size $window-decoration-size)
                (:x1 element-box))
             (+ (:y window-pos)
                top-chrome-height
                (:title-bar-size $window-decoration-size)
                (:border-size $window-decoration-size)
                element-y-offset)
             (:width element-box)
             (:height element-box))]

        
        (.setVisible mybox true)
        (Thread/sleep 1000)
        (.dispose mybox)))))

(defn -main []
  (defonce driver (api/firefox))

  (api/go driver "https://github.com/igrishaev/etaoin")
  (api/wait-visible driver :user-content-documentation)
  (api/scroll-query driver :user-content-documentation)
  #_(api/query driver {:tag :a
                       :href "http://grishaev.me/etaoin/"})
  (Thread/sleep 500)
  
  (let [envvar-dim (->> [:x :y :width :height]
                        (map (fn [key]
                               [key
                                (->> (name key)
                                     (clojure.string/upper-case)
                                     (str "DEMO_WINDOW_")
                                     (System/getenv)
                                     (Integer/parseInt))]))
                        (into {}))]
    (api/set-window-position
     driver (:x envvar-dim) (:y envvar-dim))
    (api/set-window-size
     driver (:width envvar-dim) (:height envvar-dim)))
  (Thread/sleep 200)

  (when-let [installation-h2-uuid
             (some->> (api/query-all
                       driver
                       {:tag :h2})
                      (filter (fn [element-uuid]
                                (-> (api/get-element-text-el
                                     driver element-uuid)
                                    (= "Installation"))))
                      (first))]
    (let [element-xpath
          (-> (api/js-execute
               driver
               (str $get-xpath-js ";return xpath(arguments[0])")
               (api/el->ref installation-h2-uuid)))]
      ;; "//*[@id='readme']/div[3]/article/h2[5]"
      (highlight-element driver element-xpath)))
  
  (api/close-window driver))
