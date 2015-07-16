/*
 * Copyright (c) 1997, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
/*
 * NOTE: this class includes snippets taken from KeyTool.java; hence
 * inclusion of the above copyright and license header.
 * Copyright (c) 2014, GSMA Limited.
 */

package com.gsma.iariauth.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.xml.security.utils.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import sun.security.x509.AlgorithmId;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateExtensions;
import sun.security.x509.CertificateIssuerName;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateSubjectName;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.GeneralName;
import sun.security.x509.GeneralNames;
import sun.security.x509.KeyIdentifier;
import sun.security.x509.SubjectAlternativeNameExtension;
import sun.security.x509.SubjectKeyIdentifierExtension;
import sun.security.x509.URIName;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

public class CertificateUtils {

	public static KeyStore loadKeyStore(String path, String storePass) {
		KeyStore ks = null;
		File certKeyFile = new File(path);
		if(!certKeyFile.exists() || !certKeyFile.isFile()) {
			return null;
		}
		char[] pass = storePass.toCharArray();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(path);
			try {
				ks = KeyStore.getInstance("jks");
				ks.load(fis, pass);
			} catch (Exception e1) {
				try {
					ks = KeyStore.getInstance("bks", bcProvider);
					ks.load(fis, pass);
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			}
		} catch(FileNotFoundException e) {
		} finally {
			try { if(fis != null) fis.close(); } catch(Throwable t) {}
		}
		return ks;
	}

	public static boolean isCA(X509Certificate cert) {
		return cert.getIssuerX500Principal().equals(cert.getSubjectX500Principal());
	}

	public static X509Certificate getCertificate(KeyStore ks, String certificateAlias) {
		X509Certificate cert = null;
		try {
			cert = (X509Certificate) ks.getCertificate(certificateAlias);
		} catch (KeyStoreException e) {
			e.printStackTrace();
		}
		return cert;
	}

	public static PrivateKey getPrivateKey(KeyStore ks, String privateKeyAlias, String passwd) {
		PrivateKey pk = null;
		try {
			pk = (PrivateKey) ks.getKey(privateKeyAlias, passwd.toCharArray());
		} catch (UnrecoverableKeyException e) {
			e.printStackTrace();
		} catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return pk;
	}

