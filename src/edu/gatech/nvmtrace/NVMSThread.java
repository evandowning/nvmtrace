package edu.gatech.nvmtrace;

import java.util.*;

public class NVMSThread extends Thread
{
    private Vector<NVMCThread> nvmCThreads;

    // Declare vector to hold threads
    public NVMSThread()
    {
	    this.nvmCThreads = new Vector<NVMCThread>();
    }

    // Function to add threads to vector
    public void addNVMCThread(NVMCThread nvmCThread)
    {
	    this.nvmCThreads.add(nvmCThread);
    }

    // Function to run each thread
    public void run()
    {
        // Run each thread
        try
        {
            for (int i = 0; i < this.nvmCThreads.size(); i++)
            {
                this.nvmCThreads.elementAt(i).join();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
