(ns clojurenote.stores
  (:import
    (com.evernote.thrift.transport THttpClient)
    (com.evernote.thrift.protocol TBinaryProtocol) 
    (com.evernote.edam.userstore UserStore Constants)
    (com.evernote.edam.notestore NoteStore)
    ))

(def userAgent "Evernote/EDAMDemo (Java) 1.25")

(defn user-store-url [for-production-evernote?]
  (str "https://" (if for-production-evernote? "www" "sandbox") ".evernote.com/edam/user"))

(defn createProt [url]
  (->> 
    (doto (THttpClient. url)
      (.setCustomHeader "User-Agent" userAgent))
    (TBinaryProtocol.)))

(defn create-user-store [for-production-evernote?]
  (let [
    userStoreProt (createProt (user-store-url for-production-evernote?))
    userStore (com.evernote.edam.userstore.UserStore$Client. userStoreProt userStoreProt)]
    (if (not (.checkVersion userStore "Evernote EDAMDemo (Java)" 
      Constants/EDAM_VERSION_MAJOR
      Constants/EDAM_VERSION_MINOR))
      (do
        (println "Incompatible Evernote client protocol version")
        (throw (Exception. "Bad Evernote client protocol version"))))
    userStore))

(defn get-user-details [for-production-evernote? access-token]
  (-> (create-user-store for-production-evernote?) (.getUser access-token) (bean)))

(defn get-notestore-url [for-production-evernote? access-token]
  (-> (create-user-store for-production-evernote?) (.getNoteStoreUrl access-token)))

(defn revoke-session [for-production-evernote? access-token]
  (-> (create-user-store for-production-evernote?) (.revokeLongSession access-token)))

(defn create-note-store [notestore-url]
  (println "Creating notestore")
  (let [prot (createProt notestore-url)]
    (com.evernote.edam.notestore.NoteStore$Client. prot prot)))

(defn create-note-store-with-developer-token [for-production-evernote? dev-token]
  "Only for use if you're using developer tokens, rather than OAuth, to authenticate.
    See http://dev.evernote.com/doc/articles/authentication.php#devtoken"
  (let [
    user-store (create-user-store for-production-evernote?)
    noteStoreProt (createProt (.getNoteStoreUrl user-store dev-token))]
    (com.evernote.edam.notestore.NoteStore$Client. noteStoreProt noteStoreProt)))

(defn note-store [{:keys [notestore notestore-url]}]
  (cond 
    notestore notestore
    notestore-url (create-note-store notestore-url)
    :else (throw (Exception. "Must specify :notestore or :notestore-url in user map"))))
