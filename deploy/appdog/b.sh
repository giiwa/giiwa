#!/bin/bash

#mongo backup shell

source /etc/profile

home=/home/disk3/backup

cd $home

echo '===start===='>>b.log
echo `date` >> b.log

#dump
mongodump --host s132 --port 27018 -d prod --gzip -o $home/pdc2_$(date +%Y%m%d)

#restore
#mongorestore --drop --host s110 --port 55011 -d demo dump/demo

#dump
mongodump --host s04 --port 27018 -d prod --gzip -o $home/pdc1_$(date +%Y%m%d)

#restore
#mongorestore --drop --host s110 --port 55011 -d dw dump/dw/

#cleanup old dump
find $home -maxdepth 1 -mtime +5 -type d -name "pdc1_*" -exec \rm -rf {} \;
find $home -maxdepth 1 -mtime +5 -type d -name "pdc2_*" -exec \rm -rf {} \;

echo `date` >> b.log
echo '===end=====' >> b.log