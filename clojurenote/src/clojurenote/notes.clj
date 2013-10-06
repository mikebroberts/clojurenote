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

(defn create-note-store-with-developer-token 
  "Only use if you're using a developer token, rather than OAuth, to authenticate.
    service should be a key from the clojurenote.users/services map.
    For more info about developer keys see
    http://dev.evernote.com/doc/articles/authentication.php#devtoken"
  [service dev-token]
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

(defn get-note-application-data-entry [user application-key guid]
  (-> (note-store user)
    (.getNoteApplicationDataEntry (access-token user) guid application-key)
    ))

(defn get-all-tags-for-notebook [user notebook-guid]
  (.listTagsByNotebook (note-store user) (access-token user) notebook-guid))

(defn byte-to-hex [b]
  (let [intVal (bit-and 0xFF b)]
    (str (when (< intVal 0x10) "0") (Integer/toHexString intVal))))

; based on https://github.com/vanduynslagerp/enml4j/blob/master/src/main/java/com/syncthemall/enml4j/util/Utils.java -> bytesToHex
(defn bytes-to-hex [bs]
  (apply str (map byte-to-hex bs)))

(defn resource->bean-with-body-hash-hex 
  "Takes a Resource (as returned by Note.getResources) and:
  - converts it to a map with bean (non recursively - data, recognition and attributes are still in Java Object form)
  - sets :body-hash-hex to be the hexadecimal string of .getBodyHash (and which can then be matched to the hash in <en-media> tags)
  - prunes out unnecessary fields, plus the fields.
  This is typically useful when needing the resources hashes when translating <en-media> tags
  in the Note's content."
  [res]
  (-> res
    (bean)
    (#(assoc % :body-hash-hex (->> % (:data) (.getBodyHash) (bytes-to-hex))))
    (select-keys [:guid :noteGuid :mime :width :height :updateSequenceNum 
      :body-hash-hex :data :recognition :attributes])
  ))

(defn entity->guid [entity]
  "Returns the guid of an entity that has a 'guid' field. 
    Works whether the entity is the original Java object, or whether it has been bean'ed"
  (if-let [guid (:guid entity)]
    guid
    (.getGuid entity)))

(defn resource-url 
  "Returns the URL for downloading a resource, as described at http://dev.evernote.com/doc/articles/resources.php#downloading
  - web-api-url-prefix must be the webApiUrlPrefix field of the user's PublicUserInfo. To get this do something like:
    (-> (clojurenote.users/get-public-user-info-for-username username) (.getWebApiUrlPrefix))
  - res should be the resource object taken from the resources list of the original note. It can be the original java object,
    or a bean'ed version"
  [web-api-url-prefix res]
  (str web-api-url-prefix "res/" (entity->guid res)))

(defn- thumbnail-url [web-api-url-prefix entity-type entity & {:keys [img-format size]}]
  (str
    web-api-url-prefix
    "thm/"
    (name entity-type)
    "/"
    (entity->guid entity)
    (when img-format (str "." img-format))
    (when size (str "?size=" size))
    ))

(defn resource-thumbnail-url 
  "Return the URL for the thumbnail of a given resource, as described at http://dev.evernote.com/doc/articles/thumbnails.php
  - web-api-url-prefix must the same as for clojurenote.notes/resource-url
  - res should be the resource object taken from the resources list of the original note. It can be the original java object,
    or a bean'ed version
  - options are named parameters as follows:
    - :img-format [one of jpg, gif, bmp, png]
    - :size size (recommended to be 75, 150 or 300)"
  [web-api-url-prefix res & options]
  (apply (partial thumbnail-url web-api-url-prefix :res res) options))

(defn note-thumbnail-url [web-api-url-prefix note & options]
  "Return the URL for the thumbnail of a note, as described at http://dev.evernote.com/doc/articles/thumbnails.php.
  Parameters are the same as resource-thumbnail-url, except swap resource for note"
  (apply (partial thumbnail-url web-api-url-prefix :note note) options))

; ** WRITE FUNCTIONS **

(defn create-notebook [user notebook-name]
  (->> 
    (doto (Notebook.) (.setName notebook-name))
    (.createNotebook (note-store user) (access-token user))))

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

