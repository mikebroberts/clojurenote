(ns clojurenote.read
  (:use [clojurenote.stores :only [note-store]])
  (:import
    (com.evernote.edam.notestore NoteFilter NotesMetadataResultSpec)
    (com.evernote.edam.type NoteSortOrder Note Notebook)))

(defn access-token [{access-token :access-token}]
  (when-not access-token (throw (Exception. "Must specify :access-token in user map")))
  access-token)

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

(defn- remove-enml [c]
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
