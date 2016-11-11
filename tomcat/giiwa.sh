#!/bin/sh

echo "Starting giiwa ..."

PRG="$0" 
while [ -h "$PRG" ] ; do
	ls=`ls -ld "$PRG"`
	link=`expr "$ls" : '.*-> \(.*\)$'`
	if expr "$link" : '/.*' > /dev/null; then
		PRG="$link"
	else
		PRG=`dirname "$PRG"`/"$link"
	fi
done

os=`uname -s`
startup=`pwd`/bin

getpid() {
	case $os in
	Linux)
		pid=`ps fux |grep java | grep $1 |grep -v grep | awk '{print $2}'` 
		;;
	SunOs)
		pid=`ps -gxww |grep java |grep $1 |grep -v grep | awk '{print $1}'`
		;;
	*)
		return 2
		;;
	esac

	return 0
}

start() {
	getpid $startup
	if [ -n "$pid" ]
	then
		echo ERROR: Appliction pid=$pid is still running
		return 1
	fi	
	
	$startup/startup.sh
	return 0
}

while true; do
	getpid $startup
	if [ -n "$pid" ]
	then
		sleep 1
	else
		start
		sleep 3
	fi
done
