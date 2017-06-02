#!/bin/bash

# Written by Evan Downing

myprint()
{
    title="Nvmtrace-modified setup"

    echo -e "\e[0;34m[$title]\e[0m \e[1;33m$1\e[0m"
}

endprint()
{
    echo -e "\e[0;32m[Done]\e[0m"
}

# Check for correct number of parameters
if [[ $# -ne 0 ]]; then
    echo "usage: ./config.sh"
    exit 1
fi

# Install packages
myprint "Installing dependencies..."
sudo apt-get -y install uml-utilities
sudo apt-get -y install ant
sudo apt-get -y install psmisc
sudo apt-get -y install tcpdump
sudo apt-get -y install conntrack
sudo apt-get -y install openjdk-7-jdk
sudo apt-get -y install ntfs-3g
sudo apt-get -y install isc-dhcp-server
sudo apt-get -y install postgresql
sudo apt-get -y install nginx
sudo apt-get -y install wondershaper
endprint

# Create necessary directories
myprint "Making directories..."
sudo mkdir -p /opt/gtisc
sudo mkdir -p /opt/gtisc/etc
sudo mkdir -p /opt/gtisc/bin
sudo mkdir -p /opt/gtisc/lib
sudo mkdir -p /opt/gtisc/lib/java
sudo mkdir -p /opt/gtisc/nvmtrace
sudo mkdir -p /opt/gtisc/nvmtrace/input
sudo mkdir -p /opt/gtisc/nvmtrace/output
sudo mkdir -p /opt/gtisc/nvmtrace/workspaces
sudo mkdir -p /mnt/ramfs
sudo mkdir -p /mnt/webroot
endprint

# Copy files into directories
myprint "Copying files..."
sudo cp ./etc/nvmtrace.qcow3 /opt/gtisc/lib/
sudo cp ./etc/nvmtrace.sh /opt/gtisc/bin/

# Modify fstab file
sudo bash -c "cat ./etc/fstab >> /etc/fstab"
endprint

# Forward IP connection
myprint "Forwarding IP connection..."
sudo sysctl -w net.ipv4.ip_forward=1
sudo sysctl -p
endprint

# Install firewall
myprint "Installing firewall..."
sudo cp ./etc/iptables.rules /etc/
sudo sh -c "iptables-restore < /etc/iptables.rules"
sudo sh -c "echo '#!/bin/sh\n/sbin/iptables-restore < /etc/iptables.rules' > /etc/network/if-pre-up.d/iptables"
sudo chmod +x /etc/network/if-pre-up.d/iptables
endprint

# Create postgresql role
myprint "Creating postgresql role"
user=`whoami`
sudo -u postgres bash -c "createuser -a $user"
createdb nvmtrace
psql -d nvmtrace -f ./etc/nvmtrace.sql
endprint
