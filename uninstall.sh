#!/bin/bash

# Written by Evan Downing

# Check for correct number of parameters
if [[ $# -ne 0 || $(id -u) -ne 0 ]]; then
    echo "usage: sudo ./uninstall.sh"
    exit 1
fi

dropdb nvmtrace

rm -rf /opt/gtisc
rm -rf /mnt/ramfs
rm -rf /mnt/webroot

for ((i=0; i < 255; i++))
do
    # Remove network interface for VM
    ip link delete vm$i
done
