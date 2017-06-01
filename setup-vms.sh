#!/bin/bash

# Written by Evan Downing

myprint()
{
    title="Nvmtrace: VM setup"

    echo -e "\e[0;34m[$title]\e[0m \e[1;33m$1\e[0m"
}

endprint()
{
    echo -e "\e[0;32m[Done]\e[0m"
}

# Check for correct number of parameters
if [[ $# -ne 1 || $(id -u) -ne 0 ]]; then
    echo "usage: sudo ./setup_vms.sh number-of-vms"
    exit 1
fi

# Check to see if user inputted a number
number_re='^[0-9]+$'

if ! [[ $1 =~ $number_re ]]; then
    echo "Input was not a positive integer"
    exit 1
fi

# Check for maximum number of VMs (network interfaces) that this script can handle
if [[ $1 -lt 1 || $1 -gt 255 ]]; then
    echo "Input needs to be within the range: [1, 255]"
    exit 1
fi

myprint "Resetting config files..."

# Set up DHCP configuration file from template
dhcp='./etc/dhcpd.conf'
dhcp_old='./etc/dhcpd.conf-template'
cat $dhcp_old > $dhcp

# Remove and define nginx configuration file and folder
nginx='./etc/default'
rootfolder='/mnt/webroot'
rm $nginx

# Remove nvmtrace config file
cfg='./etc/nvmtrace.cfg'
rm $cfg

# Remove any previous workspace folders
rm -rf /opt/gtisc/nvmtrace/workspaces/*

# Remove any VM images
rm -rf /mnt/ramfs/*

endprint

# Create network interfaces and workspace folders for each VM
for ((i=0, j=1; i < $1; i++, j++))
do
    myprint "Setting up VM$i..."

    hex=$(printf '%02x' $(($j)))

    name=vm$i
    router_ip=10.0.$j.1
    vm_ip=10.0.$j.2
    mac=02:00:00:00:00:$hex

    # Remove previous folder that may exist
    rm -rf $rootfolder/$j*
    # Create folder to store dumped system data
    datafolder=$rootfolder/$j-dump/
    mkdir -p $datafolder

    # Delete network interface if it already exists
    ip link delete $name
    # Create network interface for VM
    tunctl -u root -t $name
    ip addr add $router_ip/24 dev $name
    ip link set dev $name up

    # Rate-limit network interface (10 Mbps downlink, 10 Mbps uplink)
    wondershaper $name 10000 10000

    # Modify DHCP configuration file to include this new host
    echo "" >> $dhcp
    echo "# Set up subnet for virutal machine" >> $dhcp
    echo "subnet 10.0.$j.0 netmask 255.255.255.0 {" >> $dhcp
    echo "    option routers $router_ip;" >> $dhcp
    echo "}" >> $dhcp
    echo "" >> $dhcp
    echo "# Set a fixed IP address for our virtual machine" >> $dhcp
    echo "host $name {" >> $dhcp
    echo "    hardware ethernet $mac;" >> $dhcp
    echo "    fixed-address $vm_ip;" >> $dhcp
    echo "}" >> $dhcp

    # Modify nginx configuration file
    echo "server {" >> $nginx
    echo "    listen   $router_ip:80;" >> $nginx
    echo "" >> $nginx
    echo "    root $rootfolder;" >> $nginx
    echo "    index index.html index.htm;" >> $nginx
    echo "" >> $nginx
    echo "    server_name localhost;" >> $nginx
    echo "" >> $nginx
    echo "    location / {" >> $nginx
    echo "        try_files \$uri \$uri/ /index.html;" >> $nginx
    echo "    }" >> $nginx
    echo "" >> $nginx
    echo "    # From https://coderwall.com/p/swgfvw/nginx-direct-file-upload-without-passing-them-through-backend" >> $nginx
    echo "    location /upload {" >> $nginx
    echo "        limit_except POST          { deny all; }" >> $nginx
    echo "" >> $nginx
    echo "        client_body_temp_path      $datafolder;" >> $nginx
    echo "        client_body_in_file_only   on;" >> $nginx
    echo "        client_body_buffer_size    128K;" >> $nginx
    echo "        client_max_body_size       1000M;" >> $nginx
    echo "" >> $nginx
    echo "        proxy_pass_request_headers on;" >> $nginx
    echo "        proxy_set_header           X-FILE \$request_body_file;" >> $nginx
    echo "        proxy_set_body             off;" >> $nginx
    echo "        proxy_redirect             off;" >> $nginx
    echo "        proxy_pass                 http://localhost;" >> $nginx
    echo "    }" >> $nginx
    echo "" >> $nginx
    echo "}" >> $nginx
    echo "" >> $nginx

    # Make a copy of the VM image to use
    cp /opt/gtisc/lib/nvmtrace.qcow3 /mnt/ramfs/nvmtrace$i\.qcow3

    # Create and configure nvmtrace workspace for VM
    ws=/opt/gtisc/nvmtrace/workspaces/$name/

    echo "$ws" >> $cfg    

    mkdir -p $ws
    echo "$vm_ip" > "$ws/ip"
    echo "$mac" > "$ws/mac"
    echo "$name" > "$ws/name"
    echo "/mnt/ramfs/nvmtrace${i}.qcow3" > "$ws/disk"
    echo "/mnt/webroot/$j" > "$ws/exec"
    echo "qemu-system-x86_64" > "$ws/vm"

    endprint
done

myprint "Copying new config files and resetting services..."

# Copy DHCP configuration file to permanent place
cp $dhcp /etc/dhcp/

# Restart DHCP server
/etc/init.d/isc-dhcp-server restart

# Copy nginx configuration file to permanent place
cp $nginx /etc/nginx/sites-enabled/

# Restart nginx server
/etc/init.d/nginx restart

# Copy nvmtrace configuration file to permanent place
cp $cfg /opt/gtisc/etc/

endprint
