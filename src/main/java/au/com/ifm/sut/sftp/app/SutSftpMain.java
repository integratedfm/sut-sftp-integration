/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.ifm.sut.sftp.app;

import au.com.ifm.sut.pgp.PGPFileProcessor;
import au.com.ifm.sut.sftp.repo.SqlServerClient;
import static au.com.ifm.sut.sftp.repo.SqlServerClient.executeSqlSelect;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.mail.MessagingException;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 *
 * @author ifmuser1
 */
public class SutSftpMain {

    private static Properties props;
    private static List<String> errorList = new ArrayList<>();
    private static List<String> authorizedUsers;
    final static Logger logger = Logger.getLogger(SutSftpMain.class);
    private static String dateTimeFormat;

    private static String userName, host;
    private static String sftp_pwd = "E7vMwPLKswPT^EZA";

    private static String remoteFolder, localFolder;

    private static Map<String, String> sutFieldMap;//sut field name to ab field name
    private static Map<String, String> sutFieldTypeMap;//sut field name to ab field name
    private static DateFormat sdf, loggerDateFormat;
    private static Date cur_date;

    private static int nrows_processed = 0;

    static String fnameCompleteTemplate, fnameIncrementalTemplate;
    static String archive_folder;

    public static void main(String[] args) throws Exception {
        initProps(args[0]);
        initSUTFieldMapping();
        loggerDateFormat = new SimpleDateFormat("dd-MM-yy hh:mm:ss");
        //  BasicConfigurator.configure();
        PropertyConfigurator.configure(getProp("sut.log4j.File"));

        cur_date = new Date();

        sdf = new SimpleDateFormat(getProp("sut.filename.date.format"));
        String cur_dt = sdf.format(cur_date);
        fnameCompleteTemplate = getProp("complete-feed-file-name");
        fnameIncrementalTemplate = getProp("daily-feed-file-name");
        String incFilter = fnameIncrementalTemplate + cur_dt + "*.pgp";

        String comFilter = fnameCompleteTemplate + cur_dt + "*.pgp";

        userName = props.getProperty("sut.sftp.user").trim();
        host = props.getProperty("sut.sftp.host").trim();
        remoteFolder = props.getProperty("import-remote-folder").trim();
        localFolder = props.getProperty("import-local-folder");

        archive_folder = props.getProperty("local-archive-folder");
        SftpClient sftpC = new SftpClient(userName, host, sftp_pwd);
        try {
            sftpC.getRemotePGPFile(localFolder, remoteFolder, ".pgp");
        } catch (JSchException je) {
            String m = je.getMessage();
            logger.error(m);
            AddError(m);
        } catch (SftpException sfe) {
            String m = sfe.getMessage();
            logger.error(m);
            AddError(m);
        }
        FileFilter incFileFilter = new WildcardFileFilter(incFilter);
        FileFilter comFileFilter = new WildcardFileFilter(comFilter);

        File ldir = new File(getProp("import-local-folder"));
        File[] importIncFiles = ldir.listFiles(incFileFilter);
        File[] importComFiles = ldir.listFiles(comFileFilter);

        File[] importedFiles = concat(importComFiles, importIncFiles);

        PGPFileProcessor pgpFP = new PGPFileProcessor();
        String secretFN = getProp("sut.pgp.privatefile");
        String pgpPwd = getProp("sut-pgp-sk-pwd");
        List<String> csvImportFiles = new ArrayList<>();
        //testDecryptFile();
        //(String inputFN, String skfn, String ofn, String pwd) throws FileNotFoundException, Exception{
        StringBuilder sb = new StringBuilder();
        for (File fi : importedFiles) {
            String ofn = fi.getAbsolutePath().replace(".pgp", "");
            logger.info("Importing file: " + fi.getName());

            try {
                pgpFP.decryptFile(fi.getAbsolutePath(), secretFN, ofn, pgpPwd);
                csvImportFiles.add(ofn);
                sb.append(fi.getName() + "; ");
            } catch (Exception e) {
                String m = e.getMessage();
                logger.error(m);
                AddError(m);

            }
        }

        for (String fns : csvImportFiles) {
            Path myPath = Paths.get(fns);
            importCSVFile(myPath);
            String csvfn = myPath.getFileName().toString();
            String pgpfn = csvfn + ".pgp";
            Path pgptarget = Paths.get(archive_folder + pgpfn);
            Path csvtarget = Paths.get(archive_folder + csvfn);
            Path pgpsrc = Paths.get(myPath.toString() + ".pgp");
            //Files.move(myPath, csvtarget, REPLACE_EXISTING);
            Files.move(pgpsrc, pgptarget, REPLACE_EXISTING);
            File csvFile = new File(myPath.toString());
            csvFile.delete();
        }
        sendEmail(sb.toString());

        System.exit(0);
    }

