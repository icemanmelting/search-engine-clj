#!/bin/sh
lein clean

if lein uberjar; then
    docker build -t search-engine .
    echo "Docker image build successfully. Running..."
    docker run -e SEARCH_ENGINE_ENV=default  -it --rm -p 8080:8080  search-engine
else
    echo "Cannot compile"
fi
