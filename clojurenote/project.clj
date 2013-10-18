(defproject clojurenote "0.4.0"
  :description "Clojure library to access Evernote API"
  :url "https://github.com/mikebroberts/clojurenote"
  :license {:name "The MIT License"
            :url "https://raw.github.com/mikebroberts/clojurenote/master/LICENSE"}
  :dependencies [
    [org.clojure/clojure "1.4.0"]
    [org.scribe/scribe "1.3.5"]
    [com.evernote/evernote-api "1.25"]
  ]
  :profiles {
    :dev {
      :dependencies [
        [expectations "1.4.52"]
        [erajure "0.0.3"]
      ]
    }
  }
  :plugins [
    [lein-expectations "0.0.7"]
  ])
