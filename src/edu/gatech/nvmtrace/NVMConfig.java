package edu.gatech.nvmtrace;

public class NVMConfig
{
    private NVMThreadDB dbase;

    public NVMConfig(NVMThreadDB dbase)
    {
        this.dbase = dbase;
    }

    public String getInputPath()
    {
        return "/opt/gtisc/nvmtrace/input/";
    }

    private String getOutputPrefix()
    {
        return "/opt/gtisc/nvmtrace/output/";
    }

    public String getOutputPath(String sha256)
    {
        return this.getOutputPrefix() + sha256 + "/" + this.dbase.getProcessTime(sha256) + "/";
    }

    public String getPcapPath(String sha256)
    {
        return this.getOutputPath(sha256) + "dump.pcap";
    }

    public String getSystemDumpPath(String sha256)
    {
        return this.getOutputPath(sha256) + "sysdump";
    }

    public String getTCPDumpExpression(String macAddr)
    {
        return "ether host " + macAddr;
    }
}
