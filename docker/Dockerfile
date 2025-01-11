FROM ubuntu:22.04

ENV LANG=C.UTF-8
ENV DEBIAN_FRONTEND=noninteractive
ENV TZ=Asia/Shanghai

COPY ./files /

RUN apt update -y \
&& apt install -y openjdk-17-jre-headless python3 lsof python3-pip systemd vim \
&& pip3 install psutil requests 

VOLUME /data
VOLUME /home/disk1

EXPOSE 8080

ENTRYPOINT ["/home/start.sh"]
