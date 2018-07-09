# Setting up a VM image for nvmtrace-modified

This is a list of step-by-step instructions to create an image to
run malware on.

Main idea: The VM needs to be able to run quickly in order to scale.

## Dependencies

Debian Linux

## Download ISO files:
  1. Download virtio drivers for Windows
      - Reference Website: (https://fedoraproject.org/wiki/Windows_Virtio_Drivers
      - Software built from:  https://github.com/crobinso/virtio-win-pkg-scripts
      - `$ wget https://fedorapeople.org/groups/virt/virtio-win/direct-downloads/stable-virtio/virtio-win.iso`

  1. Download Windows ISO
      - If Windows XP SP3
        - Save the ISO as "winxpsp3.iso"
      - If Windows 7 (we recommend using 64-bit)
        - Save the ISO as "win7.iso"

## Create the image file:
```
$ sudo qemu-img create -f qcow2 -o compat=1.1 nvmtrace.qcow3 40G
```

## Set up TAP interface:

We need to create a tap device that will act as the virtual machine's network
interface.

```
$ sudo tunctl -u root -t vm0
$ sudo ip addr add 10.0.1.1/24 dev vm0
$ sudo ip link set dev vm0 up
```

## Set up DHCP server
```
$ sudo apt-get install isc-dhcp-server
$ sudo cp ./etc/dhcpd.conf /etc/dhcp/
$ sudo /etc/init.d/isc-dhcp-server restart
```

## Set up nginx server
```
$ sudo apt-get install nginx
$ sudo mkdir /mnt/webroot
$ sudo cp ./etc/default /etc/nginx/sites-enabled/
$ /etc/init.d/nginx restart
```

## Start Virtual Machine:

  1. Since I'm assuming a headless server (no graphics), I instruct this using VNC

  2. Start your virtual machine

      - Windows XP SP3:
        ```
        $ sudo qemu-system-x86_64 \
            -enable-kvm \
            -cpu host \
            -smp 2 \
            -m 4G \
            -vga std \
            -hda nvmtrace.qcow3 \
            -cdrom winxpsp3.iso \
            -net none \
            -usbdevice tablet \
            -vnc localhost:0
        ```

      - Windows 7:
        ```
        $ sudo qemu-system-x86_64 \
            -enable-kvm \
            -cpu host \
            -smp 2 \
            -m 4G \
            -vga cirrus \
            -hda nvmtrace.qcow3 \
            -cdrom win7.iso \
            -net none \
            -usbdevice tablet \
            -vnc localhost:0
        ```

  3. VNC into 5900
```
# For example, we can perform port forwarding to a computer that does have
# graphics.

$ ssh -TNL 5900:localhost:5900 malware-server

# Then open up your favorite VNC program and view localhost:5900
```

## Install Windows:

  - If installing Windows XP SP3:
    1. View the Windows GUI through your favorite VNC interface (hooking into port 5900 of your local machine)

    1. Install Windows normally
        - do NOT "Help protect your PC" when this dialog shows
        - skip the network setup section
        - do NOT register Windows

    1. Enable Administrator account only
        - Control Panel -> Administrative Tools -> Computer Management
        - Local Users and Groups -> Double-click Administrator
        - Uncheck "Account is disabled"
        - Apply
        - Log off user's account and log into Administrator
        - Delete other user account

    1. Disable Security features
        - Control Panel -> Administrative Tools -> Services
          - Right-click Automatic Updates -> Properties
            - Click Stop
            - Click Startup type -> Disabled
            - Click Apply
          - Right-click Security Center -> Properties
            - Click Stop
            - Click Startup type -> Disabled
          - Right-click Windows Firewall/Internet Connection Sharing -> Properties
            - Click Stop
            - Click Startup type -> Disabled

  - If installing Windows 7:
    1. View the Windows GUI through your favorite VNC interface (hooking into port 5900 of your local machine)

    1. Install Windows normally
        - When "Help protect your PC" comes up, select "Ask me later"
        - Disable User Access Control

    1. Enable Administrator account only
        - Control Panel -> Administrative Tools -> Computer Management
        - Local Users and Groups -> Double-click Administrator
        - Uncheck "Account is disabled"
        - Apply
        - Log off user's account and log into Administrator
        - Delete other user account

    1. Disable Security features
        - Control Panel -> Administrative Tools -> Services
          - Right-click Windows Update -> Properties
            - Click Stop
            - Click Startup type -> Disabled
            - Click Apply
          - Right-click Security Center -> Properties
            - Click Stop
            - Click Startup type -> Disabled
          - Right-click Windows Firewall -> Properties
            - Click Stop
            - Click Startup type -> Disabled

  - Shutdown Windows normally (through GUI).

## Install Hardware Extensions:
  1. Start your virtual machine

      - Windows XP SP3:
        ```
        $ sudo qemu-system-x86_64 \
            -enable-kvm \
            -cpu host \
            -smp 2 \
            -m 256M \
            -balloon virtio \
            -vga std \
            -hda nvmtrace.qcow3 \
            -cdrom virtio-win.iso \
            -net nic,macaddr=02:00:00:00:00:01,model=virtio \
            -net tap,script=no,ifname=vm0 \
            -usbdevice tablet \
            -vnc localhost:0
        ```

      - Windows 7:
        ```
        $ sudo qemu-system-x86_64 \
            -enable-kvm \
            -cpu host \
            -smp 2 \
            -m 1.5G \
            -balloon virtio \
            -vga cirrus \
            -hda nvmtrace.qcow3 \
            -cdrom virtio-win.iso \
            -net nic,macaddr=02:00:00:00:00:01,model=virtio \
            -net tap,script=no,ifname=vm0 \
            -usbdevice tablet \
            -vnc localhost:0
        ```

  1. Click: Control Panel -> Hardware -> Device Manager
      - Double click the "Ethernet Controller" question mark in "other devices"
        - If it asks what network location you'd like to select, click "Cancel"
      - Double click the "PCI device" question mark in "other devices"
      - On Windows XP: if these devices don't exist, click Action -> Scan for hardware changes

  1. Select to browse computer to find software under D:\ drive (virtio-win.iso)
      - Let it install the drivers

  1. Repeat until all hardware drivers have been installed

  1. If the Internet doesn't work, try checking the status of the vm0 interface
    (us `$ ip a` to see if it has an IP address associated with it). Also renewing
    ipconfig within windows (use `PROMPT> ipconfig /renew`) may work.
      - Most of the time when I encounter this, running `$ sudo ip addr add 10.0.1.1/24 dev vm0`
        on the server makes everything work again.
      - This may happen repeatedly as you start and shutdown the Windows VMs. It's
        because we're trying to use virtio as a network interface. There's probably
        a fix for it, but I'm too lazy to figure it out right now.

  1. Shutdown Windows through Windows GUI

  1. At the end of this, the following drivers will have been installed:
      - Ethernet Controller
      - Virtio memory balloon (RAM)

## Configure Windows:
  1. Start your virtual machine

      - Windows XP SP3:
        ```
        $ sudo qemu-system-x86_64 \
            -enable-kvm \
            -cpu host \
            -smp 2 \
            -m 256M \
            -balloon virtio \
            -vga std \
            -hda nvmtrace.qcow3 \
            -net nic,macaddr=02:00:00:00:00:01,model=virtio \
            -net tap,script=no,ifname=vm0 \
            -usbdevice tablet \
            -vnc localhost:0
        ```

      - Windows 7:
        ```
        $ sudo qemu-system-x86_64 \
            -enable-kvm \
            -cpu host \
            -smp 2 \
            -m 1.5G \
            -balloon virtio \
            -vga cirrus \
            -hda nvmtrace.qcow3 \
            -net nic,macaddr=02:00:00:00:00:01,model=virtio \
            -net tap,script=no,ifname=vm0 \
            -usbdevice tablet \
            -vnc localhost:0
        ```

  1. This is the part where you install whatever system-side analysis components
     you want. This could be Cuckoo (http://www.cuckoosandbox.org/) or my modified
     version of Cuckoo (https://github.com/evandowning/cuckoo-headless). I'll
     provide instruction for running my modified version of Cuckoo. Cuckoo
     requires a different setup entirely, as it doesn't (at the time of this
     writing) support DHCP. I recommend setting up the VMs using virsh if you're
     using the normal Cuckoo.

  1. Install GnuWin32 project tools: http://gnuwin32.sourceforge.net/
      - On server, download grep, mawk, unzip, and wget
      - Put these in /mnt/webroot and download them to VM
        ```
        $ sudo cp *.exe /mnt/webroot
        $ sudo chmod 666 /mnt/webroot/*.exe
        ```
      - On VM, navigate to "http://10.0.1.1/grep.exe" in Internet Explorer.
      - Run and install package (binary only). Repeat for mawk and wget

  1. Set up modified Cuckoo
      - Set Internet Explorer to connect to https websites:
        - Start up Internet Explorer
        - Click Tools -> Internet Options -> Advanced -> Use TLS 1.0
        - Click Apply
      - Install Python 2.7: https://www.python.org/downloads
        - Download the MSI installer
      - Install PIL (for taking screenshots): http://www.pythonware.com/products/pil/
      - Install requests (use pip to install: C:\Python27\Scripts\pip.exe)
      - On server, download modified Cuckoo (https://github.com/evandowning/cuckoo-headless)
      - On server, download modified Cuckoo monitor (https://github.com/evandowning/monitor)
      - On server, compile monitor and put resulting bin/ folder into analyzer/ folder
        in cuckoo-headless directory.
      - Zip compress this folder (analyzer/) to analyzer.zip
      - Put this into /mnt/webroot as well (don't forget to chmod 666).

  1. Configure to retrieve samples and analyzer from nginx server
      - On server, copy ./etc/load.bat to /mnt/webroot:
        ```
        $ sudo cp ./etc/load.bat /mnt/webroot
        $ sudo chmod 666 /mnt/webroot
        ```
      - On VM, navigate to "http://10.0.1.1/load.bat" in Internet Explorer.
      - Save it to Desktop
      - Create a shortcut of "load.bat" (Right-click load.bat –> Create Shortcut)
      - Start –> All Programs –> Right-click Startup –> Open
      - Drag-and-drop this shortcut into the Startup folder. Delete the shortcut remaining on the Desktop
      - Right-click "load.bat" -> Properties and click "Unblock" and Apply to allow this file to run without prompting.
      - Clear IE history, temporary files (including offline content), and cookies
        - Tools -> Internet Options

  1. Shutdown Windows through the GUI.

## Move final image:
```
$ mv nvmtrace.qcow3 ./etc/nvmtrace.qcow3

# If you ever want to update this image in the future, also do the following
# before running setup-vms.sh and restarting nvmtrace:
$ sudo cp ./etc/nvmtrace.qcow3 /opt/gtisc/lib/nvmtrace.qcow3

# Config.sh will take care of this for us in the beginning, but after we've run
# it (initially) there's no need to rerun the entire script again.
```

## Tear down things:
```
$ sudo ip link delete vm0
```
