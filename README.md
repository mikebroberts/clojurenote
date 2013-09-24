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
:dependencies [[clojurenote "0.2.0"]]
```

Prerequisites
---------------

* An Evernote developer API key (from [here](http://dev.evernote.com/doc/))
* You should start working against the Sandbox, rather than Production, environment. You can get a Sandbox account [here](https://sandbox.evernote.com/Registration.action).
* A basic knowledge of the fundamentals of the Evernote API is probably worth having.

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

Usage - Notes API
------------------------

The usage API is split in two. Most of what you need is based off the Evernote [NoteStore](http://dev.evernote.com/doc/reference/javadoc/com/evernote/edam/notestore/NoteStore.Client.html). You can create a NoteStore with `clojurenote.note/create-note-store` and call any method using interop. Alternatively there are convenience functions that are the bulk of `clojurenotes.notes` .

#### Evernote User

Most API functions take a 'user' map as their first argument. The simplest way of setting this is to use the map you got from 'obtain-access-token' in the authentication phase.

Alternatively the map must consist of the following entries:

* `:access-token` - The long access token string from the authentication phase

Then either of the following:

* `:notestore-url` - The user-specific notestore url (again, supplied during authentication)
* `:notestore` - An actual NoteStore object, typically created by calling `clojurenote.stores/create-note-store`

If you are using a [developer token](http://dev.evernote.com/doc/articles/authentication.php#devtoken) then `:access-token` will be your developer token, and you should specify a `:notestore` entry with the returned value from calling `clojurenote.notes/create-note-store-with-developer-token`

#### API functions

There is a very simple example of using the notes API in the `clojurenote-demo.use` namespace. Otherwise here are some repl examples:

``` clj
user=> (use '[clojurenote.notes :as notes])
nil

user=> (def en-user {:access-token "My-Access-Token" :notestore-url "https://sandbox.evernote.com/shard/s1/notestore"})
#'user/en-user

user=> (list-notebooks en-user)
#<ArrayList [Notebook(guid:...)]>

user=> (create-notebook en-user "TestNotebook")
#<Notebook Notebook(guid:...)>

user=> (find-notebook-by-name en-user "TestNotebook")
#<Notebook Notebook(guid:...)>

user=> (write-note en-user "my-notebook-guid" "First note" (create-enml-document "My content") nil nil)
#<Note Note(guid:...)>

user=> (get-note en-user "my-note-guid")
#<Note Note(guid:...)>

user=> (-> (get-note en-user "07b9c34e-b690-4bc1-954e-7b3bd513b01e") (bean) (:content) (remove-enml))
"My content"
``` 

#### Notes

* Objects returned have not had `bean` called on them due to possible performace constraints within the client application, but that's typically something you'll want to do first if you don't have such constraints.
* The content for any notes you create must be a valid ENML document. `clojurenote.notes/create-enml-document` will add the headers and footers for such documents.
* Similary the content of any notes returned from the API will be a full ENML document. Use `clojurenote.notes/remove-enml` to remove the headers and footers.

Usage - Users API
------------------------

As mentioned above most of what you need from the Evernote API is based off of the NoteStore (and `clojurenote.notes`), however there is also some useful stuff in the [UserStore](http://dev.evernote.com/doc/reference/javadoc/com/evernote/edam/userstore/UserStore.Client.html), and this is reflected in the `clojurenote.users` namespace.

All of the functions in `clojurenote.users` require a service argument, which should be one of `:production`, `:sandbox`, or `:yinxiang`, depending on which Evernote service you are accessing. Some also take an `access-token` argument, which is the long `:access-token` string returned that is part of the map returned by `clojurenote.auth/obtain-access-token`, or your developer token if you're using developer tokens.

Here are some examples:

```clj
user=> (use 'clojurenote.users)
nil

user=> (get-user-details :sandbox (:access-token full-access-token))
#<User User(id:...)>

user=> (get-notestore-url :sandbox (:access-token full-access-token))
"https://sandbox.evernote.com/shard/s1/notestore"
```
