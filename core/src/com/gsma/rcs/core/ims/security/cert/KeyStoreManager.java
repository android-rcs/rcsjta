/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.core.ims.security.cert;

import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData;
import com.gsma.rcs.utils.CloseableUtils;
import com.gsma.rcs.utils.logger.Logger;

import com.telekom.bouncycastle.wrapper.SimpleContentSignerBuilder;

import android.net.Uri;
import android.text.TextUtils;

import local.org.bouncycastle.asn1.ASN1Encodable;
import local.org.bouncycastle.asn1.x500.X500Name;
import local.org.bouncycastle.asn1.x509.BasicConstraints;
import local.org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import local.org.bouncycastle.asn1.x509.GeneralName;
import local.org.bouncycastle.asn1.x509.GeneralNames;
import local.org.bouncycastle.asn1.x509.KeyPurposeId;
import local.org.bouncycastle.asn1.x509.KeyUsage;
import local.org.bouncycastle.asn1.x509.X509Extension;
import local.org.bouncycastle.cert.CertIOException;
import local.org.bouncycastle.cert.X509v3CertificateBuilder;
import local.org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import local.org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import local.org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import local.org.bouncycastle.operator.ContentSigner;
import local.org.bouncycastle.operator.OperatorCreationException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * Keystore manager for certificates
 * 
 * @author B. JOGUET
 * @author Deutsche Telekom AG
 */
public class KeyStoreManager {
    /**
     * Rate to convert from seconds to milliseconds
     */
    private static final long SECONDS_TO_MILLISECONDS_CONVERSION_RATE = 1000;

    /**
     * Keystore name
     */
    private final static String KEYSTORE_NAME = "rcs_keystore.jks";

    // Changed by Deutsche Telekom
    /**
     * alias of own, client certificate
     */
    protected final static String CLIENT_CERT_ALIAS = "myJoynCertificate";

    // Changed by Deutsche Telekom
    /**
     * The logger
     */
    private static Logger sLogger = Logger.getLogger(KeyStoreManager.class.getName());

    /**
     * Keystore password
     */
    private final static String KEYSTORE_PASSWORD = "01RCSrcs";

    // Changed by Deutsche Telekom
    /**
     * The logger
     */
    private static String sFingerprint;

    // Changed by Deutsche Telekom
    /**
     * last used IP address
     */
    private static String sLastIpAddress;

    private static KeyStore sKeyStore;

