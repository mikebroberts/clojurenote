(ns clojurenote-demo.handler
  (:use compojure.core)
  (:require 
    [clojurenote-demo.auth :as auth]
    [clojurenote-demo.use :as use]
    [compojure.handler :as handler]
    [compojure.route :as route]
  ))

(defroutes app-routes
  (GET "/" [] "<html><body>
          <form method='post' action='/login-evernote'>
            <button type='submit'>Login to Evernote</button>
          </form>
          <a href='/use'>Use Evernote</a>
          </body></html>")

  (POST "/login-evernote" [] (auth/login-evernote))

  (GET "/evernote-oauth-callback" {:keys [params session]} 
    (auth/evernote-oauth-callback params session))

  (GET "/use" [] "<html><body>
                  <form method='post' action='/query-api'>
                    <p>Access Token: <input type='text' name='token' placeholder='token' size=80/></p>
                    <p>Notebook URL: <input type='text' name='notestore-url' placeholder='notestore-url' size=80/></p>
                    <br/>
                    <button type='submit' name='list-notebooks'>List Notebooks</button>
                  </form>
                    ")

  (POST "/query-api" [token notestore-url] 
    (use/list-notebooks token notestore-url))

  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))