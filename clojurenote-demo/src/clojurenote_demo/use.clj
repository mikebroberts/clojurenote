(ns clojurenote-demo.use
  (:require 
    [clojurenote.notes :as notes]
    ))

(defn list-notebooks [token notestore-url]
  (let [notebooks (notes/list-notebooks {:access-token token :notestore-url notestore-url})]
    (str "<html><body>
          <p>Notebooks for token: " token ", notestore: " notestore-url "</p>"
          "<ul>"
          (apply str (map (comp #(str "<li>" % "</li>") bean) notebooks))
          "</ul>"
          "</body></html>")))
