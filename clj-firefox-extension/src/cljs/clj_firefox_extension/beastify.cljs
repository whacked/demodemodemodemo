(ns clj-firefox-extension.core)

(when-not (aget js/window "hasRun")

  (aset js/window "hasRun" true)

  (defn remove-existing-beasts []
    (let [existing-beasts (.querySelectorAll
                           js/document
                           ".beastify-image")]
      (doseq [beast (array-seq existing-beasts)]
        (.remove beast))))

  (defn insert-beast [beast-url]
    (remove-existing-beasts)
    (.appendChild
     (aget js/document "body")
     (doto (.createElement js/document "img")
       (.setAttribute "src" beast-url)
       (aset "style" "height" "100vh")
       (aset "className" "beastify-image"))))

  (-> (aget js/browser "runtime" "onMessage")
      (.addListener
       (fn [message]
         (case (aget message "command")
           "beastify"
           (insert-beast (aget message "beastURL"))
           "reset"
           (remove-existing-beasts))))))
