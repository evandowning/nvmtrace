# nvmtrace

This is a modified version of the original [nvmtrace](https://code.google.com/p/nvmtrace/)
and updated [nvmtrace](https://github.com/adamwallred/nvmtrace)

Instead of managing multiple bare-metal machines, this version manages
multiple virtual machines. It is light-weight (i.e., has minimal overhead) which
makes it appealing for large-scale executions of malware.

Also I have gone through the trouble of documenting each source file as well
as include instructions on how to modify it for your particular interests.

## Requirements

Debian 8 Linux (Jessie) and Java 7

## Setup

  1. Copy the iptables template file
     ```
     $ cp ./etc/iptables.rules.template ./etc/iptables.rules
     ```

  1. Modify ./etc/iptables.rules for your particular computer's setup. Replay all
     places between brackets "< >" with appropriate settings. Hint: you can
     use the ./test-iptables.sh script to test to see if your iptables rules work.
     First, start tmux (or an equivalent persistent window manager) and then
     run the script. If your iptables rules are faulty and you get kicked out
     of your machine, the script is designed to flush the iptables rules after
     15 seconds. So if you mess it up you don't have to worry about getting locked
     out of your own machine. Using tmux (or an equivalent) allows the script
     to continue running if you're kicked from your own server.

  1. Modify ./src/edu/gatech/nvmtrace/NVMThreadDB.java to replace "dbUser" and "dbPass"
     accordingly. The configuration script will automatically create your username
     below, but you may modify it to change your username and password.

  1. Set up base VM image. See [README\_vm.md](README_vm.md)

  1. Run configuration
     ```
     $ ./config.sh
     ```

  1. Run setup-vms.sh
     ```
     $ sudo ./setup-vms.sh
     ```

## Installing

If you already have Java installed, make sure you switch to using Java 7 to compile and run:

```
$ sudo update-alternatives --config java
```

Now build and move nvmtrace:
```
$ ant 
$ sudo cp ./dist/nvmtrace.jar /opt/gtisc/lib/java/
$ ant clean
```

## Comments

Whilst most of what I'm about to document is shown within the contents of
./etc/ and config.sh, here I explain a bit about what is happening to your
server as we set it up.

Firewall rules and rate-limiting are used to protect users across the Internet.
Redirecting spam traffic means we don't spam others, rate-limiting means we
don't DDoS others, and preventing maliciously-used ports means we don't spread
the malware to the best of our ability.

The firewall rules are placed in /etc/iptables.rules and will be reloaded every
time the computer restarts. Rate limiting is turned on and will not be
persistent through computer restarts. Simply execute the same wondershaper command
in config.sh after a computer restart.

The database that is used by nvmtrace is a postgresql database called "nvmtrace".

The folders /opt/gtisc, /mnt/ramfs, and /mnt/webroot are used by nvmtrace internally
to keep track of samples run, host VM images in RAM, and supply the malware
samples to each VM to be run.

Each VM's IP address is 10.0.x.2 and is associated with a DHCP server (10.0.x.0)
and a router (10.0.x.1).

I use nginx to be able to deliver each malware sample to the VM to be run and to
upload the system-side data gathered.

## Running nvmtrace
```
$ sudo /opt/gtisc/bin/nvmtrace.sh
```

## Running malware samples
When new malware is found and added to your folder composed solely of malware
samples, re-run the following command. Nvmtrace will automatically grab the
new executables and run them.

To run all samples within folder (even if they've been run before):
```
$ ./etc/load.sh malware-folder
```

To run only new (unseen) samples:
```
$ ./etc/load-new.sh malware-folder
```

## Stopping nvmtrace
```
$ ./stop.sh
```

## Source code navigation

./src/edu/gatech/nvmtrace/NVMController.java:
  - Contains main function
  - Creates threads for each virtual machine (CThread, i.e. Client Thread)
  - Initializes manager for threads (SThread, i.e. Server Thread)
  - Starts each CThread

./src/edu/gatech/nvmtrace/NVMConfig.java:
  - Contains functions specifying where input and output files will be
    found using nvmtrace.
  - Contains functions specific to nvmtrace's usage.

./src/edu/gatech/nvmtrace/NVMSThread.java:
  - Manages multiple CThreads.
    
./src/edu/gatech/nvmtrace/NVMCThread.java:
  - Runs a virtual machine and gathers a tcpdump of the session.

./src/edu/gatech/nvmtrace/NVMThreadDB.java:
  - Manages database that determines what executables nvmtrace needs
    to run and which executables nvmtrace has already run.

./src/edu/gatech/util/ExecCommand.java:
  - Contains functions to execute command-line instructions

## Modifying source code

The descriptions of each source code file are pretty straight-forward.

If you'd like to modify how nvmtrace executes malware and gathers data, you're
going to be modifying NVMCThread.java and ExecCommand.java.

If you'd like to modify how it manages running each piece of malware and how
it creates entries in the database, you're going to be modifying NVMThreadDB.java
and ./etc/nvmtrace.sql.

If you'd like to modify where nvmtrace stores its outputted data files or
where it retrieves its input executables, you're going to be modifying
NVMConfig.java.

Everything else nvmtrace does is solely to handle running multiple threads
at once. You should never have to modify NVMSThread.java or NVMController.java.

If you want to modify the nvmtrace image (qcow3), just modify the base image in
/mnt/ramfs/nvmtrace.qcow3 and rerun setup-vms.sh
If you want to add more (or subtract) VMs, use setup-vms.sh.
NOTE: If you rerun setup-vms.sh, it will erase ALL of your workspace (other VM
setups in /opt/gtisc/nvmtrace/workspaces/\*. It will also rewrite the DHCP
and NGINX configuration files as well.

If you want to modify the iptables rules or database (nvmtrace), do so manually
and follow config.sh's workflow for propagating your changes. DO NOT rerun config.sh.

## File Organization

/opt/gtisc/nvmtrace/input
  * Stores samples to be copied to /mnt/webroot to be run in a VM
  * After this, the sample will be removed from this folder
  * NOTE: this is so you can store you malware samples elsewhere and nvmtrace only stores the resulting analysis files

/opt/gtisc/nvmtrace/output
  * Stores output (result) from running each malware sample (located in /opt/gtisc/nvmtrace/input/)
  * Each sample's folder is represented as their sha256 value.
  * The next folder down is the Unix time at which it was executed/run (the difference in seconds between now and January 1, 1970).

/opt/gtisc/nvmtrace/workspaces/
  * Stores the details of each VM
    * disk: path to VM image (disk)
    * exec: path to malware
    * ip: IP address of VM
    * log.txt: log file for VM
    * mac: MAC address of VM
    * name: name of VM
    * vm: path to virtual machine

/mnt/webroot
  * Stores malware to be run
  * Stores system data uploaded from each VM (numbers corresponding to their VM name)

/var/log/nvmtrace.log
  * Log file for nvmtrace

## Uninstalling

Well this is all very nice, but I'm tired of using this system and would like
to remove it from my server.

```
$ sudo ./uninstall.sh
```
