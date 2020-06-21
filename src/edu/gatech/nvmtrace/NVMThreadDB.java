package edu.gatech.nvmtrace;

import java.sql.*;

public class NVMThreadDB
{
    private static final String dbHost = "localhost";
    private static final String dbUser = "dbUser";
    private static final String dbName = "nvmtrace";
    private static final String dbPass = "dbPass";

    private Connection connection = null;

    static
    {
        try
        {
            Class.forName("org.postgresql.Driver").getDeclaredConstructor().newInstance();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private Connection currentConnection() throws SQLException
    {
        if (this.connection == null)
        {
            this.connection = DriverManager.
                getConnection("jdbc:postgresql://" + NVMThreadDB.dbHost +
                              ":5432/" + NVMThreadDB.dbName + "?" +
                              "user=" + NVMThreadDB.dbUser + 
                              "&password=" + NVMThreadDB.dbPass);

            return this.connection;
        }
        else
        {
            try
            {
                Statement stmt = connection.createStatement();
                stmt.executeQuery("SELECT TRUE");
                stmt.close();
            }
            catch (Exception e)
            {
                this.connection = DriverManager.
                    getConnection("jdbc:postgresql://" + NVMThreadDB.dbHost +
                                  ":5432/" + NVMThreadDB.dbName + "?" +
                                  "user=" + NVMThreadDB.dbUser + 
                                  "&password=" + NVMThreadDB.dbPass);
            }

            return this.connection;
        }
    }

    public String getUnprocessedSamplesha256()
    {
        String sha256 = null;

        try
        {
            Statement stmt = this.currentConnection().createStatement();

            ResultSet rset = 
                stmt.executeQuery("SELECT sha256 FROM sample WHERE " +
                                  "process_time IS NULL " +
                                  "ORDER BY submit_time ASC " +
                                  "LIMIT 64 FOR UPDATE");

            if (rset.next())
            {
                sha256 = rset.getString("sha256");
            }

            stmt.close();
        }
        catch (Exception e)
        {
            try
            {
                this.currentConnection().rollback();
            }
            catch (Exception e2)
            {
                e2.printStackTrace();
                System.exit(-1);
            }
        }

        return sha256;
    }

    public String getNextSamplesha256()
    {
        String nextSamplesha256 = null;

        try
        {
            this.currentConnection().setAutoCommit(false);

            nextSamplesha256 = this.getUnprocessedSamplesha256();

            if (nextSamplesha256 != null)
            {
                this.setProcessTime(nextSamplesha256,
                                   System.currentTimeMillis() / ((long) 1000));
            }

            this.currentConnection().commit();

            this.currentConnection().setAutoCommit(true);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }

        return nextSamplesha256;
    }

    public String getProcessTime(String sha256)
    {
        String processTime = null;

        try
        {
            Statement stmt = this.currentConnection().createStatement();

            ResultSet rset = 
                stmt.executeQuery("SELECT process_time FROM sample " +
                                  "WHERE sha256=" + "'" + sha256 + "'");

            if (rset.next())
            {
                processTime = rset.getString("process_time");
            }

            stmt.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }

        return processTime;
    }

    public void setProcessTime(String sha256, long processTime)
    {
        try
        {
            Statement stmt = this.currentConnection().createStatement();

            stmt.executeUpdate("UPDATE sample SET " +
                               "process_time=" + "'" + processTime + "' " +
                               "WHERE sha256=" + "'" + sha256 + "'");

            stmt.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
    }
} 