    /**
     * Load the keystore manager
     * 
     * @param rcsSettings
     * @throws IOException
     * @throws KeyStoreException
     */
    // Changed by Deutsche Telekom
    public static void loadKeyStore(RcsSettings rcsSettings) throws KeyStoreException, IOException {
        // Changed by Deutsche Telekom
        if (sLogger.isActivated()) {
            Provider[] currentProviders = Security.getProviders();
            if (currentProviders.length > 0) {
                for (Provider provider : currentProviders) {
                    sLogger.debug("Registered provider: " + provider.getName() + "; info: "
                            + provider.getInfo());
                }
            }
        }

        // Changed by Deutsche Telekom
        if (!isKeystoreExists()) {
            // Changed by Deutsche Telekom
            if (sLogger.isActivated()) {
                sLogger.debug("Create new keystore file ".concat(getKeystore().getPath()));
            }
            createKeyStore();
        }

        String certRoot = rcsSettings.getTlsCertificateRoot();
        if (!TextUtils.isEmpty(certRoot)) {
            Uri certFile = Uri.parse(certRoot);
            if (!isCertificateEntry(buildCertificateAlias(certFile))) {
                addCertificates(certFile);
            }
        }
        String certIntermediate = rcsSettings.getTlsCertificateIntermediate();
        if (!TextUtils.isEmpty(certIntermediate)) {
            Uri certIntermediateFile = Uri.parse(certIntermediate);
            if (!isCertificateEntry(buildCertificateAlias(certIntermediateFile))) {
                addCertificates(certIntermediateFile);
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
     * returns the keystore uri
     */
    public static Uri getKeystore() {
        return Uri.fromFile(new File(AndroidFactory.getApplicationContext().getFilesDir()
                .getAbsolutePath(), KEYSTORE_NAME));
    }

    // Changed by Deutsche Telekom
    /**
     * Returns the fingerprint of the client certificate
     * 
     * @return fingerprint
     */
    public static String getClientCertificateFingerprint() {
        return sFingerprint;
    }

    // Changed by Deutsche Telekom
    /**
     * Sets the fingerprint of the client certificate
     * 
     * @param cert certificate
     * @throws NoSuchAlgorithmException
     * @throws CertificateEncodingException
     */
    public static void setClientCertificateFingerprint(Certificate cert)
            throws CertificateEncodingException, NoSuchAlgorithmException {
        sFingerprint = getCertFingerprint(cert, "SHA-1");
    }

    /**
     * Test if a keystore is created
     * 
     * @return True if already created
     */
    private static boolean isKeystoreExists() {
        File file = new File(getKeystore().getPath());
        if (!file.exists()) {
            return false;
        }
        return sKeyStore != null;
    }

    // Changed by Deutsche Telekom
    /**
     * update (or create) current client certificate to reflect latest IP address
     * 
     * @param ipAddress IP address to be set in subjectAltName according to RFC 4572
     * @throws CertificateException
     * @throws IOException
     */
    public static void updateClientCertificate(String ipAddress) throws CertificateException,
            IOException {
        if (!isKeystoreExists()) {
            throw new CertificateException(new StringBuilder(
                    "Client certificate not created as keystore file ").append(getKeystore())
                    .append(" is not available").toString());
        }
        if (sLogger.isActivated()) {
            sLogger.debug("Update client certificate");
        }
        createClientCertificate(ipAddress);
    }

    // Changed by Deutsche Telekom
    private static synchronized void createClientCertificate(String ipAddress)
            throws CertificateException, IOException {
        try {
            if (ipAddress != null && ipAddress.equals(sLastIpAddress)) {
                if (sLogger.isActivated()) {
                    sLogger.debug("IP address hasn't changed. No update needed.");
                }
                return;
            }
            sLastIpAddress = ipAddress;

            KeyStore ks = loadKeyStoreFromFile();
            PrivateKey privKey = null;
            PublicKey pubKey = null;
            if (ks.isKeyEntry(CLIENT_CERT_ALIAS)) {
                if (sLogger.isActivated()) {
                    sLogger.debug("old keypair is recycled");
                }

                PrivateKeyEntry entry = (PrivateKeyEntry) ks.getEntry(CLIENT_CERT_ALIAS,
                        new KeyStore.PasswordProtection(KEYSTORE_PASSWORD.toCharArray()));
                privKey = entry.getPrivateKey();
                pubKey = entry.getCertificate().getPublicKey();
                ks.deleteEntry(CLIENT_CERT_ALIAS);
            } else {
                if (sLogger.isActivated()) {
                    sLogger.debug("new keypair is generated");
                }

                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
                SecureRandom secureRandom = new SecureRandom();
                /*
                 * Do *not* seed secureRandom! Automatically seeded from system entropy.
                 */
                keyGen.initialize(1024, secureRandom);
                KeyPair keypair = keyGen.generateKeyPair();
                privKey = keypair.getPrivate();
                pubKey = keypair.getPublic();
            }
            X509Certificate[] certChain = new X509Certificate[1];
            X500Name subjectName = new X500Name("CN=com.gsma.rcs.client");
            long timestamp = System.currentTimeMillis();
            Date startDate = new Date(timestamp - 24 * 60 * 60
                    * SECONDS_TO_MILLISECONDS_CONVERSION_RATE);
            Date endDate = new Date(timestamp + 365L * 26 * 60 * 60
                    * SECONDS_TO_MILLISECONDS_CONVERSION_RATE);
            X509v3CertificateBuilder certGen = new JcaX509v3CertificateBuilder(subjectName,
                    BigInteger.ONE, startDate, endDate, subjectName, pubKey);
            JcaX509ExtensionUtils x509ExtUtils = new JcaX509ExtensionUtils();
            certGen.addExtension(X509Extension.subjectKeyIdentifier, false,
                    (ASN1Encodable) x509ExtUtils.createSubjectKeyIdentifier(pubKey));
            certGen.addExtension(X509Extension.authorityKeyIdentifier, false,
                    (ASN1Encodable) x509ExtUtils.createAuthorityKeyIdentifier(pubKey));
            certGen.addExtension(X509Extension.keyUsage, false, new KeyUsage(
                    KeyUsage.digitalSignature | KeyUsage.keyCertSign));
            certGen.addExtension(X509Extension.extendedKeyUsage, false, new ExtendedKeyUsage(
                    KeyPurposeId.id_kp_clientAuth));
            certGen.addExtension(X509Extension.subjectAlternativeName, false, new GeneralNames(
                    new GeneralName[] {
                            new GeneralName(GeneralName.iPAddress, ipAddress),
                            new GeneralName(GeneralName.uniformResourceIdentifier,
                                    ImsModule.IMS_USER_PROFILE.getPublicUri())
                    }));
            certGen.addExtension(X509Extension.basicConstraints, false, new BasicConstraints(true));

            ContentSigner sigGen = new SimpleContentSignerBuilder().build(privKey);

            JcaX509CertificateConverter certConv = new JcaX509CertificateConverter();
            certChain[0] = certConv.getCertificate(certGen.build(sigGen));

            setClientCertificateFingerprint(certChain[0]);

            ks.setEntry(CLIENT_CERT_ALIAS, new KeyStore.PrivateKeyEntry(privKey, certChain),
                    new KeyStore.PasswordProtection(KEYSTORE_PASSWORD.toCharArray()));

            saveKeyStoreToFile(ks);

            if (sLogger.isActivated()) {
                sLogger.debug("Client certificate " + CLIENT_CERT_ALIAS + " for IP address "
                        + ipAddress + " with fingerprint " + getClientCertificateFingerprint()
                        + " added");
            }
        } catch (KeyStoreException e) {
            throw new CertificateException(new StringBuilder(
                    "Unable to create client certificate : ").append(CLIENT_CERT_ALIAS)
                    .append(" for IP address : ").append(ipAddress).toString(), e);

        } catch (NoSuchAlgorithmException e) {
            throw new CertificateException(new StringBuilder(
                    "Unable to create client certificate : ").append(CLIENT_CERT_ALIAS)
                    .append(" for IP address : ").append(ipAddress).toString(), e);

        } catch (UnrecoverableEntryException e) {
            throw new CertificateException(new StringBuilder(
                    "Unable to create client certificate : ").append(CLIENT_CERT_ALIAS)
                    .append(" for IP address : ").append(ipAddress).toString(), e);

        } catch (CertIOException e) {
            throw new CertificateException(new StringBuilder(
                    "Unable to create client certificate : ").append(CLIENT_CERT_ALIAS)
                    .append(" for IP address : ").append(ipAddress).toString(), e);

        } catch (OperatorCreationException e) {
            throw new CertificateException(new StringBuilder(
                    "Unable to create client certificate : ").append(CLIENT_CERT_ALIAS)
                    .append(" for IP address : ").append(ipAddress).toString(), e);
        }
    }

    /**
     * Create the RCS keystore
     * 
     * @throws KeyStoreException
     * @@throws IOException
     */
    private static void createKeyStore() throws KeyStoreException, IOException {
        if (sKeyStore != null) {
            return;
        }
        try {
            sKeyStore = KeyStore.getInstance(getKeystoreType());
            // Changed by Deutsche Telekom
            synchronized (KeyStoreManager.class) {
                sKeyStore.load(null, KEYSTORE_PASSWORD.toCharArray());
            }
            // Changed by Deutsche Telekom
            saveKeyStoreToFile(sKeyStore);
        } catch (NoSuchAlgorithmException e) {
            throw new KeyStoreException("Unable to create key store!", e);

        } catch (CertificateException e) {
            throw new KeyStoreException("Unable to create key store!", e);

        }
    }

    /**
     * Check if a certificate is in the keystore
     * 
     * @param alias certificate alias
     * @return True if available
     * @throws KeyStoreException
     * @throws IOException
     */
    // Changed by Deutsche Telekom
    private static boolean isCertificateEntry(String alias) throws KeyStoreException, IOException {
        return sKeyStore.isCertificateEntry(alias);
    }

    /**
     * Add a certificate or all certificates in folder in the keystore
     * 
     * @param certificateFile Uri
     * @throws KeyStoreException
     * @throws IOException
     */
    // Changed by Deutsche Telekom
    private static void addCertificates(Uri certificateFile) throws KeyStoreException, IOException {
        InputStream inStream = null;
        try {
            // Changed by Deutsche Telekom
            KeyStore ks = loadKeyStoreFromFile();
            final String certPath = certificateFile.getPath();
            File pathFile = new File(certPath);
            if (pathFile.isDirectory()) {
                File[] certificates = pathFile.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String filename) {
                        return filename.endsWith(RcsSettingsData.CERTIFICATE_FILE_TYPE);
                    }
                });

                if (certificates != null) {
                    for (File file : certificates) {
                        CertificateFactory cf = CertificateFactory.getInstance("X.509");
                        inStream = new FileInputStream(file);
                        X509Certificate cert = (X509Certificate) cf.generateCertificate(inStream);
                        CloseableUtils.close(inStream);
                        ks.setCertificateEntry(buildCertificateAlias(certificateFile), cert);
                        // Changed by Deutsche Telekom
                        saveKeyStoreToFile(ks);
                    }
                }
            } else {
                if (certPath.endsWith(RcsSettingsData.CERTIFICATE_FILE_TYPE)) {
                    CertificateFactory cf = CertificateFactory.getInstance("X.509");
                    inStream = new FileInputStream(certPath);
                    X509Certificate cert = (X509Certificate) cf.generateCertificate(inStream);
                    ks.setCertificateEntry(buildCertificateAlias(certificateFile), cert);
                    // Changed by Deutsche Telekom
                    saveKeyStoreToFile(ks);
                }
            }
        } catch (CertificateException e) {
            throw new KeyStoreException(new StringBuilder("Adding certificate ")
                    .append(certificateFile).append(" failed!").toString(), e);

        } catch (FileNotFoundException e) {
            throw new KeyStoreException(new StringBuilder("Adding certificate ")
                    .append(certificateFile).append(" failed!").toString(), e);

        } finally {
            CloseableUtils.close(inStream);
        }
    }

