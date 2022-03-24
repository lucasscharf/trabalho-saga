#!/bin/bash

curl -X POST "http://localhost:8085/pagamento/pagarComSucesso/$1" -H  "accept: */*" -H  "Content-Type: application/json" -d "$2"
