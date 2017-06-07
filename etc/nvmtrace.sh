#!/bin/bash

if [[ $(id -u) -ne 0 ]]; then
    echo nvmtrace must be run as root
    exit 1
fi;

NVMTRACE=/opt/gtisc/lib/java/nvmtrace.jar
CONFIG=/opt/gtisc/etc/nvmtrace.cfg
LOG=/var/log/nvmtrace.log

nohup java -Xmx3G -jar $NVMTRACE $CONFIG > $LOG 2>&1 &
