#!/bin/bash

servie postgresql restart
sudo -u postgres dropdb $1
sudo -u postgres createdb $1
echo "grant all on database $1 to $3;" | sudo -u postgres psql $1
sudo -u postgres psql $1 < $2

