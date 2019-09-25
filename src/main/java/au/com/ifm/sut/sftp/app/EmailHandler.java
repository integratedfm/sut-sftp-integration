/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.ifm.sut.sftp.app;



import java.io.FileInputStream;
import java.net.InetAddress;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.apache.log4j.Logger;

public class EmailHandler {

    final static Logger logger = Logger.getLogger(EmailHandler.class);
    private static EmailHandler eh = null;

    private String to;
    private String from;
    private String host;
    private String msgText;
    private String subject;
    
   
    
    private boolean debug = false;
    private String passwd = "";
    private int smtp_port = 0;
    private String userName = "";

    private static EmailHandler emh;

    public EmailHandler() {////assume email config is in kioskmail.cfg
        FileInputStream in;

        host = SutSftpMain.getProp("ifm.mail.host");
        from = SutSftpMain.getProp("ifm.mail.from");
        passwd = SutSftpMain.getProp("ifm.mail.passwd");
        smtp_port = Integer.parseInt(SutSftpMain.getProp("ifm.smtp.port").trim());
        userName = SutSftpMain.getProp("ifm.mail.username");
        //logger.debug("user name, passwd, from, host are "+ userName+"; "+passwd+", "+from+"; "+ host);

        Properties props = System.getProperties();

        String senderEmail = from;
        String senderMailPassword = passwd;
        String gmail = host;
    }
////=============

   
    public void send(String recipeintEmail,
            String subject,
            String messageText,
            String[] attachments)
            throws MessagingException, AddressException {

        Properties props = System.getProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", smtp_port);
        props.put("mail.smtp.ssl.trust", host);
        
        String[] recipients=recipeintEmail.split(";");
        
        //logger.debug("user name, passwd, from, host, smtp_port are "+ userName+"; "+passwd+", "+from+"; "+ host+"; "+ smtp_port);
        //props.put("mail.smtp.user", userName);
        //props.put("mail.smtp.password", passwd);

        Session session;

        session = Session.getDefaultInstance(props, null);
        // session.setDebug(true);

        MimeMessage message = new MimeMessage(session);
        
        //message.setFrom(new InternetAddress(from));
        InternetAddress fromAddr = new InternetAddress(userName + "<" + from + ">");
        
        message.setFrom(fromAddr);
        for(String destEmailAddr: recipients){/////IFM Mehran Enhance 1.2 Multiple email receivers
               message.addRecipient(Message.RecipientType.TO,
                new InternetAddress(destEmailAddr));
        }


        message.setContent(messageText, "text/plain");
        message.setSubject(subject);
        
        message.setFrom(from);
        
        // Transport.send(message);
        Transport transport = session.getTransport("smtp");
        //logger.debug("trying to run email transport");
        transport.connect(host, smtp_port, userName, passwd);
        //logger.debug("success to connect to email host server");
        transport.sendMessage(message, message.getAllRecipients());
        //logger.debug("success to send email to host server");

        transport.close();

    }

    public static EmailHandler getEmailHandler() {
        if (eh == null) {
            eh = new EmailHandler();

        }
        return eh;
    }

}
