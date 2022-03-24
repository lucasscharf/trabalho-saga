#!/bin/bash

curl -X GET "http://localhost:8085/pagamento/$1" -H  "accept: */*"