    /**
     *
     */
    public static void initSUTFieldMapping() {
        String[] sutFields = getProp("sut-src-field-names").split(",");
        String[] abFields = getProp("sut-dest-field-names").split(",");
        String[] abFieldTypes = getProp("sut-src-field-types").split(",");
        sutFieldMap = new HashMap<>();
        sutFieldTypeMap = new HashMap<>();
        int k = 0;
        for (String ss : sutFields) {
            sutFieldMap.put(ss.trim(), abFields[k].trim());
            sutFieldTypeMap.put(ss.trim(), abFieldTypes[k++].trim());
        }

    }

    /**
     *
     * @param myPath
     * @throws IOException
     */
    public static void importCSVFile(Path myPath) throws IOException {
        CSVParser parser = new CSVParserBuilder().withSeparator(',').withQuoteChar('"').build();
        StringBuilder csvOutput = new StringBuilder("");

        //INSERT INTO afm.em (EM_ID,f1,f2,.. ) VALUES ('', v1, v2, ...)
        //UPDATE afm.em SET c1=v1, c2=v2
        try (BufferedReader br = Files.newBufferedReader(myPath,
                StandardCharsets.UTF_8);
                CSVReader reader = new CSVReaderBuilder(br).withCSVParser(parser)
                .build()) {

            List<String[]> rows = reader.readAll();
            String[] hdr = rows.get(0);
            int lupdPos = -1;
            int em_id = -1;
            int fte_id = -1;
            int cf_id = -1;

            //get header based on AB em field names 
            StringBuilder headerStrBuf = new StringBuilder("");

            for (int i = 0; i < hdr.length; i++) {
                headerStrBuf.append("," + sutFieldMap.get(hdr[i]));
                if (hdr[i].contains("LastUpdatedTimestamp")) {
                    lupdPos = i;//store lastdate for putting into last time
                } else if (hdr[i].contains("SIMs")) {
                    em_id = i;
                } else if (hdr[i].contains("FTE")) {
                    fte_id = i;
                } else if (hdr[i].contains("Classification")) {
                    cf_id = i;
                }
            }
            headerStrBuf.delete(0, 1);//remove first , from header
            if (lupdPos > -1) {
                headerStrBuf.append(",source_time_update");
            }

            csvOutput.append(headerStrBuf + "\n");

            for (int k = 1; k < rows.size(); k++) {
                String[] row = rows.get(k);
                //added fix to not allow empty em_id be imported
                if (row[em_id].trim().length() < 1) {
                    continue;
                }
                boolean nrec = true;

                StringBuilder fvsb = new StringBuilder("UPDATE afm.em SET ");
                StringBuilder isb = new StringBuilder("");

                //int rn = SqlServerClient.getSelectRecNum("fte, em_id", "afm.em", " em_id='" + row[em_id] + "'");
                List<List<Object>> rs = SqlServerClient.executeSqlSelect("fte, classification", "afm.em", " em_id='" + row[em_id] + "'");
                int rn = rs.size();
                if (lupdPos > -1) {
                    String t = row[lupdPos].replace("T", " ");;
                    row[lupdPos] = t;
                }
                if (rn > 0) {//added for not updating fte and classification if classification is empty
                    try {
                        Object fte = rs.get(0).get(0);
                        Object cf = rs.get(0).get(1);
                        String cf_import = row[cf_id].trim();
                        if (cf_import.length() < 1) {

                            row[fte_id] = fte.toString().trim();
                            row[cf_id] = cf.toString().trim();

                        }
                    } catch (Exception efte) {
                        logger.error(efte.getMessage()+ " classification is null value");
                    }
                }

                //row.length is number of fields per row
                for (int j = 0; j < row.length; j++) {
                    String fn = sutFieldMap.get(hdr[j]);
                    String ftype = sutFieldTypeMap.get(hdr[j]);
                    String v = row[j].trim();
                    String ov = v;
                    if (v.contains("'")) {
                        v = v.replaceAll("'", "''");
                    }

                    csvOutput.append('"' + ov + '"' + ',');//need to be quoted
                    if (ftype.equals("number")) {
                        isb.append(v + ",");
                        fvsb.append(fn + "=" + v + ",");
                    } else {

                        isb.append("'" + v + "',");
                        fvsb.append(fn + "='" + v + "',");
                    }
                }

                if (lupdPos > -1) {
                    isb.append("'" + row[lupdPos] + "'");
                    fvsb.append("source_time_update='" + row[lupdPos].trim() + "'");
                    csvOutput.append('"' + row[lupdPos] + '"');
                } else {
                    isb.delete(isb.length() - 1, isb.length());
                    fvsb.delete(fvsb.length() - 1, fvsb.length());
                    csvOutput.delete(csvOutput.length() - 1, csvOutput.length());
                }
                String insert = "INSERT INTO afm.em (" + headerStrBuf + ") VALUES (" + isb + ")";
                String upd = fvsb + " WHERE em_id='" + row[0].trim() + "'";
                csvOutput.append("\n");

                if (rn == 0) {
                    //System.out.println(insert);

                    SqlServerClient.executeSql(insert);
                    logger.info(insert);
                } else {
                    //System.out.println(upd);

                    SqlServerClient.executeSql(upd);
                    logger.info(upd);
                }
                nrows_processed++;
            }
        }//*/
        // System.out.println(csvOutput);

    }