	public static String getCertificateHash(X509Certificate cert) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			String certstring = Base64.encode(digest.digest(cert.getEncoded()));
			return certstring;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (CertificateEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String getFingerprint(X509Certificate x509Cert) throws CertificateEncodingException {
		try {
			byte[] digest = MessageDigest.getInstance(FINGERPRINT_DIGEST_ALG).digest(x509Cert.getEncoded());
			StringBuffer result = new StringBuffer();
			int len = digest.length-1;
			for(int i=0; i<len; i++) {
				result.append(byte2hex(digest[i]));
				result.append(':');
			}
			result.append(byte2hex(digest[len]));
			return result.toString();
		} catch (NoSuchAlgorithmException e) { return null; /* should not be possible */ }
	}

	public static void selfSignCertificate(KeyStore keyStore, String ksName, String alias, String storePass, String keyPass, String keyAlgName, String sigAlgName, String dname, List<String> subjectAltNames, String startDate, int validityInDays) throws Exception {
		/* Get the original certificate */
		Certificate oldCert = getCertificate(keyStore, alias);
		if(oldCert == null) {
			throw new Exception("Unable to find certificate with alias " + alias);
		}
		if(!(oldCert instanceof X509Certificate)) {
			throw new Exception("Certificate with alias " + alias + " has invalid or unsupported format");
		}

		/* convert to X509CertImpl, so that we can modify selected fields
		 * (no public APIs available yet) */
		byte[] encoded = oldCert.getEncoded();
		X509CertImpl certImpl = new X509CertImpl(encoded);
		X509CertInfo certInfo = (X509CertInfo)certImpl.get(X509CertImpl.NAME + "." + X509CertImpl.INFO);

		/* Extend its validity */
		Date firstDate = getStartDate(startDate);
		Date lastDate = new Date();
		lastDate.setTime(firstDate.getTime() + validityInDays*1000L*24L*60L*60L);
		CertificateValidity interval = new CertificateValidity(firstDate, lastDate);
		certInfo.set(X509CertInfo.VALIDITY, interval);

		/* Make new serial number */
		certInfo.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(new java.util.Random().nextInt() & 0x7fffffff));

		/* Set owner and issuer fields */
		X500Name owner;
		if(dname == null) {
			/* Get the owner name from the certificate */
			owner = (X500Name)certInfo.get(X509CertInfo.SUBJECT + "." + CertificateSubjectName.DN_NAME);
		} else {
			/* Use the owner name specified at the command line */
			owner = new X500Name(dname);
			certInfo.set(X509CertInfo.SUBJECT + "." + CertificateSubjectName.DN_NAME, owner);
		}
		/* Make issuer same as owner (self-signed!) */
		certInfo.set(X509CertInfo.ISSUER + "." + CertificateIssuerName.DN_NAME, owner);

		AlgorithmId sigAlgid = AlgorithmId.get(sigAlgName);
		certInfo.set(CertificateAlgorithmId.NAME + "." + CertificateAlgorithmId.ALGORITHM, sigAlgid);

		certInfo.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));

		CertificateExtensions ext = createAltNameExtensions(
				(CertificateExtensions)certInfo.get(X509CertInfo.EXTENSIONS),
				subjectAltNames,
				oldCert.getPublicKey());
		certInfo.set(X509CertInfo.EXTENSIONS, ext);

		/* Sign the new certificate */
		X509CertImpl newCert = new X509CertImpl(certInfo);
		PrivateKey privKey = (PrivateKey)keyStore.getKey(alias, keyPass.toCharArray());
		newCert.sign(privKey, sigAlgName);

		/* Store the new certificate as a single-element certificate chain */
		char[] pass = ((keyPass != null) ? keyPass : storePass).toCharArray();
		keyStore.setKeyEntry(alias, privKey, pass, new Certificate[] { newCert } );
		saveKeyStore(keyStore, ksName, storePass);
	}

	public static void saveKeyStore(KeyStore keyStore, String ksName, String storepass) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        keyStore.store(bout, storepass.toCharArray());
        String tmpName = ksName + ".inprogress";
        File tmpFile = new File(tmpName);
        if(tmpFile.exists())
        	tmpFile.delete();
        FileOutputStream fout = null;
        try {
        	fout = new FileOutputStream(tmpFile);
            fout.write(bout.toByteArray());
        } finally {
        	if(fout != null) {
        		try {
        			fout.close();
                	tmpFile.renameTo(new File(ksName));
        		} catch(IOException ioe) {
        			tmpFile.delete();
        			throw new IOException("Unable to save modified keystore: " + ksName);
        		}
        	}
        }
	}

    /**
     * If no signature algorithm was specified at the command line,
     * we choose one that is compatible with the selected private key
     */
    public static String getCompatibleSigAlgName(String keyAlgName)
            throws Exception {
        if ("DSA".equalsIgnoreCase(keyAlgName)) {
            return "SHA1WithDSA";
        } else if ("RSA".equalsIgnoreCase(keyAlgName)) {
            return "SHA256WithRSA";
        } else if ("EC".equalsIgnoreCase(keyAlgName)) {
            return "SHA256withECDSA";
        } else {
            throw new Exception("Cannot derive signature algorithm");
        }
    }

    /**
     * If no signature algorithm was specified at the command line,
     * we choose one that is compatible with the selected private key
     */
    public static String getCompatibleKeyAlgName(String sigAlgName)
            throws Exception {
    	sigAlgName = sigAlgName.toUpperCase();
    	if(sigAlgName.contains("RSA")) {
    		return "RSA";
    	} else if(sigAlgName.contains("ECDSA")) {
    		return "EC";
    	} else if(sigAlgName.contains("DSA")) {
    		return "DSA";
    	} else {
            throw new Exception("Cannot derive key algorithm");
        }
    }

    public static String hashPublicKey(X509Certificate cert) {
		try {
			MessageDigest sha = MessageDigest.getInstance("SHA-224");
			byte[] hash = sha.digest(cert.getPublicKey().getEncoded());
			String b64 = new String(Base64.encode(hash));
			/* make URL-safe, remove trailing = */
			return b64.replace('+', '-').replace('/', '_').replace("=", "");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}   
	}

	public static char[] byte2hex(byte b) {
		String lookup = "0123456789ABCDEF";
		int c1 = (b & 0xf0) >> 4;
			int c2 = b & 0xf;
			return new char[]{lookup.charAt(c1), lookup.charAt(c2)};		
	}

	private static Date getStartDate(String s) throws IOException {
		Calendar c = new GregorianCalendar();
		if (s != null) {
			IOException ioe = new IOException("Illegal startdate value");
			int len = s.length();
			if (len == 0) {
				throw ioe;
			}
			if (s.charAt(0) == '-' || s.charAt(0) == '+') {
				// Form 1: ([+-]nnn[ymdHMS])+
				int start = 0;
				while (start < len) {
					int sign = 0;
					switch (s.charAt(start)) {
					case '+': sign = 1; break;
					case '-': sign = -1; break;
					default: throw ioe;
					}
					int i = start+1;
					for (; i<len; i++) {
						char ch = s.charAt(i);
						if (ch < '0' || ch > '9') break;
					}
					if (i == start+1) throw ioe;
					int number = Integer.parseInt(s.substring(start+1, i));
					if (i >= len) throw ioe;
					int unit = 0;
					switch (s.charAt(i)) {
					case 'y': unit = Calendar.YEAR; break;
					case 'm': unit = Calendar.MONTH; break;
					case 'd': unit = Calendar.DATE; break;
					case 'H': unit = Calendar.HOUR; break;
					case 'M': unit = Calendar.MINUTE; break;
					case 'S': unit = Calendar.SECOND; break;
					default: throw ioe;
					}
					c.add(unit, sign * number);
					start = i + 1;
				}
			} else  {
				// Form 2: [yyyy/mm/dd] [HH:MM:SS]
				String date = null, time = null;
				if (len == 19) {
					date = s.substring(0, 10);
					time = s.substring(11);
					if (s.charAt(10) != ' ')
						throw ioe;
				} else if (len == 10) {
					date = s;
				} else if (len == 8) {
					time = s;
				} else {
					throw ioe;
				}
				if (date != null) {
					if (date.matches("\\d\\d\\d\\d\\/\\d\\d\\/\\d\\d")) {
						c.set(Integer.valueOf(date.substring(0, 4)),
								Integer.valueOf(date.substring(5, 7))-1,
								Integer.valueOf(date.substring(8, 10)));
					} else {
						throw ioe;
					}
				}
				if (time != null) {
					if (time.matches("\\d\\d:\\d\\d:\\d\\d")) {
						c.set(Calendar.HOUR_OF_DAY, Integer.valueOf(time.substring(0, 2)));
						c.set(Calendar.MINUTE, Integer.valueOf(time.substring(0, 2)));
						c.set(Calendar.SECOND, Integer.valueOf(time.substring(0, 2)));
						c.set(Calendar.MILLISECOND, 0);
					} else {
						throw ioe;
					}
				}
			}
		}
		return c.getTime();
	}

    /**
     * Create X509v3 extensions from a string representation. Note that the
     * SubjectKeyIdentifierExtension will always be created non-critical besides
     * the extension requested in the <code>extstr</code> argument.
     *
     * @param ext the original extensions, can be null, used for -selfcert
     * @param extstrs -ext values, Read keytool doc
     * @param pkey the public key for the certificate
     * @param akey the public key for the authority (issuer)
     * @return the created CertificateExtensions
     */
    private static CertificateExtensions createAltNameExtensions(
            CertificateExtensions ext,
            List <String> subjectAltNames,
            PublicKey pkey) throws Exception {

        if (ext == null) ext = new CertificateExtensions();
        try {
            for(String name : subjectAltNames) {
                GeneralNames gnames = new GeneralNames();
                gnames.add(new GeneralName(new URIName(name)));
                ext.set(SubjectAlternativeNameExtension.NAME, new SubjectAlternativeNameExtension(false, gnames));
            }
            ext.set(SubjectKeyIdentifierExtension.NAME, new SubjectKeyIdentifierExtension(new KeyIdentifier(pkey).getIdentifier()));
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        return ext;
    }

	private static final String FINGERPRINT_DIGEST_ALG = "SHA-1";
	private static Provider bcProvider = new BouncyCastleProvider();

	static {
		org.apache.xml.security.Init.init();
		Security.addProvider(bcProvider);
	}
}
