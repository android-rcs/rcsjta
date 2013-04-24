package com.orangelabs.rcs.platform.network;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import com.orangelabs.rcs.core.ims.security.cert.KeyStoreManager;

/**
 * Android secure socket connection
 * 
 * @author jexa7410
 */
public class AndroidSecureSocketConnection extends AndroidSocketConnection {
	/**
     * SSL factory
     */
	private static SSLSocketFactory sslSocketFactory = null;

    /**
	 * Constructor
	 */
	public AndroidSecureSocketConnection() {
		super();
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
		Socket s = getSslFactory().createSocket(remoteAddr, remotePort);
		setSocket(s);
	}

	/**
	 * Get the certificate fingerprint
	 * 
	 * @return String
	 */
	public String getFingerprint() {
		String result = null;
		SSLSocket s = (SSLSocket)getSocket();
		if ((s != null) && (s.getSession() != null)) { 
			Certificate[] certs = s.getSession().getLocalCertificates();
			if (certs.length > 0) {
				result = KeyStoreManager.getCertFingerprint(certs[0]);
			}
		}
		return result;
	}
	
	/**
	 * Returns the SSL factory instance
	 * 
	 * @return SSL factory
	 * @throws IOException
	 */
	private static synchronized SSLSocketFactory getSslFactory() throws IOException {
		if (sslSocketFactory == null) {
			try {
				String keyStoreType = KeyStoreManager.getKeystoreType();
				String keyStoreFile = KeyStoreManager.getKeystorePath();
				String trustStoreFile = KeyStoreManager.getKeystorePath();
				char[] keyStorePassword = KeyStoreManager.getKeystorePassword().toCharArray();

				SSLContext sslContext = SSLContext.getInstance("TLS");
		        String algorithm = KeyManagerFactory.getDefaultAlgorithm();
		        TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(algorithm);
		        KeyManagerFactory kmFactory = KeyManagerFactory.getInstance(algorithm);

		        SecureRandom secureRandom = new SecureRandom();
		        secureRandom.nextInt();
		        
		        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
		        keyStore.load(new FileInputStream(keyStoreFile), keyStorePassword);
		        KeyStore trustStore = KeyStore.getInstance(keyStoreType);
		        trustStore.load(new FileInputStream(trustStoreFile), keyStorePassword);
		        tmFactory.init(trustStore);
		        kmFactory.init(keyStore, keyStorePassword);

		        sslContext.init(kmFactory.getKeyManagers(), tmFactory.getTrustManagers(), secureRandom);

		        sslSocketFactory = sslContext.getSocketFactory();
			} catch(Exception e) {
				throw new IOException("Certificate exception: " + e.getMessage());
			}
		}
		return sslSocketFactory;
	}
}
