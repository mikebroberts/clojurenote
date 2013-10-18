(ns clojurenote.auth-expectations
  (:import (org.scribe.model Token))
  (:use [expectations] [clojurenote.auth]))

(def unprivate-create-access-map #'clojurenote.auth/create-access-token-map)

(def raw-response "oauth_token=zz-oauth-token&oauth_token_secret=&edam_shard=s1&edam_userId=my-user-id&edam_expires=1413655946244&edam_noteStoreUrl=https%3A%2F%2Fsandbox.evernote.com%2Fshard%2Fs1%2Fnotestore&edam_webApiUrlPrefix=https%3A%2F%2Fsandbox.evernote.com%2Fshard%2Fs1%2F")

(expect
  {:access-token "my-access-token"}
  (in (unprivate-create-access-map (Token. "my-access-token" "" raw-response))))

(expect
  {:userId "my-user-id"}
  (in (unprivate-create-access-map (Token. "" "" raw-response))))

(expect
  {:notestore-url "https://sandbox.evernote.com/shard/s1/notestore"}
  (in (unprivate-create-access-map (Token. "" "" raw-response))))

(expect
  {:shard "s1"}
  (in (unprivate-create-access-map (Token. "" "" raw-response))))

(expect
  {:expires "1413655946244"}
  (in (unprivate-create-access-map (Token. "" "" raw-response))))

(expect
  {:web-api-url-prefix "https://sandbox.evernote.com/shard/s1/"}
  (in (unprivate-create-access-map (Token. "" "" raw-response))))
