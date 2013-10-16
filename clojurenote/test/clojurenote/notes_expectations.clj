(ns clojurenote.notes-expectations
  (:import
    (com.evernote.edam.type Notebook NoteSortOrder Note Tag Resource Data ResourceAttributes)
    (com.evernote.edam.notestore NoteStoreIface NoteMetadata NoteFilter NotesMetadataResultSpec NotesMetadataList)
    )
  (:use 
    [expectations]
    [clojurenote.notes]
    [erajure.core]
    ))

(expect
  "actual-notestore"
  (note-store {:notestore "actual-notestore" :notestore-url "Shouldn't be used"}))

(def test-notebook-1 (doto (Notebook.) (.setName "Book1") (.setGuid "Guid1")))
(def test-notebook-2 (doto (Notebook.) (.setName "Book2") (.setGuid "Guid2")))
(def all-notebooks [test-notebook-1 test-notebook-2])

; In this test we actually stub out the create-note-store fn to show how it will be used when a user
; specifies notestore by url
; In subsequent tests we'll just pass a stub note store in the user map
(defn stub-create-note-store [url]
  (if (= "stub-notestore-url" url)
    (mock NoteStoreIface (behavior (.listNotebooks "my-token") all-notebooks))
    (throw (Exception. (str "Unexpected note store url:" url)))))

