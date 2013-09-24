(ns clojurenote.notes
  (:import
    (com.evernote.edam.notestore NoteStore NoteFilter NotesMetadataResultSpec)
    (com.evernote.edam.type Note Notebook NoteSortOrder)
    )
  (:use [clojurenote.protocol :only [protocol-for]])
  (:require 
    [clojurenote.users :as users]
  ))

(defn create-note-store [notestore-url]
  (let [protocol (protocol-for notestore-url)]
    (com.evernote.edam.notestore.NoteStore$Client. protocol protocol)))

(defn create-note-store-with-developer-token [service dev-token]
  "Only use if you're using a developer token, rather than OAuth, to authenticate.
    service should be a key from the clojurenote.users/services map.
    For more info about developer keys see
    http://dev.evernote.com/doc/articles/authentication.php#devtoken"
  (-> 
    (users/create-user-store service)
    (.getNoteStoreUrl dev-token)
    (create-note-store)))

(defn note-store [{:keys [notestore notestore-url]}]
  (cond 
    notestore notestore
    notestore-url (create-note-store notestore-url)
    :else (throw (Exception. "Must specify :notestore or :notestore-url in user map"))))

(defn access-token [{access-token :access-token}]
  (when-not access-token (throw (Exception. "Must specify :access-token in user map")))
  access-token)

; ** READ FUNCTIONS **

(defn list-notebooks [user]
  (.listNotebooks (note-store user) (access-token user)))

(defn find-notebook-by-predicate [user pred]
  (->> (list-notebooks user)
    (filter pred)
    (first)))

(defn find-notebook-by-name [user notebook-name]
  (find-notebook-by-predicate user #(= notebook-name (.getName %))))

(defn find-notebook-by-guid [user notebook-guid]
  (find-notebook-by-predicate user #(= notebook-guid (.getGuid %))))

(defn basic-notes-for-notebook [user notebook-guid]
  (->
    (.findNotesMetadata 
      (note-store user)
      (access-token user)
      (doto (NoteFilter.)
        (.setNotebookGuid notebook-guid)
        (.setOrder (.getValue NoteSortOrder/CREATED))
        (.setAscending false)) 
      0 
      100 
      (NotesMetadataResultSpec.))
    (.getNotes)) 
  )

(defn get-note [user guid]
  (.getNote 
    (note-store user) 
    (access-token user)
    guid true false false false))

(defn remove-enml [c]
  (-> c
    (clojure.string/replace "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" "")
    (clojure.string/replace "<!DOCTYPE en-note SYSTEM \"http://xml.evernote.com/pub/enml2.dtd\">" "")
    (clojure.string/replace "<en-note>" "")
    (clojure.string/replace "</en-note>" "")
    (clojure.string/trim)
    ))

(defn plain-content [note]
  (-> note (.getContent) remove-enml))

(defn get-note-application-data-entry [user application-key guid]
  (-> (note-store user)
    (.getNoteApplicationDataEntry (access-token user) guid application-key)
    ))

(defn get-all-tags-for-notebook [user notebook-guid]
  (.listTagsByNotebook (note-store user) (access-token user) notebook-guid))

; ** WRITE FUNCTIONS **

(defn create-notebook [user notebook-name]
  (->> 
    (doto (Notebook.) (.setName notebook-name))
    (.createNotebook (note-store user) (access-token user))))

(def enml-header (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        "<!DOCTYPE en-note SYSTEM \"http://xml.evernote.com/pub/enml2.dtd\">"
        "<en-note>"))

(def enml-footer "</en-note>")

(defn create-enml-document [content]
  (format (str enml-header "%s" enml-footer) (if content content "")))

(defn write-note [user notebook-guid title content-document date tag-names]
  (->> 
    (doto (Note.)
      (.setTitle title)
      (.setNotebookGuid notebook-guid)
      (.setContent content-document)
      (#(if date (.setCreated % date)))
      (#(if (seq tag-names) (.setTagNames % tag-names))))
    (.createNote (note-store user) (access-token user))
    ))

(defn set-note-application-data-entry [user application-key guid application-data-entry]
  (.setNoteApplicationDataEntry
    (note-store user)
    (access-token user)
    guid
    application-key
    (str application-data-entry)))

(defn delete-note [user guid]
  (.deleteNote (note-store user) (access-token user) guid))

