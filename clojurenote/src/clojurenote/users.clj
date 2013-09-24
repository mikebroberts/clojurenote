(ns clojurenote.users
  (:import (com.evernote.edam.userstore UserStore Constants))
  (:use [clojurenote.protocol :only [protocol-for]]))

(def services {
  :production "https://www.evernote.com"
  :sandbox "https://sandbox.evernote.com"
  :yinxiang "https://app.yinxiang.com"
  })

(defn user-store-url [service]
  (if-let [url-base (service services)]
    (str url-base "/edam/user")
    (throw (Exception. (str "Invalid service key specified: " service)))))

(defn create-user-store [service]
  (let [
    protocol (protocol-for (user-store-url service))
    store (com.evernote.edam.userstore.UserStore$Client. protocol protocol)]
    (when-not (.checkVersion store "Evernote EDAMDemo (Java)" 
                Constants/EDAM_VERSION_MAJOR
                Constants/EDAM_VERSION_MINOR)
        (throw (Exception. "Bad Evernote client protocol version")))
    store))

(defn get-user-details [service access-token]
  (-> (create-user-store service) (.getUser access-token)))

(defn get-notestore-url [service access-token]
  (-> (create-user-store service) (.getNoteStoreUrl access-token)))

(defn revoke-session [service access-token]
  (-> (create-user-store service) (.revokeLongSession access-token)))
