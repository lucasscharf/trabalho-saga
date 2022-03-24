#!/bin/bash

curl -X POST "http://localhost:8080/inscricao" -H  "accept: */*" -H  "Content-Type: application/json" -d "{\"descricao\":\"desc\",\"id\":0,\"nome\":\"$1\",\"status\":\"status\"}"