(expect
  all-notebooks
  (with-redefs [clojurenote.notes/create-note-store stub-create-note-store]
    (#(list-notebooks {:notestore-url "stub-notestore-url" :access-token "my-token"}))))

(expect
  test-notebook-1
  (let [stub-store (mock NoteStoreIface (behavior (.listNotebooks "my-token") all-notebooks))]
    (find-notebook-by-name {:notestore stub-store :access-token "my-token"} "Book1")))

(expect 
  nil
  (let [stub-store (mock NoteStoreIface (behavior (.listNotebooks "my-token") all-notebooks))]
    (find-notebook-by-name {:notestore stub-store :access-token "my-token"} "Book3")))

(expect 
  test-notebook-2
  (let [stub-store (mock NoteStoreIface (behavior (.listNotebooks "my-token") all-notebooks))]
    (find-notebook-by-guid {:notestore stub-store :access-token "my-token"} "Guid2")))

(def note-metadata-1 (doto (NoteMetadata.) (.setTitle "Note 1") (.setGuid "NoteGuid1")))
(def note-metadata-2 (doto (NoteMetadata.) (.setTitle "Note 2") (.setGuid "NoteGuid2")))
(def all-notes-metadata [note-metadata-1 note-metadata-2])

(expect
  all-notes-metadata
  (let [expected-filter (doto (NoteFilter.)
                          (.setNotebookGuid "my-notebook-guid")
                          (.setOrder (.getValue NoteSortOrder/CREATED))
                          (.setAscending false))
        stub-store
        (mock NoteStoreIface
            (behavior (.findNotesMetadata "my-token" expected-filter 0 100 (NotesMetadataResultSpec.))
                      (doto (NotesMetadataList.) (.setNotes all-notes-metadata))))]

    (basic-notes-for-notebook {:notestore stub-store :access-token "my-token"} "my-notebook-guid")
    ))

(def note (doto (Note.) (.setTitle "Note 1")))

(expect
  note
  (let [stub-store (mock NoteStoreIface
                     (behavior (.getNote "my-token" "my-note-guid" true false false false) note))]
    (get-note {:notestore stub-store :access-token "my-token"} "my-note-guid")))

(expect
  "my-app-data"
  (let [stub-store (mock NoteStoreIface
                     (behavior (.getNoteApplicationDataEntry "my-token" "my-note-guid" "app-key")
                               "my-app-data"))]
    (get-note-application-data-entry
      {:notestore stub-store :access-token "my-token"} "app-key" "my-note-guid")))

(def all-tags [(doto (Tag.) (.setName "Tag 1")) , (doto (Tag.) (.setName "Tag 2"))])

(expect
  all-tags
  (let [stub-store (mock NoteStoreIface
                     (behavior (.listTagsByNotebook "my-token" "my-notebook-guid") all-tags))]
    (get-all-tags-for-notebook {:notestore stub-store :access-token "my-token"} "my-notebook-guid")))

(expect
  "81ff0001027f"
  (bytes-to-hex (byte-array (map byte [-127 -1 0 1 2 127]))))

(expect
  {:guid "Resource-Guid" :noteGuid "Note-Guid" :mime "mimetype"
   :width 100 :height 200 :updateSequenceNum 123
   :body-hash-hex "04050607"
   :data (doto (Data.) (.setBodyHash (byte-array (map byte [4 5 6 7]))))
   :attributes (doto (ResourceAttributes.) (.setCameraMake "my-camera"))
   :recognition (doto (Data.) (.setBody (byte-array (map byte [1 1 1 1]))))
   }
  (resource->bean-with-body-hash-hex
    (doto (Resource.)
     (.setGuid "Resource-Guid") (.setNoteGuid "Note-Guid") (.setMime "mimetype")
     (.setWidth 100) (.setHeight 200) (.setUpdateSequenceNum 123)
     (.setRecognition (doto (Data.) (.setBody (byte-array (map byte [1 1 1 1])))))
     (.setAttributes (doto (ResourceAttributes.) (.setCameraMake "my-camera")))
     (.setData (doto (Data.) (.setBodyHash (byte-array (map byte [4 5 6 7])))))
     )))

(expect
  "1234"
  (entity->guid (doto (Note.) (.setGuid "1234"))))

(expect
  "1234"
  (entity->guid {:guid "1234"}))

(expect
  "http://prefix/res/1234"
  (resource-url "http://prefix/" {:guid "1234"}))

(expect
  "http://prefix/thm/res/1234"
  (resource-thumbnail-url "http://prefix/" {:guid "1234"}))

(expect
  "http://prefix/thm/res/1234.gif?size=150"
  (resource-thumbnail-url "http://prefix/" {:guid "1234"} :img-format "gif" :size 150))

(expect
  "http://prefix/thm/note/1234"
  (note-thumbnail-url "http://prefix/" {:guid "1234"}))

(expect
  "http://prefix/thm/note/1234.gif?size=150"
  (note-thumbnail-url "http://prefix/" {:guid "1234"} :img-format "gif" :size 150))

(expect-let [store (mock NoteStoreIface)]
            (interaction (.createNotebook store "my-token" (doto (Notebook.) (.setName "New Notebook"))))
            (create-notebook {:notestore store :access-token "my-token"} "New Notebook"))

(expect-let [store (mock NoteStoreIface)]
            (interaction
              (.createNote store "my-token"
                (doto (Note.) (.setTitle "My Note") (.setNotebookGuid "nb-guid")
                              (.setContent "My content"))))
            (write-note {:notestore store :access-token "my-token"}
                        "nb-guid" "My Note" "My content" nil nil))

(expect-let [store (mock NoteStoreIface)]
            (interaction
              (.createNote store "my-token"
                           (doto (Note.) (.setTitle "My Note") (.setNotebookGuid "nb-guid")
                                         (.setContent "My content") (.setCreated 1234)
                                         (.setTagNames ["My Tag"]))))
            (write-note {:notestore store :access-token "my-token"}
                        "nb-guid" "My Note" "My content" 1234 ["My Tag"]))

(expect-let [store (mock NoteStoreIface)]
            (interaction
              (.setNoteApplicationDataEntry store "my-token" "my-guid" "app-key" "{:appdata \"foo\"}"))
            (set-note-application-data-entry {:notestore store :access-token "my-token"}
                                             "app-key" "my-guid" {:appdata "foo"}))

(expect-let [store (mock NoteStoreIface)]
            (interaction (.deleteNote store "my-token" "my-guid"))
            (delete-note {:notestore store :access-token "my-token"} "my-guid"))


