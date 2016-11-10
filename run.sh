#!/bin/bash
# If no configuration, use the templates
if [[ ! -e config/config.json ]]; then
    cp config/config.json.template config/config.json;
fi;
mvn spring-boot:run -Drun.addResources=true -Dserver.port=8080
