#!/bin/bash

#2025-03-07
rm -rf lib/*.jar
rm -rf bin/*
rm -rf conf/*
mv modules/default/init/upgrade/startup.sh bin/
chmod ugo+x bin/startup.sh

cp modules/default/WEB-INF/lib/* lib/
rm modules/ddeps/WEB-INF/lib/commons-io-2.6.jar

rm -rf modules/default/init/upgrade/*
echo "JAVA_OPTS=\"-Xms10g -Xmx10g\"" > conf/jvm.options
