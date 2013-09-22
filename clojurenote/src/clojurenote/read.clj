(ns clojurenote.read
  (:use [clojurenote.stores :only [note-store]])
  (:import
    (com.evernote.edam.notestore NoteFilter NotesMetadataResultSpec)
    (com.evernote.edam.type NoteSortOrder)
    (com.evernote.edam.type Note)
    (com.evernote.edam.type Notebook)))

(defn access-token [{access-token :access-token}]
  (when-not access-token (throw (Exception. "Must specify :access-token in en-user map")))
  access-token)

(defn list-notebooks [en-user]
  (.listNotebooks (note-store en-user) (access-token en-user)))

(defn find-notebook-by-predicate [en-user pred]
  (->> (list-notebooks en-user)
    (filter pred)
    (first)))

(defn find-notebook-by-name [en-user notebook-name]
  (find-notebook-by-predicate en-user #(= notebook-name (.getName %))))

(defn find-notebook-by-guid [en-user notebook-guid]
  (find-notebook-by-predicate en-user #(= notebook-guid (.getGuid %))))

(defn basic-notes-for-notebook [en-user notebook-guid]
  (->
    (.findNotesMetadata 
      (note-store en-user)
      (access-token en-user)
      (doto (NoteFilter.)
        (.setNotebookGuid notebook-guid)
        (.setOrder (.getValue NoteSortOrder/CREATED))
        (.setAscending false)) 
      0 
      100 
      (NotesMetadataResultSpec.))
    (.getNotes)) 
  )

(defn get-note [en-user guid]
  (.getNote 
    (note-store en-user) 
    (access-token en-user)
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

(defn get-note-application-data-entry [en-user application-key guid]
  (-> (note-store en-user)
    (.getNoteApplicationDataEntry (access-token en-user) guid application-key)
    ))

(defn get-all-tags-for-notebook [en-user notebook-guid]
  (.listTagsByNotebook (note-store en-user) (access-token en-user) notebook-guid))
