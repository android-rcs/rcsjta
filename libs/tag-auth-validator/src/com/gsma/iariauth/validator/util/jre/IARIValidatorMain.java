/*
 * Copyright (C) 2014 GSM Association
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.gsma.iariauth.validator.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.gsma.iariauth.validator.PackageProcessor;
import com.gsma.iariauth.validator.ProcessingResult;

public final class IARIValidatorMain {

	private static class ArgOption extends Option {
		private static final long serialVersionUID = 1L;

		public ArgOption(String opt, String description) throws IllegalArgumentException {
			super(opt, true, description);
			setRequired(false);
		}
		public ArgOption(String opt, String longOpt, String description) throws IllegalArgumentException {
			super(opt, longOpt, true, description);
			setRequired(false);
		}
	}

	public static void main(String[] args) {
		String formatstr = "IARIValidator [-d <authorization document>] [-n <package name>] [-ps <package signer fingerprint>] [-pk <package signer keystore>] [-pa <package signer certificate alias>] [-pp <package signer keystore password>] -k <keystore> -p <password> [-v]";

	    HelpFormatter formatter = new HelpFormatter();
		GnuParser parser =  new GnuParser();
		Options opts = new Options();

		opts.addOption(new ArgOption("d", "document", "IARI Authorization document"));
		opts.addOption(new ArgOption("pkgname", "package-name", "package name"));
		opts.addOption(new ArgOption("pkgsigner", "package-signer", "package signer fingerprint"));
		opts.addOption(new ArgOption("pkgkeystore", "package-keystore", "package signing keystore"));
		opts.addOption(new ArgOption("pkgalias", "package-key-alias", "package signing certificate alias"));
		opts.addOption(new ArgOption("pkgstorepass", "package-keystore-pass", "package signing keystore password"));
		opts.addOption(new Option("v", "verbose", false, "verbose output"));

		CommandLine cli = null;
		try {
			 cli = parser.parse(opts, args);
		} catch (ParseException e) {
			formatter.printHelp(formatstr, opts);
            return;
		}

		boolean verbose = cli.hasOption("v");

		String packageName = cli.getOptionValue("pkgname");
		String packageSigner = cli.getOptionValue("pkgsigner");
		if(packageSigner == null) {
			String packageSignerKeystore = cli.getOptionValue("pkgkeystore");
			String packageSignerKeystoreAlias = cli.getOptionValue("pkgalias");
			String packageSignerKeystorePasswd = cli.getOptionValue("pkgstorepass");
			if(packageSignerKeystore != null) {
				if(packageSignerKeystoreAlias == null) {
					System.err.println("No alias given for package signing certificate");
					System.exit(1);
				}
				if(packageSignerKeystorePasswd == null) {
					System.err.println("No password given for package signing keystore");
					System.exit(1);
				}
				KeyStore packageKeystore = loadKeyStore(packageSignerKeystore, packageSignerKeystorePasswd);
				if(packageKeystore == null) {
					System.err.println("Unable to read package keystore");
					System.exit(1);
				}
				try {
					X509Certificate c = (X509Certificate) packageKeystore.getCertificate(packageSignerKeystoreAlias);
					if(c == null) {
						System.err.println("Unable to access package signing certificate");
						System.exit(1);
					}
					packageSigner = getFingerprint(c);
				} catch (KeyStoreException e) {
					System.err.println("Unable to access package signing certificate");
					System.exit(1);
				} catch (CertificateEncodingException e) {
					e.printStackTrace();
					System.err.println("Unable to read package signing certificate");
					System.exit(1);
				}
			}
		}

		String authDocumentPath = cli.getOptionValue("d");
		if(authDocumentPath == null) {
			System.err.println("No auth document specified");
			System.exit(1);
		}
		File authDocument = new File(authDocumentPath);
		if(!authDocument.exists() || !authDocument.isFile()) {
			System.err.println("Unable to read specified auth document");
			System.exit(1);
		}

		PackageProcessor processor = new PackageProcessor(packageName, packageSigner);
		ProcessingResult result = processor.processIARIauthorization(authDocument);
		if(result.getStatus() != ProcessingResult.STATUS_OK) {
			System.err.println("Error validating authDocument:");
			System.err.println(result.getError().toString());
			System.exit(1);
		}

		if(verbose) {
			System.out.println(result.getAuthDocument().toString());
		}
		System.exit(0);
	}

	private static KeyStore loadKeyStore(String path, String password) {
		KeyStore ks = null;
		File certKeyFile = new File(path);
		if(!certKeyFile.exists() || !certKeyFile.isFile()) {
			return null;
		}
		char[] pass = password.toCharArray();
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

	private static String getFingerprint(X509Certificate x509Cert) throws CertificateEncodingException {
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

	private static char[] byte2hex(byte b) {
		String lookup = "0123456789ABCDEF";
		int c1 = (b & 0xf0) >> 4;
		int c2 = b & 0xf;
		return new char[]{lookup.charAt(c1), lookup.charAt(c2)};		
	}

	/* static init */
	private static final String FINGERPRINT_DIGEST_ALG = "SHA-1";
	private static Provider bcProvider = new BouncyCastleProvider();

	static {
		org.apache.xml.security.Init.init();
		Security.addProvider(bcProvider);
	}
}
