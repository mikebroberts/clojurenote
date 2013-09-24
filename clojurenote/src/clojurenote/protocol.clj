(ns clojurenote.protocol
  (:import
    (com.evernote.thrift.transport THttpClient)
    (com.evernote.thrift.protocol TBinaryProtocol) 
    ))

(def userAgent "Evernote/EDAMDemo (Java) 1.25")

(defn protocol-for [url]
  (->> 
    (doto (THttpClient. url)
      (.setCustomHeader "User-Agent" userAgent))
    (TBinaryProtocol.)))
