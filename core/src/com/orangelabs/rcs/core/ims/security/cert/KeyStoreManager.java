/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.orangelabs.rcs.core.ims.security.cert;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.provider.settings.RcsSettingsData;

/**
 * Keystore manager for certificates
 * 
 * @author B. JOGUET
 * @author Deutsche Telekom AG
 */
public class KeyStoreManager {

    /**
     * Keystore name
     */
    private final static String KEYSTORE_NAME = "rcs_keystore.jks";

    /**
     * Keystore password
     */
    private final static String KEYSTORE_PASSWORD = "01RCSrcs";

    /**
     * Load the keystore manager
     * 
     * @throws KeyStoreManagerException
     */
    public static void loadKeyStore() throws KeyStoreManagerException {    
        // Create the keystore if not present
        if (!KeyStoreManager.isKeystoreExists(KeyStoreManager.getKeystorePath())) {
            KeyStoreManager.createKeyStore();
        }
        
        // Add certificates if not present
        String certRoot = RcsSettings.getInstance().getTlsCertificateRoot();
        if ((certRoot != null) && (certRoot.length() > 0)) {
            if (!KeyStoreManager.isCertificateEntry(certRoot)) {
            	KeyStoreManager.addCertificates(certRoot);
            }
        }
        String certIntermediate = RcsSettings.getInstance().getTlsCertificateIntermediate();
        if ((certIntermediate != null) && (certIntermediate.length() > 0)) {
            if (!KeyStoreManager.isCertificateEntry(certIntermediate)) {
                KeyStoreManager.addCertificates(certIntermediate);
            }
        }
    }    
    
    /**
     * Returns the keystore type
     * 
     * @return Type
     */
    public static String getKeystoreType() {
    	return KeyStore.getDefaultType();
    }
    
    /**
     * Returns the keystore password
     * 
     * @return Password
     */
    public static String getKeystorePassword() {
    	return KEYSTORE_PASSWORD;
    }
    	
    /**
     * Returns the keystore path
     * 
     * @return Keystore path
     */
    public static String getKeystorePath() {
        return AndroidFactory.getApplicationContext().getFilesDir().getAbsolutePath() + "/"
                + KEYSTORE_NAME;
    }

    /**
     * Test if a keystore is created
     * 
     * @return True if already created
     * @throws KeyStoreManagerException
     */
    private static boolean isKeystoreExists(String path) throws KeyStoreManagerException {
        // Test file 
        File file = new File(path);
        if ((file == null) || (!file.exists()))
            return false;
        
        // Test keystore
        FileInputStream fis = null;
        boolean result = false;
        try {
            // Try to open the keystore
            fis = new FileInputStream(path);
            KeyStore ks = KeyStore.getInstance(getKeystoreType());
            ks.load(fis, KEYSTORE_PASSWORD.toCharArray());
            result = true;
        } catch (FileNotFoundException e) {
            throw new KeyStoreManagerException(e.getMessage());
        } catch (Exception e) {
            result = false;
        } finally {
            try {
                if (fis != null)
                    fis.close();
            } catch (IOException e) {
                // Intentionally blank
            }
        }
        return result;
    }

