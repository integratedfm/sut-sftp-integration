/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.ifm.sut.sftp.app;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import org.apache.log4j.Logger;

/**
 *
 * @author ifmuser1
 */
public class SftpClient {

    final static Logger logger = Logger.getLogger(SftpClient.class);
    String host, userName, pwd;

    Session session;
    ChannelSftp sftpChannel;
    JSch jsch;

    /**
     *
     * @param un
     * @param h
     * @param pw
     */
    public SftpClient(String un, String h, String pw) {
        host = h;
        userName = un;
        pwd = pw;
    }

    public List<File> getRemotePGPFile(String import_local_dir, String import_remote_dir, String ext) throws JSchException, SftpException, IOException {

        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        List<File> fl = new ArrayList<>();
        File dir = new File(import_local_dir);
        
        if (!dir.isDirectory()){
            throw new java.io.IOException(import_local_dir+" is not a directory");
        }
        
        if (session == null || sftpChannel == null) {
            jsch = new JSch();
            session = jsch.getSession(userName, host);
            session.setConfig(config);
            session.setPassword(pwd);
            session.setConfig("StrictHostKeyChecking", "no");
            logger.info("Establishing Connection...");
            session.connect();
            logger.info("Connection established.");
            logger.info("Creating SFTP Channel.");
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            logger.info("SFTP Channel created.");

        }
        sftpChannel.cd("/");
        sftpChannel.cd(import_remote_dir);

        Vector filelist = sftpChannel.ls(".");
        sftpChannel.lcd(import_local_dir);
 
        for (int i = 0; i < filelist.size(); i++) {
            ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) filelist.get(i);
            String fn = entry.getFilename();
            if (!fn.endsWith(ext)) {
                continue;
            }
            sftpChannel.get(fn, ".");

            File f= new File(import_local_dir+"/"+fn);
            if (f.exists()){
                fl.add(f);
            }
        }
        sftpChannel.exit();
        session.disconnect();
        return fl;

    }

    /**
     * returns files with name ending with ext
     * @param import_local_dir
     * @param ext
     * @return 
     */
    public List<File> getListOfLocalDirFiles(String import_local_dir, String ext) {
        File dir = new File(import_local_dir);
        List<File> fl = new ArrayList<>();
        if (!dir.exists() || !dir.isDirectory()) {
            return fl;
        }
        String[] fa = dir.list();
        for (String fn : fa) {
            File f = new File(fn);
            if (f.getName().endsWith(ext)) {
                fl.add(f);

            }
        }
        return fl;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

}