    public static void sendEmail(String importedFiles) {
        try {
            {//if (errorList.size() > 0)
                //logger.debug("Trying to send email ");
                EmailHandler emh = new EmailHandler();

                String msg = "Employee import report";//props.getProperty("sut.sftp.error.email.body");
                String email_subject = "Employee import report";//props.getProperty("sut.sftp.error.email.subject");
                StringBuilder bodyBuilder;
                StringBuilder subjBuilder;

                if (errorList.size() < 1) {
                    msg = "Employee csv files imported successfully. \n" + "Number of updated employees: " + nrows_processed + "\n" + "Imported files: \n" + importedFiles;//props.getProperty("sut.sftp.success.email.body");
                    email_subject = "Employee records updated successfully: " + nrows_processed;//props.getProperty("sut.sftp.success.email.subject");

                    //email_subject = email_subject.replace("NUMRECS", "" + nrows_processed);
                    bodyBuilder = new StringBuilder(msg);

                    msg = bodyBuilder.toString();

                } else if (errorList.size() > 0) {

                    bodyBuilder = new StringBuilder(msg);

                    for (int k = 0; k < errorList.size(); k++) {
                        bodyBuilder.append(errorList.get(k)).append("\n");
                    }

                    msg = bodyBuilder.toString();

                    email_subject = "Employee records updated with errors. Updated records: "
                            + nrows_processed + " Number of errors: " + errorList.size(); //email_subject.replace("NUMRECS", "" + nrows_processed);

                }
                String toAddr = props.getProperty("sut.mail.to");

                emh.send(toAddr, email_subject, msg, null);
                //logger.debug("Success send email ");
            }
        } catch (MessagingException ex) {
            String m = ex.getMessage();
            logger.error(m);
            AddError(m);

        }
    }

    public static void initProps(String filePath) throws IOException {
        FileInputStream fis = null;
        //System.out.println(filePath);
        //logger.info(Paths.get(".").toAbsolutePath().normalize());
        try {
            fis = new FileInputStream(new File(filePath));///read the properties file using the first input argument
            props = new Properties();
            props.load(fis);//load all the propties

            dateTimeFormat = getProp("ws.datetime.format");
        } catch (FileNotFoundException fnf) {
            logger.error(fnf.getMessage());
            AddError(fnf.getMessage());
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }

    public static String getProp(String pName) {
        return props.getProperty(pName);
    }

    public static String getDateTimeFormat() {
        return dateTimeFormat;
    }

    public static void AddError(String err) {

        cur_date.setTime(System.currentTimeMillis());
        String tm = loggerDateFormat.format(cur_date);
        String err_msg = "[Error]" + " " + tm + " " + err;
        errorList.add(err_msg);
    }

    public static <T> T[] concat(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

}
