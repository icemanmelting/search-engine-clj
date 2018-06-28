# search-engine-clj

This project was created to build a small search engine. It allows to query documents stored in a database (PGSQL).

# Usage

In order to use, first you need to deploy a local postgres instance, and create a database called `documents_index_test`(check resources `db.edn` for more details, 
like user and password).

After creation, run `postgresql-schema.sql` to deploy the schema needed. You can also check `dev-seed.sql`, to check the seed data,
used to create a simple user(user creation routes were not added due to time constraints) and the first document.

The project contains a docker file and another file called `dock.sh`. This file will create an uberjar and deploy it to docker automatically. The only issue with this approach,
is that since the pg instance will be running outside the container, we will need to point to a known address besides localhost (since localhost will be the container's own ip).

This can be solved by adding `--net="host"` to the docker run command, or simply by using an external database. Note that this wouldn't be a problem for a staging or production environments,
since usually the db is running on a completely different machine, and configured accordingly in the `db.edn` file, located in the resources of the project.

# Sessions

To create a session we need to send a post request to `http://localhost:8080/session`
with the JSON body containing `login` which is the user login info, and `password` the user password.
                    
```
{
    "api_status": 201,
    "api_timestamp": "2018-06-28T13:48:21Z",
    "id": "cf1c4a12-36e7-443d-991a-e3aa1e4c955c",
    "login": "fabio@dias.com",
    "seen": "2018-06-28T13:48:21Z"
}
```                    
                    
If successful, you will get the session object in the response. To use any authorized route, you will need to add the `id` value,
of the session object, to the header of the request, like this `Authorization: Bearer session-id`.

Note: In this implementation, sessions have a time to live of 1 hour. If no request is done to the server with that session id, it will be terminated and won't be valid anymore.

# Adding documents

To add documents, first we need a filled authorization header (check above). Then, in the JSON body, we need to add the `content` and the `id`.

`curl -X POST  localhost:3000/_index \
     -d '{ "content" : "I really like bananas", "id" : "abc" }'`
     
It is important to mention here that if the id already exists, the content will be updated.
     
If successful, it will return the id of the created document in the response body:

```
{
    "api_status": 200,
    "api_timestamp": "2018-06-28T13:46:33Z",
    "id": "doc1"
}
```

# Deleting documents

To delete documents, first we need a filled authorization header (check above). Then we need to pass a parameter in the route, to specify the document we need to delete.

`curl -X DELETE localhost:3000/_search/abc`

If successful, it will return the id of the deleted document in the response body

```
{
    "api_status": 200,
    "api_timestamp": "2018-06-28T13:46:33Z",
    "id": "doc1"
}
```
     
# Querying

To query documents, first we need a filled authorization header (check above). 
 You can query in 2 ways, by id:
 
 `curl localhost:3000/_search/abc`

Here the response should look something like this:

```
{
    "api_status": 200,
    "api_timestamp": "2018-06-28T13:46:00Z",
    "id": "doc1",
    "content": "I really like bananas, apples not so much",
    "created_at": "2018-06-28T13:29:53Z"
}
```
 
 Or by query string. To query by query string we need to use a POST method, and specify the query string in the body:
 
 `curl -X POST  localhost:3000/_search \
      -d '{ "query" : "rea*" }'`
 
 As in this situation, the query can originate multiple results, the api result will look something like this:
 
 ```
 {
     "metadata": {
         "more_results": false,
         "next_offset": 2,
         "count": 2,
         "total": 2
     },
     "results": [
         {
             "api_status": 200,
             "api_timestamp": "2018-06-28T13:32:31Z",
             "id": "doc1",
             "content": "I really like bananas, apples not so much",
             "created_at": "2018-06-28T13:29:53Z"
         },
         {
             "api_status": 200,
             "api_timestamp": "2018-06-28T13:32:31Z",
             "id": "doc3",
             "content": "I really like oranges",
             "created_at": "2018-06-28T13:29:53Z"
         }
     ]
 }
 ```

# Possible future improvements

- Adding support for caching using Redis - This will enable faster results for documents indexing/search;
- Adding support for multiple command types in the same query string (idea is already in the works);
- Maybe using something else faster than regex, and without the need of holding a copy of the content on the server side (binary search?? maybe?).

# License

Copyright © 2018 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
