(defproject clojurenote-demo "0.1.0"
  :description "Demonstration app to show usage of clojurenote"
  :url "https://github.com/mikebroberts/clojurenote"
  :dependencies [
    [org.clojure/clojure "1.4.0"]
    [compojure "1.1.5"]
    [environ "0.4.0"]
    [clojurenote "0.2.0"]
  ]
  :plugins [
    [lein-ring "0.8.2"]
    [lein-environ "0.4.0"]
    ]
  :ring {:handler clojurenote-demo.handler/app}
  :profiles
  {:dev {:dependencies [[ring-mock "0.1.5"]]}})
