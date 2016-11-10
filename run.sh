#!/bin/bash
# If no configuration, use the templates
if [[ ! -e config/folders.txt ]]; then
    cp config/folders.txt.template config/folders.txt;
fi;
if [[ ! -e config/labels.json ]]; then
    cp config/labels.json.template config/labels.json;
fi;
mvn spring-boot:run -Drun.addResources=true -Dserver.port=8080
