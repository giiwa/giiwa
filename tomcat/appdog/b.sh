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

#tar
tar czf demo_`date +%Y%m%d`.tgz  dump

#cleanup
rm -rf dump
echo `date` >> b.log
echo '===end=====' >> b.log