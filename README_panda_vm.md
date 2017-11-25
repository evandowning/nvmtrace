# Setting up a PANDA VM image for nvmtrace-modified

This is a list of step-by-step instructions to create an image to
run malware on.

## Dependencies
Debian Linux

## Download ISO files:
  1. Download Windows 7 32-bit ISO

## Create the image file:
```
$ sudo ./panda/build/qemu-img create -f qcow2 -o compat=1.1 nvmtrace.qcow3 40G
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
      - Windows 7:
        ```
        $ sudo ./panda/build/x86_64-softmmu/qemu-system-x86_64 qemu-system-x86_64 \
            -cpu Westmere \
            -smp 4 \
            -m 4G \
            -vga cirrus \
            -hda nvmtrace.qcow3 \
            -cdrom win7.iso \
            -net none \
            -usbdevice tablet \
            -vnc :0
        ```

  3. VNC into 5900
```
# For example, we can perform port forwarding to a computer that does have
# graphics.

$ ssh -TNL 5900:localhost:5900 malware-server

# Then open up your favorite VNC program and view localhost:5900
```

## Install Windows:
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

## Configure Windows:
  1. Start your virtual machine

      - Windows 7:
        ```
        $ sudo qemu-system-x86_64 \
            -cpu Westmere \
            -smp 1 \
            -m 1.5G \
            -vga cirrus \
            -hda nvmtrace.qcow3 \
            -net nic,macaddr=02:00:00:00:00:01,model=rtl8139 \
            -net tap,script=no,ifname=vm0 \
            -usbdevice tablet \
            -vnc :0 \
            -monitor stdio
        ```

  1. Install GnuWin32 project tools: http://gnuwin32.sourceforge.net/
      - On server, download grep, mawk, unzip, and wget
      - Put these in /mnt/webroot and download them to VM
        ```
        $ sudo cp *.exe /mnt/webroot
        $ sudo chmod 666 /mnt/webroot/*.exe
        ```
      - On VM, navigate to "http://10.0.1.1/grep.exe" in Internet Explorer.
      - Run and install package (binary only). Repeat for mawk and wget

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

  1. Reboot VM (in Windows GUI)

  1. After Windows has booted, immediately take a snapshot (before our script runs)
     - (qemu) stop
     - (qemu) savevm ready
     - (qemu) quit

## Move final image:
```
$ mv nvmtrace.qcow3 ./etc/nvmtrace.qcow3

# If you ever want to update this image in the future, also do the following
# before running setup-vms.sh and restarting nvmtrace:
$ sudo cp ./etc/nvmtrace.qcow3 /opt/gtisc/lib/nvmtrace.qcow3

# Config.sh will take care of this for us in the beginning, but after we've run
# it (initially) there's no need to rerun the entire script (i.e., config.sh)
# again.
```

## Tear down things:
```
$ sudo ip link delete vm0
```
