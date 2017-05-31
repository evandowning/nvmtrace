#!/bin/bash

sudo iptables-restore < ./etc/iptables.rules

sleep 15

sudo iptables -F
sudo iptables -t nat -F
