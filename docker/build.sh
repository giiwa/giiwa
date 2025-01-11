#!/bin/bash

cd files
chmod ugo+x home/start.sh home/giiwa/giiwa home/giiwa/bin/* etc/init.d/*
find . -name ".DS_Store" |xargs rm

cd ..
docker buildx build -t giiwa:x86 .
docker save giiwa:x86 | gzip > giiwa_x86.tgz
docker image rm giiwa:x86
docker buildx build --platform=linux/arm64 -t giiwa:arm64 .
docker save giiwa:arm64 | gzip > giiwa_arm64.tgz
docker image rm giiwa:arm64
