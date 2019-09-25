/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.ifm.sut.sftp.repo;

import au.com.ifm.sut.sftp.app.SutSftpMain;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.apache.log4j.Logger;

/**
 *
 * @author ifmuser1
 */
public class SqlServerClient {

    final static Logger logger = Logger.getLogger(SqlServerClient.class);
    String destSql = "SELECT [table_name],[field_name],[afm_type],[data_type] FROM [cqu213prod].[afm].[afm_flds]";
    String srcSql = "SELECT [table_name],[field_name],[afm_type],[data_type] FROM [mssql-hq-2014-0918].[afm].[afm_flds]";

    /**
     * executes the query on the tName table and return a Map of field name to
     * field values
     *
     * @param fNames
     * @param tName
     * @param cond
     * @return
     */
    public static List<List<Object>> executeSqlSelect(String fieldNames, String tableName, String cond) {

        Statement stmt = null;

        ResultSet rs = null;

        StringBuilder sqlStmt = new StringBuilder("SELECT ").append(fieldNames)
                .append(" FROM ").append(tableName).append(" WHERE ").append(cond);
        Connection con = MSSQLJDBC.getSqlServerDBConnection();
        if (con == null) {
            return null;
        }
        List<List<Object>> res = new ArrayList<>();

        try {
            stmt = con.createStatement();
            rs = stmt.executeQuery(sqlStmt.toString());
            String[] fna = fieldNames.split(",");
            while (rs.next()) {
                List<Object> ls = new ArrayList<>();
                for (String nm : fna) {
                    Object fo = rs.getObject(nm.trim());
                    ls.add(fo);
                }
                res.add(ls);
            }

        } catch (SQLException ex) {
            String m = ex.getMessage();
            logger.error(m);
            SutSftpMain.AddError(m);
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                    if (rs != null) {
                        rs.close();
                    }
                } catch (SQLException ex) {
                    String m = ex.getMessage();
                    logger.error(m);
                    SutSftpMain.AddError(m);
                }
            }
        }

        return res;
    }

    public static String executeSql(String sp) {
        Statement stmt = null;

        ResultSet rs = null;
        String retMsg = "Successfully executed=" + sp;

        Connection con = MSSQLJDBC.getSqlServerDBConnection();
        if (con == null) {
            logger.error("Connection Error: Null Database Connection");//added for error loggin 26-04-2018
            return "Connection Error: Null Database Connection";
        }

        try {
            stmt = con.createStatement();
            stmt.execute(sp);
            con.commit();//
            //retMsg = "Successfully executed="+sp;
            logger.debug(retMsg);

        } catch (SQLException ex) {
            String m = ex.getMessage();
            logger.error(m);
            SutSftpMain.AddError(m);
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                    if (rs != null) {
                        rs.close();
                    }

                } catch (SQLException ex) {
                    String m = ex.getMessage();
                    logger.error(m);
                    SutSftpMain.AddError(m);
                }
            }
        }
        return retMsg;
    }

    public static int getSelectRecNum(String fieldNames, String tableName, String cond) {
        Statement stmt = null;

        ResultSet rs = null;
        int sz = 0;

        StringBuilder sqlStmt = new StringBuilder("SELECT ").append(fieldNames)
                .append(" FROM ").append(tableName).append(" WHERE ").append(cond);
        Connection con = MSSQLJDBC.getSqlServerDBConnection();
        if (con == null) {
            return sz;
        }

        try {
            stmt = con.createStatement();
            rs = stmt.executeQuery(sqlStmt.toString());
            while (rs.next()) {
                sz++;
            }

        } catch (SQLException ex) {
            String m = ex.getMessage();
            logger.error(m);
            SutSftpMain.AddError(m);
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                    if (rs != null) {
                        rs.close();
                    }
                } catch (SQLException ex) {
                    String m = ex.getMessage();
                    logger.error(m);
                    SutSftpMain.AddError(m);
                }
            }
        }

        return sz;
    }
        
}
