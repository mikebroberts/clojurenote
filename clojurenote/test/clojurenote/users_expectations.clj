(ns clojurenote.users-expectations
  (:import (com.evernote.edam.userstore UserStore$Client UserStoreIface PublicUserInfo)
           (com.evernote.edam.type User))
  (:use
    [expectations]
    [clojurenote.users]
    [erajure.core]))

(expect
  "https://www.evernote.com/edam/user"
  (user-store-url :production))

(expect
  Exception
  (user-store-url :something-else))

(expect
  UserStore$Client
  (create-user-store :sandbox))

(defn stub-create-user-store [stub-user-store]
  (fn [service]
    (when (= :sandbox service) stub-user-store)))

(expect-let [user (doto (User.) (.setUsername "Bob"))]
  user
  (with-redefs [clojurenote.users/create-user-store
                (stub-create-user-store
                  (mock UserStoreIface
                        (behavior (.getUser "access-token") user)))]
               (#(get-user-details :sandbox "access-token"))))


(expect-let [public-user (doto (PublicUserInfo.) (.setUsername "Bob"))]
  public-user
  (with-redefs [clojurenote.users/create-user-store
                (stub-create-user-store
                  (mock UserStoreIface
                        (behavior (.getPublicUserInfo "username") public-user)))]
               (#(get-public-user-info-for-username :sandbox "username"))))

(expect-let [user (doto (User.) (.setUsername "Bob"))
             public-user (doto (PublicUserInfo.) (.setUsername "Bob"))]
            public-user
            (with-redefs [clojurenote.users/create-user-store
                          (stub-create-user-store
                            (mock UserStoreIface
                                  (behavior (.getUser "access-token") user
                                            (.getPublicUserInfo "Bob") public-user)))]
                         (#(get-public-user-info-for-access-token :sandbox "access-token"))))

(expect
  "my-ns-url"
  (with-redefs [clojurenote.users/create-user-store
                  (stub-create-user-store
                    (mock UserStoreIface
                      (behavior (.getNoteStoreUrl "access-token") "my-ns-url")))]
    (#(get-notestore-url :sandbox "access-token"))))

(expect-let [store (mock UserStoreIface)]
            (interaction (.revokeLongSession store "my-token"))
            (with-redefs [clojurenote.users/create-user-store
                          (fn [_] store)]
                          (revoke-session :sandbox "my-token")))







