(ns clojurenote.enml-expectations
  (:import (com.evernote.edam.type Note))
  (:use [expectations] [clojurenote.enml]))

(def with-media "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE en-note SYSTEM \"http://xml.evernote.com/pub/enml2.dtd\">\n<en-note><div><en-media hash=\"96d170e1f8819cc5179ca102110a5962\" type=\"image/jpeg\"></en-media></div></en-note>")

; Simple example with defaults
(expect
  "<div>\nHello\n</div>\n"
  (enml->html "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE en-note SYSTEM \"http://xml.evernote.com/pub/enml2.dtd\">\n<en-note>Hello</en-note>"))

; Same simple example, but from a note
(expect
  "<div>\nHello\n</div>\n"
  (note->html
    (doto (Note.) (.setContent "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE en-note SYSTEM \"http://xml.evernote.com/pub/enml2.dtd\">\n<en-note>Hello</en-note>"))))

; Same simple example, with inline line feeds
(expect
  "<div>\nHello\n</div>\n"
  (enml->html
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
    <!DOCTYPE en-note SYSTEM \"http://xml.evernote.com/pub/enml2.dtd\">
    <en-note>Hello</en-note>"))

; Using default mappings for all 4 en-* tags
(expect
  "<div>\n<span>\n[Image in Evernote]\n</span>\n<span>\n[Encrypted in Evernote]\n</span>\n<span>\n\n</span>\nA todo item\n</div>\n"
  (enml->html
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
    <!DOCTYPE en-note SYSTEM \"http://xml.evernote.com/pub/enml2.dtd\">
    <en-note>
      <en-media hash=\"96d170e1f8819cc5179ca102110a5962\" type=\"image/jpeg\"></en-media>
      <en-crypt>zzzzz</en-crypt>
      <en-todo/>A todo item</en-note>"))

; Creating an HTML document
(expect
  "<html>\n<body>\nHello\n</body>\n</html>\n"
  (enml->html
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
    <!DOCTYPE en-note SYSTEM \"http://xml.evernote.com/pub/enml2.dtd\">
    <en-note>Hello</en-note>"
    :en-note en-note->html-document))

; Mapping media tags using example fn
(expect
  "<div>\n<img src='my-image.jpg' hash='96d170e1f8819cc5179ca102110a5962' type='image/jpeg'/>\n</div>\n"
  (enml->html
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
    <!DOCTYPE en-note SYSTEM \"http://xml.evernote.com/pub/enml2.dtd\">
    <en-note><en-media hash=\"96d170e1f8819cc5179ca102110a5962\" type=\"image/jpeg\"></en-media></en-note>"
    :en-media (en-media->simple-img-fn {"96d170e1f8819cc5179ca102110a5962" "my-image.jpg"})))

; Use two different mapper fns, also shows what media fn does for non image tags
(expect
  "<html>\n<body>\n<span>\n[Media in Evernote]\n</span>\n</body>\n</html>\n"
  (enml->html
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
    <!DOCTYPE en-note SYSTEM \"http://xml.evernote.com/pub/enml2.dtd\">
    <en-note><en-media hash=\"96d170e1f8819cc5179ca102110a5962\" type=\"audio/wav\"></en-media></en-note>"
    :en-note en-note->html-document
    :en-media (en-media->simple-img-fn {})))

(expect
  "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE en-note SYSTEM \"http://xml.evernote.com/pub/enml2.dtd\"><en-note><p>Hello</p></en-note>"
  (create-enml-document "<p>Hello</p>"))


