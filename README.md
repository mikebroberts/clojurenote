clojurenote
===========

A Clojure library to access the Evernote API. 

This is still in early days. Please feel free to bug me about errors / ommissions!

I'm still to add tests, but this code is almost the same as that which I've been running in production for a couple of months.

Overview
-------------

A library that wraps the [official Evernote Java API](https://github.com/evernote/evernote-sdk-java) . As of writing it implements the following:

* OAuth authentication (using [Scribe Java](https://github.com/fernandezpablo85/scribe-java))
* Basic read/write capabilities, using an OAuth access token, or developer token.

Installation
-------------

Include the following dependency in your `project.clj` file:

```clojure
:dependencies [[clojurenote "0.1.1"]]
```

Prerequisites
---------------

* An Evernote developer API key (from [here](http://dev.evernote.com/doc/))
* You should start working against the Sandbox, rather than Production, environment. You can get a Sandbox account [here](https://sandbox.evernote.com/Registration.action).
* A basic knowledge of the fundamentals of the Evernote API is probably worth having


Usage - Authentication
-------------------------

For OAuth authentication you need to have a web application. There is a sample app here in the `clojurenote-demo` tree. Check out that directory and use it as the basis of your implementation.

Note that you only *need* a web application for authentication. Once you've got your access token you can access the API from any type of app.

Take a look at `auth.clj` file. There are a few options for configuring the demo. The easiest way is to hard code your API key and secret into the `config` function. Alternatively you can use environment variables / leiningen config as explained in `auth.clj`

Once you've got your config ready run the app as follows:

```bash
$ lein ring server
```
(This assumes you have [Leiningen](http://leiningen.org/) installed)

This should open up a browser at `http://localhost:3000`. Press the 'Login to Evernote' button to initiate the login process. Eventually you should get back to a page giving your
access token and notestore URL. Copy these 2 things down since you'll need them next.

Usage - API Access
------------------------

#### Evernote User

All API functions that call the Evernote api take a 'user' map as their first argument. The simplest way of setting this is to use the map you got from 'obtain-access-token' in the authentication phase.

Alternatively the map must consist of the follow entries:

* `:access-token` - The long access token string from the authentication phase

Then either of:

* `:notestore-url` - The user-specific notestore url (again, supplied during authentication)
* `:notestore` - An actual NoteStore object, typically created by calling `clojurenote.stores/create-note-store`

If you are using a [developer token](http://dev.evernote.com/doc/articles/authentication.php#devtoken) then `:access-token` will be your developer token, and you should specify a `:notestore` entry with the returned value from calling `clojurenote.stores/create-note-store-with-developer-token`

#### API functions

There is a very simple example in the clojurenote-demo `use.clj` namespace. Otherwise here are some repl examples:


``` clj
user=> (require '[clojurenote.read :as read])
nil

user=> (require '[clojurenote.write :as write])
nil

user=> (def en-user {:access-token "My-Access-Token" :notestore-url "https://sandbox.evernote.com/shard/s1/notestore"})
#'user/en-user

user=> (read/list-notebooks en-user)
#<ArrayList [Notebook(guid:...)]>

user=> (write/create-notebook en-user "TestNotebook")
#<Notebook Notebook(guid:...)>

user=> (read/find-notebook-by-name en-user "TestNotebook")
#<Notebook Notebook(guid:...)>

user=> (write/write-note en-user "my-notebook-guid" "First note" (write/create-enml-document "My content") nil nil)
#<Note Note(guid:...)>
``` 

#### Notes

* Call any other method on a [NoteStore](http://dev.evernote.com/doc/reference/javadoc/com/evernote/edam/notestore/NoteStore.Client.html) by using the notestore returned by `clojurenote.stores/create-note-store`
* Objects returned have not had `bean` called on them due to possible performace constraints within the client application, but that's typically something you'll want to do first if you don't have such constraints.
* The content for any notes you create must be a valid ENML document. `clojurenote.write/create-enml-document` will add the headers and footers for such documents.
