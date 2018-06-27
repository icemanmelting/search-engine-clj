#!/bin/sh
rm search-engine.zip
lein clean

if lein uberjar; then
    zip -r search-engine.zip Dockerfile* ./resources/* ./target/uberjar/search-engine-standalone.jar
    echo "ZIP created successfully"
else
    echo "Cannot create ZIP"
fi
