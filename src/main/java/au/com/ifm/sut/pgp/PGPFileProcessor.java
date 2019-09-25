/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.ifm.sut.pgp;

/**
 *
 * @author ifmuser1
 */
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

 
public class PGPFileProcessor {
 
    private String passphrase;
    private String publicKeyFileName;
    private String secretKeyFileName;
    private String inputFileName;
    private String outputFileName;
    private boolean asciiArmored = false;
    private boolean integrityCheck = true;
 
    public boolean encrypt() throws Exception {
        FileInputStream keyIn = new FileInputStream(publicKeyFileName);
        FileOutputStream out = new FileOutputStream(outputFileName);
        PGPUtils.encryptFile(out, inputFileName, PGPUtils.readPublicKey(keyIn), asciiArmored, integrityCheck);
        out.close();
        keyIn.close();
        return true;
    }
 
    
 
    public boolean decrypt() throws Exception {
        FileInputStream in = new FileInputStream(inputFileName);
        FileInputStream keyIn = new FileInputStream(secretKeyFileName);
        FileOutputStream out = new FileOutputStream(outputFileName);
        PGPUtils.decryptFile(in, out, keyIn, passphrase.toCharArray());
        in.close();
        out.close();
        keyIn.close();
        return true;
    }
    /**
     * 
     * @param inputFN
     * @param skfn: SecretKeyFileName
     * @param ofn: OutputFileName
     * @param pwd
     * @return
     * @throws FileNotFoundException
     * @throws Exception 
     */
    public boolean decryptFile(String inputFN, String skfn, String ofn, String pwd) throws FileNotFoundException, Exception{
        FileInputStream in = new FileInputStream(inputFN);
        FileInputStream keyIn = new FileInputStream(skfn);
        FileOutputStream out = new FileOutputStream(ofn);
        PGPUtils.decryptFile(in, out, keyIn, pwd.toCharArray());
        in.close();
        out.close();
        keyIn.close();
        return true;
    }
 
    public boolean isAsciiArmored() {
            return asciiArmored;
    }
 
    public void setAsciiArmored(boolean asciiArmored) {
            this.asciiArmored = asciiArmored;
    }
 
    public boolean isIntegrityCheck() {
            return integrityCheck;
    }
 
    public void setIntegrityCheck(boolean integrityCheck) {
            this.integrityCheck = integrityCheck;
    }
 
    public String getPassphrase() {
            return passphrase;
    }
 
    public void setPassphrase(String passphrase) {
            this.passphrase = passphrase;
    }
 
    public String getPublicKeyFileName() {
            return publicKeyFileName;
    }
 
    public void setPublicKeyFileName(String publicKeyFileName) {
            this.publicKeyFileName = publicKeyFileName;
    }
 
    public String getSecretKeyFileName() {
            return secretKeyFileName;
    }
 
    public void setSecretKeyFileName(String secretKeyFileName) {
            this.secretKeyFileName = secretKeyFileName;
    }
 
    public String getInputFileName() {
            return inputFileName;
    }
 
    public void setInputFileName(String inputFileName) {
            this.inputFileName = inputFileName;
    }
 
    public String getOutputFileName() {
            return outputFileName;
    }
 
    public void setOutputFileName(String outputFileName) {
            this.outputFileName = outputFileName;
    }
 

}