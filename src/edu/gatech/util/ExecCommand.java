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
        String[] cmd = {"cat", path};
        return ExecCommand.execProvideOutput(cmd);
    }

    public static void cp(String src, String dest)
    {
        String[] cmd = {"cp", src, dest};
        ExecCommand.execAndWait(cmd);
    }

    public static void shellmv(String src, String dest)
    {
        String[] cmd = {"sh", "-c", "mv" + " " + src + " " + dest};
        ExecCommand.execAndWait(cmd);
    }

    public static void mkdir(String path)
    {
        String[] cmd = {"mkdir", "-p", path};
        ExecCommand.execAndWait(cmd);
    }

    public static void rm(String path)
    {
        String[] cmd = {"rm", "-f", path};
        ExecCommand.execAndWait(cmd);
    }

    public static void cntrkrm(String src)
    {
        String[] cmd = {"conntrack", "-D", "-s", src};
        ExecCommand.execProvideOutput(cmd);
    }

    public static void chmod(String perm, String path)
    {
        String[] cmd = {"chmod", perm, path};
        ExecCommand.execAndWait(cmd);
    }

    public static void chown(String perm, String path)
    {
        String[] cmd = {"chown", perm, path};
        ExecCommand.execAndWait(cmd);
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

    public static void disableRL(String name)
    {
        String[] cmd = {"wondershaper", "clear", name};
        ExecCommand.execAndWait(cmd);
    }

    public static void enableRL(String name)
    {
        String[] cmd = {"wondershaper", name, "10000", "10000"};
        ExecCommand.execAndWait(cmd);
    }

    public static void resetTAP(String cidr, String name)
    {
        String[] cmd = {"ip", "addr", "add", cidr, "dev", name};
        ExecCommand.execAndWait(cmd);
    }

    public static int uploadStarted(String path)
    {
        String[] cmd = {"sh", "-c", "ls -A " +  path + " | wc -l"};

        int rv;
        String result = ExecCommand.execProvideOutput(cmd)[0];

        try
        {
            rv = Integer.parseInt(result.trim());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            rv = 0;
        }

        return rv;
    }

    public static Process tcpdump(String iface, String file, String exp)
    {
        String[] cmd = 
            {"tcpdump", "-U", "-s", "0", "-i", iface, "-w", file, exp};
        return ExecCommand.execCommand(cmd);
    }

    public static Process qemu(String path, String image,
                               String mac, String ifname)
    {
        String cpu = "host";     //type of processor to use
        String cores = "2";      //number of cores to use
        String memory = "1.5G";  //amount of memory to use

        String[] cmd = 
            {"runuser","-l","<user>","-c",path + " -enable-kvm "
                                             + " -cpu " + cpu
                                             + " -smp " + cores
                                             + " -hda " + image
                                             + " -m " + memory
                                             + " -device " + " virtio-balloon "
                                             + " -vga " + " cirrus "
                                             + " -nographic "
                                             + " -net " + " nic,macaddr=" + mac + ",model=virtio "
                                             + " -net " + " tap,script=no,ifname=" + ifname
            };

        /* Debugging */
/*
             " -vnc " + " localhost:" + ifname.substring(2)
*/

        return ExecCommand.execCommand(cmd);
    }

    public static Process removeSnapshot(String image)
    {
        String[] cmd = 
            {"rm", image};

        return ExecCommand.execCommand(cmd);
    }

    // From: https://wiki.qemu.org/Documentation/CreateSnapshot
    public static Process resetSnapshot(String image)
    {
        String[] cmd = 
            {"qemu-img",
             "create",
             "-f", "qcow2",
             "-b", "/opt/gtisc/lib/nvmtrace.qcow3",
            image};

        return ExecCommand.execCommand(cmd);
    }
}
