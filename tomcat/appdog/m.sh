#!/bin/sh

if [ -f "/home/data/db/mongod.lock" ]
then
	rm -f /home/data/db/mongod.lock
#	/home/mongo/bin/mongod --dbpath /home/data/db --repair
#	/home/mongo/bin/mongod --dbpath /home/data/db --repair --replSet doogoo --oplogSize=4000
fi

mkdir -p /home/data/db
ulimit -HSn 65536 > /dev/null 2>&1
ulimit -n 10240 >/dev/null 2>&1
/home/mongodb/bin/mongod --dbpath /home/data/db --port 27018 --bind_ip 127.0.0.1 &
