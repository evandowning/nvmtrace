package edu.gatech.nvmtrace;

import java.io.*;

import edu.gatech.util.*;

public class NVMController
{
    // Function for printing out usage information
    public static void usage()
    {
        System.out.println("Usage: nvmtrace.jar wspacefile");
        System.exit(-1);
    }

    // Main function for NVMTrace
    public static void main(String[] args)
    {
        if (args.length == 0 || !new File(args[0]).exists())
        {
            NVMController.usage();
        }

        // Get workspace paths
        String[] workspacePaths = ExecCommand.cat(args[0]);

        // Construct threads for each workspace
        NVMCThread[] nvmCThreads = new NVMCThread[workspacePaths.length];
        for (int i = 0; i < workspacePaths.length; i++)
        {
            nvmCThreads[i] = new NVMCThread(workspacePaths[i]);
        }

        // Create thread handler
        NVMSThread nvmSThread = new NVMSThread();
        for (int i = 0; i < nvmCThreads.length; i++)
        {
            nvmSThread.addNVMCThread(nvmCThreads[i]);
        }

        // Register new VM shutdown hook
        Runtime.getRuntime().addShutdownHook(nvmSThread);

        // Run threads for each workspace
        for (int i = 0; i < nvmCThreads.length; i++)
        {
            nvmCThreads[i].start();
        }
    }
}
