#!/bin/bash

export MAVEN_OPTS="-Xmx256m -XX:MaxDirectMemorySize=256m"

docker-compose up -d 
docker-compose up -d 
cd acesso
mvn quarkus:dev > /dev/null &

cd ../emissao
mvn quarkus:dev  > /dev/null &

cd ../inscricao
mvn quarkus:dev  > /dev/null &

cd ../pagamento
mvn quarkus:dev > /dev/null  &

cd ../acesso
mvn quarkus:dev > /dev/null  &

cd ../logs
mvn quarkus:dev 