    /**
     * Build alias from path
     * 
     * @param path File path
     * @return Alias
     */
    private static String buildCertificateAlias(Uri certFile) {
        File file = new File(certFile.getPath());
        String filename = file.getName();
        long lastModified = file.lastModified();
        int lastDotPosition = filename.lastIndexOf('.');
        StringBuilder alias = new StringBuilder();
        if (lastDotPosition > 0) {
            alias.append(filename.substring(0, lastDotPosition));
        } else {
            alias.append(filename);
        }
        return alias.append(lastModified).toString();
    }

    /**
     * Returns the fingerprint of a certificate
     * 
     * @param cert Certificate
     * @param algorithm hash algorithm to be used
     * @return String as xx:yy:zz
     * @throws NoSuchAlgorithmException
     * @throws CertificateEncodingException
     */
    public static String getCertFingerprint(Certificate cert, String algorithm)
            throws NoSuchAlgorithmException, CertificateEncodingException {
        if (sLogger.isActivated()) {
            sLogger.debug("Getting " + algorithm + " fingerprint for certificate: "
                    + cert.toString());
        }
        MessageDigest md = MessageDigest.getInstance(algorithm);
        md.update(cert.getEncoded());
        return hexify(md.digest());
    }

    /**
     * Hexify a byte array
     * 
     * @param bytes Byte array
     * @return String
     */
    private static String hexify(byte bytes[]) {
        char[] hexDigits = {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
        };
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

    /**
     * Returns whether own certificates are used
     * 
     * @param rcsSettings
     * @return True if own certificates are used
     * @throws KeyStoreException
     * @throws IOException
     */
    public static boolean isOwnCertificateUsed(RcsSettings rcsSettings) throws KeyStoreException,
            IOException {
        String certRoot = rcsSettings.getTlsCertificateRoot();
        if (TextUtils.isEmpty(certRoot)) {
            return false;
        }
        return isCertificateEntry(buildCertificateAlias(Uri.parse(certRoot)));
    }

    // Changed by Deutsche Telekom
    /**
     * Returns keystore from file
     * 
     * @return KeyStore
     * @throws KeyStoreException
     * @throws IOException
     */
    private static KeyStore loadKeyStoreFromFile() throws KeyStoreException, IOException {
        if (sKeyStore == null) {
            FileInputStream fis = null;
            try {
                String keyStorePath = getKeystore().getPath();
                File file = new File(keyStorePath);
                if (!file.exists()) {
                    throw new KeyStoreException(new StringBuilder(
                            "Loading of key store failed, Reason : ").append(keyStorePath)
                            .append(" does not exist!").toString());
                }
                fis = new FileInputStream(keyStorePath);
                sKeyStore = KeyStore.getInstance(getKeystoreType());
                synchronized (KeyStoreManager.class) {
                    sKeyStore.load(fis, KEYSTORE_PASSWORD.toCharArray());
                }
            } catch (NoSuchAlgorithmException e) {
                throw new KeyStoreException("Loading of key store from file failed!", e);

            } catch (CertificateException e) {
                throw new KeyStoreException("Loading of key store from file failed!", e);

            } finally {
                CloseableUtils.close(fis);
            }
        }
        return sKeyStore;
    }

    // Changed by Deutsche Telekom
    /**
     * Saves keystore in file
     * 
     * @throws IOException
     * @throws
     * @throws KeyStoreException
     */
    private static void saveKeyStoreToFile(KeyStore ks) throws KeyStoreException, IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(getKeystore().getPath());
            synchronized (KeyStoreManager.class) {
                ks.store(fos, KEYSTORE_PASSWORD.toCharArray());
            }
        } catch (FileNotFoundException e) {
            throw new KeyStoreException("Saving of key store to file failed!", e);

        } catch (NoSuchAlgorithmException e) {
            throw new KeyStoreException("Saving of key store to file failed!", e);

        } catch (CertificateException e) {
            throw new KeyStoreException("Saving of key store to file failed!", e);

        } finally {
            CloseableUtils.close(fos);
        }
    }
}
