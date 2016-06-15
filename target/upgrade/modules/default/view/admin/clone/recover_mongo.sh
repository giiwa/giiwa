#!/bin/bash

mongorestore --host $1 --port $2 --db $3 --drop $4
