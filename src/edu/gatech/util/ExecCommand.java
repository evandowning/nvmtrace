package edu.gatech.util;

import java.util.*;
import java.io.*;

public class ExecCommand
{
    public static Process execCommand(String[] command)
    {
        Process proc = null;

        try
        {
            proc = Runtime.getRuntime().exec(command);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }

        return proc;
    }

    public static Process execAndWait(String[] command)
    {
        Process proc = ExecCommand.execCommand(command);

        try
        {
            proc.waitFor();
            proc.getInputStream().close();
            proc.getOutputStream().close();
            proc.getErrorStream().close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }

        return proc;
    }

    private static String[] execAndRead(String[] command) throws Exception
    {
        Vector<String> lineVector = new Vector<String>();

        Process proc = ExecCommand.execCommand(command);

        BufferedReader br =
            new BufferedReader(new InputStreamReader(proc.getInputStream()));

        String line = br.readLine();

        while (line != null)
        {
            lineVector.add(line);
            line = br.readLine();
        }

        proc.waitFor();

        proc.getInputStream().close();
        proc.getOutputStream().close();
        proc.getErrorStream().close();

        return (String[]) lineVector.toArray(new String[0]);
    }

    public static String[] execProvideOutput(String[] command)
    {
        String[] output = null;

        try
        {
            output = ExecCommand.execAndRead(command);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }

        return output;
    }

    public static String[] cat(String path)
    {
	    String[] catCmd = {"cat", path};
	    return ExecCommand.execProvideOutput(catCmd);
    }

    public static void cp(String src, String dest)
    {
        String[] cpCmd = {"cp", src, dest};
        ExecCommand.execAndWait(cpCmd);
    }

    public static void shellmv(String src, String dest)
    {
        String[] mvCmd = {"sh", "-c", "mv" + " " + src + " " + dest};
        ExecCommand.execAndWait(mvCmd);
    }

    public static void mkdir(String path)
    {
	    String[] mkdirCmd = {"mkdir", "-p", path};
	    ExecCommand.execAndWait(mkdirCmd);
    }

    public static void rm(String path)
    {
        String[] rmCmd = {"rm", "-f", path};
        ExecCommand.execAndWait(rmCmd);
    }

    public static void cntrkrm(String src)
    {
        String[] cntrkrmCmd = {"conntrack", "-D", "-s", src};
        ExecCommand.execProvideOutput(cntrkrmCmd);
    }

    public static void chmod(String perm, String path)
    {
        String[] chmodCmd = {"chmod", perm, path};
        ExecCommand.execAndWait(chmodCmd);
    }

    public static void sleep(int seconds)
    {
        if (seconds < 0)
        {
            return;
        }

        try
        {
            Thread.sleep(seconds * 1000);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public static void resetTAP(String cidr, String name)
    {
        String[] resetCmd = {"ip", "addr", "add", cidr, "dev", name};
        ExecCommand.execAndWait(resetCmd);
    }

    public static Process tcpdump(String iface, String file, String exp)
    {
        String[] tcpdumpCmd = 
            {"tcpdump", "-U", "-s", "0", "-i", iface, "-w", file, exp};
        return ExecCommand.execCommand(tcpdumpCmd);
    }

    public static Process qemu(String path, String image,
                               String mac, String ifname)
    {
        String cpu = "host";     //type of processor to use
        String cores = "2";      //number of cores to use
        String memory = "1.5G";  //amount of memory to use

        String[] qemuCmd = 
            {path,
             "-enable-kvm",
             "-cpu", cpu,
             "-smp", cores,
             "-hda", image,
             "-m", memory,
             "-balloon", "virtio",
             "-vga", "cirrus",
             "-nographic",
             "-net", "nic,macaddr=" + mac + ",model=virtio",
             "-net", "tap,script=no,ifname=" + ifname,
             "-snapshot"};

        return ExecCommand.execCommand(qemuCmd);
    }
}
