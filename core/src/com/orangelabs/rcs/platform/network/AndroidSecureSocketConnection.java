package com.orangelabs.rcs.platform.network;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.KeyManager;

import com.orangelabs.rcs.core.ims.security.cert.KeyStoreManager;
import com.orangelabs.rcs.core.ims.security.cert.X509KeyManagerWrapper;
import com.orangelabs.rcs.provisioning.https.EasyX509TrustManager;
import com.orangelabs.rcs.utils.CloseableUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Android secure socket connection
 * 
 * @author jexa7410
 */
public class AndroidSecureSocketConnection extends AndroidSocketConnection {
	// Changed by Deutsche Telekom
	/**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * SSL factory
     */
	// Changed by Deutsche Telekom
    // TODO: check why this was static before
	private SSLSocketFactory mSslSocketFactory = null;

	// Changed by Deutsche Telekom
	/**
     * usage of certificate checks
     */
	private boolean mCheckCertificate = true;

	// Changed by Deutsche Telekom
	/**
     * announced fingerprint
     */
	private String mFingerprint = null;

	/**
	 * Constructor
	 */
	public AndroidSecureSocketConnection() {
		super();
	}
	
	// Changed by Deutsche Telekom
    /**
	 * Constructor
	 * 
	 * @param fingerprint 
	 */
	public AndroidSecureSocketConnection(String fingerprint) {
		super();
		mCheckCertificate = false;
		mFingerprint = fingerprint;
	}

	/**
	 * Constructor
	 * 
	 * @param socket SSL socket
	 */
	public AndroidSecureSocketConnection(SSLSocket socket) {
		super(socket);
	}

	/**
	 * Open the socket
	 * 
	 * @param remoteAddr Remote address
	 * @param remotePort Remote port
	 * @throws IOException
	 */
	public void open(String remoteAddr, int remotePort) throws IOException {
		// Changed by Deutsche Telekom
		SSLSocket s = (SSLSocket) getSslFactory().createSocket(remoteAddr, remotePort);
		// Changed by Deutsche Telekom
		try {
			s.startHandshake();
			if (mFingerprint != null) {
				// example of a fingerprint: 
				// SHA-1 4A:AD:B9:B1:3F:82:18:3B:54:02:12:DF:3E:5D:49:6B:19:E5:7C:AB
				String[] announcedFingerprintElements = mFingerprint.split(" ");
				if (announcedFingerprintElements != null
						&& announcedFingerprintElements.length > 1) {
					// use the same hashing algorithm as announced
					String usedFingerprint = getFingerprint(announcedFingerprintElements[0], s);
					// compare fingerprints and stop if not matching
					if (announcedFingerprintElements[1] != null
							&& !announcedFingerprintElements[1]
									.equals(usedFingerprint)) {
						if (logger.isActivated()) {
							logger.debug("Wrong fingerprint! " + usedFingerprint
									+ " is used while "
									+ announcedFingerprintElements[1]
									+ " is expected!");
						}
						try {
							// close the socket as an attack is assumed
							s.close();
						} catch (IOException ex) {
							if (logger.isActivated()) {
								logger.error("Closing the socket failed: ", ex);
							}
						} finally {
							s = null;
						}
					}
				}
			}
		} catch (Exception ex) {
			if (logger.isActivated()) {
				logger.error("SSL handshake failed! Error: ", ex);
			}
			s = null;
		}
		setSocket(s);
	}

	/**
	 * Get the certificate fingerprint
	 * 
	 * @param algorithm hash algorithm to be used
	 * 
	 * @return String
	 */
	// Changed by Deutsche Telekom
	public String getFingerprint(String algorithm, SSLSocket socket) {
		String result = null;
		// Changed by Deutsche Telekom
		try{
			if ((socket != null) && (socket.getSession() != null)) { 
				Certificate[] certs = socket.getSession().getPeerCertificates();
				if (logger.isActivated()) {
					logger.debug("Remote certificate chain length: " + certs.length);
				}
				if (certs.length > 0) {
					result = KeyStoreManager.getCertFingerprint(certs[0], algorithm);
				}
			} else {
				if (logger.isActivated()) {
					if (socket == null) {
						logger.error("SSL socket is null!");
					} else {
						logger.error("SSL session is null!");						
					}
				}				
			}
		} catch (Exception ex) {
			if (logger.isActivated()) {
				logger.error("Getting remote certificate fingerprint failed: ", ex);
			}
		}
		return result;
	}
	
