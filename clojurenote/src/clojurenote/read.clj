(ns clojurenote.read
  (:require [clojure.tools.reader.edn :as edn])
  (:use [clojurenote.stores :only [note-store access-token]])
  (:import
    (com.evernote.edam.notestore NoteFilter NotesMetadataResultSpec)
    (com.evernote.edam.type NoteSortOrder)
    (com.evernote.edam.type Note)
    (com.evernote.edam.type Notebook)))

(defn list-notebooks []
  (.listNotebooks (note-store) (access-token)))

(defn find-notebook-by-predicate [pred]
  (->> (list-notebooks)
    (filter pred)
    (first)))

(defn find-notebook-by-name [notebook-name]
  (find-notebook-by-predicate #(= notebook-name (.getName %))))

(defn find-notebook-by-guid [notebook-guid]
  (find-notebook-by-predicate #(= notebook-guid (.getGuid %))))

(defn basic-notes-for-notebook [notebook-guid]
  (->>
    (->
      (.findNotesMetadata 
        (note-store)
        (access-token)
        (doto (NoteFilter.)
          (.setNotebookGuid notebook-guid)
          (.setOrder (.getValue NoteSortOrder/CREATED))
          (.setAscending false)) 
        0 
        100 
        (NotesMetadataResultSpec.))
      (.getNotes)) 
    (map bean)))

(defn- remove-enml [c]
  (-> c
    (clojure.string/replace "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" "")
    (clojure.string/replace "<!DOCTYPE en-note SYSTEM \"http://xml.evernote.com/pub/enml2.dtd\">" "")
    (clojure.string/replace "<en-note>" "")
    (clojure.string/replace "</en-note>" "")
    (clojure.string/trim)
    ))

(defn get-note [guid]
  (-> (.getNote 
        (note-store) 
        (access-token)
        guid true false false false)
    (bean) 
    (update-in [:content] remove-enml)))

(defn get-note-application-data-entry [application-key guid]
  (-> (note-store)
    (.getNoteApplicationDataEntry (access-token) guid application-key)
    (edn/read-string)))

(defn get-all-tags-for-notebook [notebook-guid]
  (->>
    (.listTagsByNotebook (note-store) (access-token) notebook-guid)
    (map bean)))