#!/bin/bash

curl -X POST "http://localhost:8090/emissao/emitirComSucesso/$1" -H  "accept: */*" -H  "Content-Type: application/json" -d "$2"
