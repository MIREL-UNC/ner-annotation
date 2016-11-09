#!/bin/bash
# If no configuration, use the templates
if [[ ! -e config/folders.txt ]]; then
    cp config/folders.txt.template config/folders.txt;
fi;
if [[ ! -e config/labels.txt ]]; then
    cp config/labels.txt.template config/labels.txt;
fi;
mvn spring-boot:run -Drun.addResources=true -Dserver.port=8080
