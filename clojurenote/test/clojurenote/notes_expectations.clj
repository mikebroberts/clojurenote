(ns clojurenote.notes-expectations
  (:import
    (com.evernote.edam.type Notebook))
  (:use 
    [expectations]
    [clojurenote.notes]
    )
  )

; -- Setup / Stubbing note store

(def expected-notestore-url "my-notestore-url")
(def expected-token "my-token")
(def en-user {
  :notestore-url expected-notestore-url
  :access-token expected-token })

(def notebooks [
    (doto (Notebook.) (.setName "Book1") (.setGuid "Guid1"))
    (doto (Notebook.) (.setName "Book2") (.setGuid "Guid2"))])

(def stub-note-store
  (reify com.evernote.edam.notestore.NoteStoreIface
    (listNotebooks [this token]
      (when (= token expected-token)
        notebooks)
    )))

(defn create-note-store-stub [url]
  (if (= expected-notestore-url url)
    stub-note-store
    (throw (Exception. (str "Unexpected note store url:" url)))))

(defn stubbing-note-store [f]
  (with-redefs [clojurenote.notes/create-note-store create-note-store-stub]
    (f)))

;---

; ToDo - can we update project.clj so that testing libraries are a build-time-only dependency?

(expect 
  notebooks
  (stubbing-note-store #(list-notebooks en-user)))

(expect 
  "Guid1"
  (stubbing-note-store #(-> (find-notebook-by-name en-user "Book1") (.getGuid))))

(expect 
  nil
  (stubbing-note-store #(find-notebook-by-name en-user "Book3")))

(expect 
  "Book2"
  (stubbing-note-store #(-> (find-notebook-by-guid en-user "Guid2") (.getName))))


