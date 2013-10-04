(ns clojurenote.enml
  (:use [clojure.walk :only [postwalk]])
  (:require
    [clojure.xml :as xml])
  )

; ** READ **

(defn plain-span [text]
  (struct xml/element :span nil [text]))

(defn en-media->simple-img-fn [resource-map]
  (fn [{{:keys [hash type] :as attrs} :attrs :as node}]
    (if (.startsWith type "image/")
      (assoc node :tag :img :attrs (assoc attrs :src (get resource-map hash))) 
      (plain-span "[Media in Evernote]")
    )))

(defn en-note->html-document [node]
  (struct xml/element :html nil [(assoc node :tag :body)]))

(def default-en-tag-fns {
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

(defn enml->html 
  ([enml] (enml->html enml default-en-tag-fns))
  ([enml en-tag-fns]
    (->>
      enml
      (remove-headers)
      (.getBytes)
      (java.io.ByteArrayInputStream.)
      (xml/parse)
      (postwalk (partial replace-tags en-tag-fns))
      (xml/emit-element)
      (with-out-str)
      )))

(defn note->html [{content :content :as note} & more]
  (apply enml->html (if content content (.getContent note)) more))  

; ** WRITE **

(def enml-header (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        "<!DOCTYPE en-note SYSTEM \"http://xml.evernote.com/pub/enml2.dtd\">"
        "<en-note>"))

(def enml-footer "</en-note>")

(defn create-enml-document [content]
  (format (str enml-header "%s" enml-footer) (if content content "")))
