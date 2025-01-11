#!/bin/bash

mkdir /data/logs

if [ -f "/data/etc/hosts" ]; then
  cat /data/etc/hosts >> /etc/hosts
fi

/bin/cp -rf /home/giiwa/lib/sigar/* /lib/

sysctl -p

/etc/init.d/appdog start

/bin/bash

