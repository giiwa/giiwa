#!/bin/bash

#mongo backup shell

source /etc/profile

cd /home/disk1/backup/
echo '===start===='>>b.log
echo `date` >> b.log

#cleanup
rm -rf dump

#dump
mongodump --host g09 --port 27018 -d demo

#restore
#mongorestore --host g09 --port 27018 -d demo .

#tar, very slow
#tar czf demo_`date +%Y%m%d`.tgz  dump

#move
mv dump /home/disk3/backup/pdc1/`date '+%Y%m%d'`/

#cleanup
rm -rf dump

#cleanup old dump
find /home/disk3/backup/pdc1/ -maxdepth 1 -mtime +10 -type d -name "*" -exec \rm -rf {} \;

echo `date` >> b.log
echo '===end=====' >> b.log