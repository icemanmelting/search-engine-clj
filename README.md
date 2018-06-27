# search-engine-clj

This project was created to build a small search engine. It allows to query documents stored in a database (PGSQL).

# Usage

In order to use, first you need to deploy a local postgres instance, and create a database called `search_engine_test`.

After creation, run `postgresql-schema.sql` to deploy the schema needed. You can also check `dev-seed.sql`, to check the seed data,
used to create a simple user(user creation routes were not added due to time constraints) and the first document.

# Sessions

To create a session we need to send a post request to `http://localhost:8080/session`
with the JSON body containing `login` which is the user login info, and `password` the user password.
                    
If successful, you will get the session object in the response. To use any authorized route, you will need to add the `id` value,
of the session object, to the header of the request, like this `Authorization: Bearer session-id`.

# Adding documents

To add documents, first we need a filled authorization header (check above). Then, in the JSON body, we need to add the `content` and the `id`.

`curl -X POST  localhost:3000/_index \
     -d '{ "content" : "I really like bananas", "id" : "abc" }'`
     
It is important to mention here that if the id already exists, the content will be updated.
     
If successful, it will return the id of the created document in the response body

# Deleting documents

To delete documents, first we need a filled authorization header (check above). Then we need to pass a parameter in the route, to specify the document we need to delete.

`curl -X DELETE localhost:3000/_search/abc`

If successful, it will return the id of the deleted document in the response body
     
# Querying

To query documents, first we need a filled authorization header (check above). 
 You can query in 2 ways, by id:
 
 `curl localhost:3000/_search/abc`
 
 Or by query string. To query by query string we need to use a POST method, and specify the query string in the body:
 
 `curl -X POST  localhost:3000/_search \
      -d '{ "query" : "banana*" }'`
 
In both situations, the query will return a vector consisting of the documents it was able to match, or empty if nothing was found. 

# License

Copyright © 2018 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