	/**
	 * Returns the SSL factory instance
	 * 
	 * @param checkCertificate flag to indicate whether the remote certificate will be checked
	 * 
	 * @return SSL factory
	 * @throws IOException
	 */
	// Changed by Deutsche Telekom
    // TODO: check why this was static before
	private synchronized SSLSocketFactory getSslFactory() throws IOException {
        // Changed by Deutsche Telekom
	    FileInputStream ksFileInputStream = null;
        FileInputStream tsFileInputStream = null;
		if (mSslSocketFactory == null) {
			try {
				// Changed by Deutsche Telekom
				if (logger.isActivated()) {
					logger.debug("Create SSLSocketFactory");
				}
				String keyStoreType = KeyStoreManager.getKeystoreType();
				String keyStoreFile = KeyStoreManager.getKeystorePath();
				String trustStoreFile = KeyStoreManager.getKeystorePath();
				char[] keyStorePassword = KeyStoreManager.getKeystorePassword().toCharArray();

				SSLContext sslContext = SSLContext.getInstance("TLS");

				// Changed by Deutsche Telekom
				TrustManager[] tms = null;
		        KeyManager[] kms = null;
		        
		        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
                // Changed by Deutsche Telekom
		        ksFileInputStream = new FileInputStream(keyStoreFile);
		        keyStore.load(ksFileInputStream, keyStorePassword);

		        String algorithm = KeyManagerFactory.getDefaultAlgorithm();
		        
		        if (mCheckCertificate) {
			        // only use trusted certificates terminating on a well-known root CA
			        TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(algorithm);
			        
			        if(KeyStoreManager.isOwnCertificateUsed()){
			        	KeyStore trustStore = KeyStore.getInstance(keyStoreType);
			        	// Changed by Deutsche Telekom
			        	tsFileInputStream = new FileInputStream(trustStoreFile);
				        trustStore.load(tsFileInputStream, keyStorePassword);
				        tmFactory.init(trustStore);		        	
		
				        KeyManagerFactory kmFactory = KeyManagerFactory.getInstance(algorithm);
				        kmFactory.init(keyStore, keyStorePassword);			        
				        kms = kmFactory.getKeyManagers();
			        } else {
				        tmFactory.init((KeyStore)null);		        			        	
			        }
			        tms = tmFactory.getTrustManagers();
		        } else {
		        	// use an own TrustManager to allow the usage of self-signed certificates
					// Changed by Deutsche Telekom
					if (logger.isActivated()) {
						logger.debug("Use self-signed certificates");
					}
			        KeyManagerFactory kmFactory = KeyManagerFactory.getInstance(algorithm);
			        kmFactory.init(keyStore, keyStorePassword);			        
			        kms = kmFactory.getKeyManagers();
			        
					// overwrite 1st key manager with own wrapper to work around
					// certificate request for unknown issuers
			        KeyManager km = new X509KeyManagerWrapper(kms);
			        kms[0] = km;
		        	
		        	tms = new TrustManager[] { new EasyX509TrustManager(null) };
		        }
		        SecureRandom secureRandom = new SecureRandom();
		        secureRandom.nextInt();

		        sslContext.init(kms, tms, secureRandom);

		        mSslSocketFactory = sslContext.getSocketFactory();
			} catch(Exception e) {
				throw new IOException("Certificate exception: " + e.getMessage());
			} finally {
			    CloseableUtils.close(ksFileInputStream);
			    CloseableUtils.close(tsFileInputStream);
			}
		}
		return mSslSocketFactory;
	}

}
