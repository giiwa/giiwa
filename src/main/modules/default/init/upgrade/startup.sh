#!/bin/bash

ulimit -HSn 65536 > /dev/null 2>&1
ulimit -n 10240 >/dev/null 2>&1

source /etc/profile

if [ ! -d "/data" ]; then
	mkdir /data
fi
if [ ! -d "/data/logs" ]; then
	mkdir /data/logs
fi
if [ ! -d "/data/temp" ]; then
	mkdir /data/temp
fi

PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

PRGDIR=`dirname "$PRG"`

export GIIWA_HOME=`cd "$PRGDIR"/../ >/dev/null; pwd`

if [ -f "$GIIWA_HOME/conf/jvm.options" ]; then
	source $GIIWA_HOME/conf/jvm.options
fi

CLASSPATH=

for file in $(ls $GIIWA_HOME/lib/*.jar); 
do 
  CLASSPATH="$CLASSPATH":"$file" 
done

GIIWA_TMPDIR=/data/temp

export CLASSPATH

JAVA_OPTS="$JAVA_OPTS -Dplatform.dependencies -Duser.timezone=Asia/Shanghai -Djava.awt.headless=true -XX:+UseG1GC -XX:ParallelGCThreads=20 -XX:G1HeapRegionSize=32 -XX:-OmitStackTraceInFastThrow -XX:OnOutOfMemoryError=kill -Dio.netty.noUnsafe=true -Dio.netty.noKeySetOptimization=true -Dio.netty.recycler.maxCapacityPerThread=0 -Dio.netty.allocator.type=pooled -Xlog:gc*,gc+age=trace,safepoint:file=/data/logs/gc.log:utctime,pid,tags:filecount=10,filesize=64m -Djava.security.egd=file:/dev/urandom -Djava.library.path=$GIIWA_HOME/lib/sigar"

echo "`java -version`"
echo "Using GIIWA_HOME:      $GIIWA_HOME"
echo "Using GIIWA_TMPDIR:    $GIIWA_TMPDIR"
echo "Using JRE_HOME:        $JRE_HOME"
#echo "Using CLASSPATH:       $CLASSPATH"

java $JAVA_OPTS \
  -Dgiiwa.home="$GIIWA_HOME" \
  -Djava.io.tmpdir="$GIIWA_TMPDIR" \
  org.giiwa.server.Server &
