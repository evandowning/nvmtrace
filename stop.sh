#!/bin/bash

sudo killall qemu-system-x86_64 > /dev/null 2>&1
sudo killall tcpdump > /dev/null 2>&1
pgrep -f java.*nvmtrace | awk 'BEGIN{ORS=" "} 1' | sudo xargs kill -9
