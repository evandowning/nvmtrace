package edu.gatech.nvmtrace;

import java.io.*;
import java.text.*;
import java.util.*;

import edu.gatech.util.*;

public class NVMCThread extends Thread
{
    // How much time (in seconds) to run each executable for
    public static final int nvmRunTime = 120;

    // How much time (in seconds) to pause to check for new sha256 in DB if none exists
    public static final int pollInterval = 16;

    // String to hold path of virtual machine configuration files
    private String basePath;

    private BufferedWriter log;
    private NVMThreadDB dbase;
    private NVMConfig config;

    // Constructor for NVMCThread
    public NVMCThread(String basePath)
    {
        // Add a '/' after the directory path if one does not exist
        if (!basePath.endsWith("/"))
        {
            this.basePath = basePath + "/";
        }
        else
        {
            this.basePath = basePath;
        }

        // Construct log file for the virtual machine
        try
        {
            final String logPath =  this.basePath + "log.txt";
            this.log = new BufferedWriter(new FileWriter(logPath, true));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }

        /*
            Give the ability to access database for determining which malware
            to run next and update the database accordingly.
        */
        this.dbase = new NVMThreadDB();

        /*
            Give the ability to access configuration of nvmtrace
            files and folders.
        */
        this.config = new NVMConfig(this.dbase);
    }

    // Function to retrieve the MAC address of the virtual machine
    public String getMacAddr()
    {
        return ExecCommand.cat(this.basePath + "mac")[0];
    }

    // Function to retrieve the IP address of the virtual machine
    public String getNetworkAddr()
    {
        return ExecCommand.cat(this.basePath + "ip")[0];
    }

    // Function to retrieve the location of the virtual machine image
    public String getNVMDiskPath()
    {
        return ExecCommand.cat(this.basePath + "disk")[0];
    }

    /*
        Function to retrieve the location of the executable for the virtual
        machine to run.
    */
    public String getNVMExecPath()
    {
        return ExecCommand.cat(this.basePath + "exec")[0];
    }

    // Function to retrieve the location of the virtual machine executable.
    public String getVMPath()
    {
        return ExecCommand.cat(this.basePath + "vm")[0];
    }

    // Function to retrieve the name of the virtual machine
    public String getWorkspaceName()
    {
        return ExecCommand.cat(this.basePath + "name")[0];
    }

    // Function to write to nvmtrace log file
    private void logWrite(String message)
    {
        final String logTimeFormat = "yyyyMMdd HH:mm:ss";

        String logTime = 
            new SimpleDateFormat(logTimeFormat).format(new Date());

        try
        {
            this.log.write(logTime + " | " + message + "\n");
            this.log.flush();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    // Function to start virtual machine instance and tcpdump session
    public Vector<Process> startNVMSession(String sha256)
    {
        Vector<Process> nvmSession = new Vector<Process>();

        /*
            Copy sha256 executable to folder where virtual macine
            will retrieve it from.
        */
        ExecCommand.cp(this.config.getInputPath() + sha256,
                       this.getNVMExecPath());

        // Make sure executable has proper permissions
        ExecCommand.chmod("666", this.getNVMExecPath());

        /*
            Run tcpdump on designated network interface to record 
            network activity coming out of virtual machine.
        */
        Process tcpdump = 
            ExecCommand.tcpdump(this.getWorkspaceName(),
                                this.config.getPcapPath(sha256),
                                this.config.
                                    getTCPDumpExpression(this.getMacAddr()));

        // Run virtual machine
        Process qemu =
            ExecCommand.qemu(this.getVMPath(),
                             this.getNVMDiskPath(),
                             this.getMacAddr(),
                             this.getWorkspaceName());

        // Add processes to vector so they can be manaaged by stopNVMSession()
        // These processes will be killed in the order they're inserted
        nvmSession.add(qemu);
        nvmSession.add(tcpdump);

        return nvmSession;
    }

    // Function to stop processes (virtual machines and tcpdump's)
    public void stopNVMSession(String sha256, Vector<Process> nvmSession)
    {
        // Kill each process
        for (int i = 0; i < nvmSession.size(); i++)
        {
            nvmSession.elementAt(i).destroy();

            try
            {
                nvmSession.elementAt(i).waitFor();
            }
            catch (Exception e)
            {
            }
        }

        // Bug in cntrk
        // Get rid of excess states for MAC address
        ExecCommand.cntrkrm(this.getNetworkAddr());
    }

    // Function to run thread
    public void run()
    {
        while (true)
        {
            // Retrieve the next malware sample
            String sha256 = this.dbase.getNextSamplesha256();

            /*
                If no sample is retrieved, wait some amount
                of time and try again.
            */
            if (sha256 == null)
            {
                ExecCommand.sleep(NVMCThread.pollInterval);
                continue;
            }

            // Create output folder for malware's data
            ExecCommand.mkdir(this.config.getOutputPath(sha256));

            this.logWrite("starting nvm for " + sha256); 

            // TODO: make it so you don't have to do this every time
            // For resetting TAP interface
            String ip = this.getNetworkAddr();
            char[] ipArray = ip.toCharArray();
            ipArray[ip.length()-1] = '1';
            ip = String.valueOf(ipArray);
            String cidr = ip + "/24";

            // Reset TAP interface
            ExecCommand.resetTAP(cidr,this.getWorkspaceName());

            // Rate-limit connection again
            ExecCommand.enableRL(this.getWorkspaceName());

            // Start virtual machine, tcpdump session, and run malware
            Vector<Process> vmSession = this.startNVMSession(sha256);

            // Remove malware sample from input folder
            ExecCommand.rm(this.config.getInputPath() + sha256);

            // Sleep a fixed amount of time to give malware a chance to run
            ExecCommand.sleep(NVMCThread.nvmRunTime);

            // Wait until the analyzer starts uploading files
            int count = 0;
            while (ExecCommand.uploadStarted(this.getNVMExecPath() + "-dump/*") == 0)
            {
                ExecCommand.sleep(5);
                count += 5;

                // If we've waited long enough for the sample to finish, just quit
                if (count > 300)
                {
                    break;
                }
            }

            // Disable rate-limiting temporarily so the analyzer can upload its results quickly
            ExecCommand.disableRL(this.getWorkspaceName());

            // Let analyzer upload results to server
            ExecCommand.sleep(15);

            // Stop virtual machine and tcpdump capture
            this.stopNVMSession(sha256, vmSession);

            // Reset snapshot for backing file
            ExecCommand.removeSnapshot(this.getNVMDiskPath());
            ExecCommand.resetSnapshot(this.getNVMDiskPath());

            // Move system logs to workspace
            ExecCommand.mkdir(this.config.getSystemDumpPath(sha256));
            ExecCommand.shellmv(this.getNVMExecPath() + "-dump/*", this.config.getSystemDumpPath(sha256) + "/");

            // Reset TAP interface
            ExecCommand.resetTAP(cidr,this.getWorkspaceName());

            // Rate-limit connection again
            ExecCommand.enableRL(this.getWorkspaceName());

            this.logWrite("stopped nvm for " + sha256);
        }
    }
}
