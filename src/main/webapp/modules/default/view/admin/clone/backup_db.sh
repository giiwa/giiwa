#!/bin/bash

#postgresql
sudo -u postgres pg_dump $1 > $2

#mysql
#mysqldump -u root $1 > $2