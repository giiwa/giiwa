#!/bin/bash

mongodump --host $1 --port $2 --db $3 --out $4