    /**
     * Create the RCS keystore
     * 
     * @throws KeyStoreManagerException
     */
    private static void createKeyStore() throws KeyStoreManagerException {
        File file = new File(getKeystorePath());
        if ((file == null) || (!file.exists())) {
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(getKeystorePath());
                
                // Build empty keystore
                KeyStore ks = KeyStore.getInstance(getKeystoreType());
                ks.load(null, KEYSTORE_PASSWORD.toCharArray());
                
                // Export keystore in a file
                ks.store(fos, KEYSTORE_PASSWORD.toCharArray());
            } catch (Exception e) {
                throw new KeyStoreManagerException(e.getMessage());
            } finally {
                try {
                    if (fos != null)
                        fos.close();
                } catch (IOException e) {
                    // Intentionally blank
                }
            }
        }
    }

    /**
     * Check if a certificate is in the keystore
     * 
     * @param path Certificate path
     * @return True if available
     * @throws KeyStoreManagerException
     */
    private static boolean isCertificateEntry(String path) throws KeyStoreManagerException {
        FileInputStream fis = null;
        boolean result = false;
        if (KeyStoreManager.isKeystoreExists(getKeystorePath())) {
            try {
                fis = new FileInputStream(getKeystorePath());
                
                // Open the existing keystore
                KeyStore ks = KeyStore.getInstance(getKeystoreType());
                ks.load(fis, KEYSTORE_PASSWORD.toCharArray());
                result = ks.isCertificateEntry(buildCertificateAlias(path));
            } catch (Exception e) {
                throw new KeyStoreManagerException(e.getMessage());
            } finally {
                try {
                    if (fis != null)
                        fis.close();
                } catch (IOException e) {
                    // Intentionally blank
                }
            }
        } 
        return result;
    }

    /**
     * Add a certificate or all certificates in folder in the keystore
     *
     * @param path certificates path
     * @throws KeyStoreManagerException
     */
    private static void addCertificates(String path) throws KeyStoreManagerException {
        if (KeyStoreManager.isKeystoreExists(getKeystorePath())) {
            FileInputStream fis = null;
            FileOutputStream fos = null;
            try {
                // Open the existing keystore
                fis = new FileInputStream(getKeystorePath());
                KeyStore ks = KeyStore.getInstance(getKeystoreType());
                ks.load(fis, KEYSTORE_PASSWORD.toCharArray());

                // Open certificates path
                File pathFile = new File(path);
                if (pathFile.isDirectory()) {
                    // The path is a folder, add all certificates
                    File[] certificates = pathFile.listFiles(new FilenameFilter() {
                        public boolean accept(File dir, String filename) {
                            return filename.endsWith(RcsSettingsData.CERTIFICATE_FILE_TYPE);
                        }
                    });

                    if (certificates != null) {
                        for (File file : certificates) {
                            // Get certificate and add in keystore
                            CertificateFactory cf = CertificateFactory.getInstance("X.509");
                            InputStream inStream = new FileInputStream(file);
                            X509Certificate cert = (X509Certificate) cf.generateCertificate(inStream);
                            inStream.close();
                            ks.setCertificateEntry(buildCertificateAlias(path), cert);

                            // Save the keystore
                            fos = new FileOutputStream(getKeystorePath());
                            ks.store(fos, KEYSTORE_PASSWORD.toCharArray());
                            fos.close();
                            fos = null;
                        }
                    }
                } else {
                    // The path is a file, add certificate
                    if (path.endsWith(RcsSettingsData.CERTIFICATE_FILE_TYPE)) {
                        // Get certificate and add in keystore
                        CertificateFactory cf = CertificateFactory.getInstance("X.509");
                        InputStream inStream = new FileInputStream(path);
                        X509Certificate cert = (X509Certificate) cf.generateCertificate(inStream);
                        inStream.close();
                        ks.setCertificateEntry(buildCertificateAlias(path), cert);

                        // Save the keystore
                        fos = new FileOutputStream(getKeystorePath());
                        ks.store(fos, KEYSTORE_PASSWORD.toCharArray());
                    }
                }
            } catch (Exception e) {
                throw new KeyStoreManagerException(e.getMessage());
            } finally {
                try {
                    if (fis != null) {
                        fis.close();
                    }
                } catch (IOException e) {
                    // Intentionally blank
                }
                try {
                    if (fos != null) {
                        fos.close();
                    }
                } catch (IOException e) {
                    // Intentionally blank
                }
            }
        }
    }

    /**
     * Build alias from path
     * 
     * @param path File path
     * @return Alias
     */
    private static String buildCertificateAlias(String path) {
        String alias = "";
        File file = new File(path);
        String filename = file.getName();
        long lastModified = file.lastModified();
        int lastDotPosition = filename.lastIndexOf('.');
        if (lastDotPosition > 0)
            alias = filename.substring(0, lastDotPosition) + lastModified;
        else
            alias = filename + lastModified;
        return alias;
    }
    
    /**
     * Returns the fingerprint of a certificate
     * 
     * @param cert Certificate
     * @return String as xx:yy:zz
     */
    public static String getCertFingerprint(Certificate cert) {
    	try {
		    MessageDigest md = MessageDigest.getInstance("SHA-1");
		    byte[] der = cert.getEncoded();
		    md.update(der);
		    byte[] digest = md.digest();
		    return hexify(digest);
    	} catch(Exception e) {
    		return null;
    	}
	}

    /**
     * Hexify a byte array 
     * 
     * @param bytes Byte array
     * @return String
     */
	private static String hexify(byte bytes[]) {
	    char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', 
	                    '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
	    StringBuffer buf = new StringBuffer(bytes.length * 2);
	    for (int i = 0; i < bytes.length; ++i) {
	    	if (i != 0) {
	    		buf.append(":");	
	    	}
	    	buf.append(hexDigits[(bytes[i] & 0xf0) >> 4]);
	        buf.append(hexDigits[bytes[i] & 0x0f]);
	    }
	    return buf.toString();
	}
}
