(ns clojurenote.auth
  "Functions to interact with Evernote's OAuth workflow to retrieve an access token. 
    Follows specification at http://dev.evernote.com/doc/articles/authentication.php"
  (:import 
    (org.scribe.utils OAuthEncoder)
    (org.scribe.builder ServiceBuilder)
    (org.scribe.model Token Verifier)
    (org.scribe.builder.api EvernoteApi EvernoteApi$Sandbox)))

(defn- create-builder [{:keys [key secret callback use-sandbox] :or {use-sandbox false}}]
  (when (nil? key) (throw (Exception. "Error - key for Evernote Auth cannot be nil"))) 
  (when (nil? secret) (throw (Exception. "Error - secret for Evernote Auth cannot be nil"))) 
  (when (nil? callback) (throw (Exception. "Error - secret for Evernote Auth cannot be nil")))
  (-> (ServiceBuilder.)
    (.provider (if use-sandbox EvernoteApi$Sandbox EvernoteApi))
    (.apiKey key)
    (.apiSecret secret)
    (.callback callback)
    (.build)))

(defn obtain-request-token 
  "Retrieve an access token and authorization url. The returned map must be used 
    later when calling obtain-access-token so will most likely be stored temporarily 
    in the user's session.
  config must be a map of:
    :key - required - Your project's API key
    :secret - required - Your project's API secret
    :callback - required 
              - the full URL that the user will be redirected to after completing
                login on the Evernote site
    :use-sandbox - not required, defaults to false 
                 - if false use Evernote production servers, otherwise use sandbox 
                   servers
  Returned map will contain a :url entry that you should redirect the user to in
    order that they can login with Evernote.
    "
  [config]
  (let [service (create-builder config) 
    token (.getRequestToken service)]
    (-> token 
      (bean) 
      (select-keys [:token :secret :rawResponse])
      (assoc :url (.getAuthorizationUrl service token)))))

(defn- update-values [m f] (into {} (for [[k v] m] [k (f v)])))

(defn- decode-token [t]
  (-> 
    (->> 
      (-> t (.getRawResponse) (clojure.string/split #"&|=")) 
      (apply hash-map)) 
    (update-values #(OAuthEncoder/decode %))))

(defn- create-access-token-map [t]
  (let [decoded (decode-token t)]
    (hash-map 
      :access-token (.getToken t) 
      :userId (get decoded "edam_userId")
      :notestore-url (get decoded "edam_noteStoreUrl")
      :shard (get decoded "edam_shard")
      :expires (get decoded "edam_expires")
      :web-api-url-prefix (get decoded "edam_webApiUrlPrefix")
      )))

(defn obtain-access-token 
  "Retrieve an access token for a given verifier and request token.
    - config should be the same as for obtain-request-token. 
    - verifier should be the oauth_verifier passed on the callback url
    - request token should be the same map that was returned when obtain-request-token
      was called
  Returned value will be a map, including:
    - :access-token - the actual access token to be used when calling Evernote API
      on behalf of user
    - various other user details - :userId, :notestore-url, :shard, 
                                    :expires, :web-api-url-prefix"
  [config verifier {:keys [token secret rawResponse]}]
  (-> config 
    (create-builder) 
    (.getAccessToken (Token. token secret rawResponse) (Verifier. verifier))
    (create-access-token-map)))
