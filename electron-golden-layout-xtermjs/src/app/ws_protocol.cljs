(ns app.ws-protocol)

(defn encode [[op body]]
  (->> [(name op) body]
       (clj->js)
       (.stringify js/JSON)))

(defn decode [json-string]
  (let [[op-string payload]
        (-> (.parse js/JSON json-string)
            (js->clj :keywordize-keys true))]
    [(keyword op-string) payload]))
