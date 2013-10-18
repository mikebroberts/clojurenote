(ns clojurenote.enml
  (:use [clojure.walk :only [postwalk]])
  (:require [clojure.xml :as xml]))

; ** READ **

(defn plain-span [text]
  (struct xml/element :span nil [text]))

(defn en-media->simple-img-fn
  "An alternate translation for <en-media> that will produce <img> tags if the 
  type of media starts with 'image/' . This function actually produces the function 
  that should be passed to enml-> html, given a resource-map argument. This would be a 
  map of hash-to-URL. It thus requires knowledge of all the resource hashes 
  contained within the ENML ahead of time, and this would typically be gathered by
  examining the resources of the original Note, possibly using 
  clojurenote.notes/resource->bean-with-body-hash-hex"
  [resource-map]
  (fn [{{:keys [hash type] :as attrs} :attrs :as node}]
    (if (.startsWith type "image/")
      (assoc node :tag :img :attrs (assoc attrs :src (get resource-map hash))) 
      (plain-span "[Media in Evernote]")
    )))

(defn en-note->html-document 
  "An alternate translation for <en-note> that will create a complete HTML document."
  [node]
  (struct xml/element :html nil [(assoc node :tag :body)]))

(def default-en-tag-fns ^{:doc 
  "Default translation behavior for enml->html. Override some or all of the functions
  in this map to specify your own translation behavior for each of the en- tags.
  Each function must take exactly 1 argument, which will be the top level node 
  (as provided by clojure.xml) for a given XML tree of that type. See the
  en-*->* functions as examples of how your own functions should work"}
  {
  :en-note (fn [node] (assoc node :tag :div))
  :en-media (fn [_] (plain-span "[Image in Evernote]"))
  :en-crypt (fn [_] (plain-span "[Encrypted in Evernote]"))
  :en-todo (fn [_] (plain-span ""))
  })

(defn remove-headers [enml]
  (-> enml 
    (clojure.string/replace "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" "")
    (clojure.string/replace "<!DOCTYPE en-note SYSTEM \"http://xml.evernote.com/pub/enml2.dtd\">" "")))

(defn replace-tags [tag-fns node]
  (if-let [tag-fn (get tag-fns (:tag node))]
    (tag-fn node)
    node))

(defn walk-fn [user-supplied-tag-fns]
  (partial replace-tags (into default-en-tag-fns (apply hash-map user-supplied-tag-fns))))

(defn enml->html
  "Translates an ENML document to HTML by removing headers and translating <en-*> tags.
  Default translation is simplist possible, but won't (for example) map <en-media> tags
  to <img> tags. Specify optional en-tag-fns argument in order to customize the tag
  translation behavior. See documentation on default-en-tag-fns for more detail." 
  [enml & optional-en-tag-fns]
    (->>
      enml
      (remove-headers)
      (.getBytes)
      (java.io.ByteArrayInputStream.)
      (xml/parse)
      (postwalk (walk-fn optional-en-tag-fns))
      (xml/emit-element)
      (with-out-str)
      ))

(defn note->html 
  "Same as enml->html (including the same optional argument), but takes a Note instead of
  ENML document. Note can be the original Java object returned by the Evernote API, or can
  be the 'beaned' version of the same object."
  [{content :content :as note} & more]
  (apply enml->html (if content content (.getContent note)) more))  

; ** WRITE **

(def enml-header (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        "<!DOCTYPE en-note SYSTEM \"http://xml.evernote.com/pub/enml2.dtd\">"
        "<en-note>"))

(def enml-footer "</en-note>")

(defn create-enml-document 
  "Creates an ENML document by adding XML headers and wrapping with a <en-note> tag.
  Original content must already be valid ENML"
  [content]
  (format (str enml-header "%s" enml-footer) (if content content "")))
