(ns clojurenote.write
  (:use [clojurenote.stores :only [note-store]])
  (:import
    (com.evernote.edam.type Note Notebook)))

(defn access-token [{access-token :access-token}]
  (when-not access-token (throw (Exception. "Must specify :access-token in user map")))
  access-token)

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
