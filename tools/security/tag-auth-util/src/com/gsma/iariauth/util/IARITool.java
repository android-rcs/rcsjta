package com.gsma.iariauth.util;

import java.io.PrintStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import sun.security.tools.KeyTool;

import com.gsma.iariauth.signer.IARISigner;
import com.gsma.iariauth.util.IARIAuthDocument.AuthType;

public class IARITool {

	private static class Command extends Option {
		private static final long serialVersionUID = 1L;

		public Command(String opt, String description) throws IllegalArgumentException {
			super(opt, false, description);
			setRequired(false);
		}		
	}

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

	private static class KeytoolOption extends Option {
		private static final long serialVersionUID = 1L;

		public KeytoolOption(String opt, String description) throws IllegalArgumentException {
			this(opt, true, description);
		}
		public KeytoolOption(String opt, boolean hasArg, String description) throws IllegalArgumentException {
			super(opt, hasArg, description);
		}
	}

    public static void main(String[] args) {
        IARITool inst = new IARITool(System.out, System.err);
        System.exit(inst.run(args));
    }

    private int run(String[] args) {
		String formatstr = "IARITool";

	    HelpFormatter formatter = new HelpFormatter();
		GnuParser parser =  new GnuParser();
		Options opts = new Options();

		/******************************
		 *          Commands
		 ******************************/

		opts.addOption(new Command("init", "Generates an IARI Authorization template document with given params"));
		opts.addOption(new Command("generate", "Generates an IARI from a given or new keypair"));
		opts.addOption(new Command("sign", "Generates a signed IARIAuthorization document"));

		/******************************
		 *          Options
		 ******************************/

		opts.addOption(new ArgOption("template", "IARI Authorization document template"));
		opts.addOption(new ArgOption("iari", "IARI string"));
		opts.addOption(new ArgOption("range", "IARI range string"));
		opts.addOption(new ArgOption("pkgname", "package-name", "package name"));
		opts.addOption(new ArgOption("pkgsigner", "package-signer", "package signer fingerprint"));
		opts.addOption(new ArgOption("pkgkeystore", "package-keystore", "package signing keystore"));
		opts.addOption(new ArgOption("pkgalias", "package-key-alias", "package signing certificate alias"));
		opts.addOption(new ArgOption("pkgstorepass", "package-keystore-pass", "package signing keystore password"));
		opts.addOption(new ArgOption("dest", "destination file"));
		opts.addOption(new ArgOption("identifier", "format string for dsp:Identifier SignatureProperty"));
		opts.addOption(new ArgOption("subcakeystore", "Path of (.jks/.bks) keystore containing sub-CA intermediate certificates (not password protected)"));
		opts.addOption(new ArgOption("crl", "Path of CRL to embed in signature"));

		opts.addOption(new KeytoolOption("alias", "alias name of the entry to process"));
		opts.addOption(new KeytoolOption("destalias", "destination alias"));
		opts.addOption(new KeytoolOption("dname", "distinguished name"));
		opts.addOption(new KeytoolOption("ext", "X.509 extension"));
		opts.addOption(new KeytoolOption("keyalg", "key algorithm name"));
		opts.addOption(new KeytoolOption("keypass", "key password"));
		opts.addOption(new KeytoolOption("keysize", "key bit size"));
		opts.addOption(new KeytoolOption("keystore", "keystore name"));
		opts.addOption(new KeytoolOption("noprompt", false, "do not prompt"));
		opts.addOption(new KeytoolOption("protected", false, "password through protected mechanism"));
		opts.addOption(new KeytoolOption("providerarg", "provider argument"));
		opts.addOption(new KeytoolOption("providerclass", "provider class name"));
		opts.addOption(new KeytoolOption("providername", "provider name"));
		opts.addOption(new KeytoolOption("providerpath", "provider classpath"));
		opts.addOption(new KeytoolOption("sigalg", "signature algorithm name"));
		opts.addOption(new KeytoolOption("srcalias", "alias name of the source certificate/keypair entry for IARI"));
		opts.addOption(new KeytoolOption("srckeystore", "name of the keystore containing source certificate/keypair"));
		opts.addOption(new KeytoolOption("srcstorepass", "source keystore password"));
		opts.addOption(new KeytoolOption("srckeypass", "source certificate/keypair password"));
		opts.addOption(new KeytoolOption("startdate", "certificate validity start date time"));
		opts.addOption(new KeytoolOption("storepass", "keystore password"));
		opts.addOption(new KeytoolOption("storetype", "keystore type"));
		opts.addOption(new KeytoolOption("v", false, "verbose output"));
		opts.addOption(new KeytoolOption("validity", "validity number of days"));

		try {
			 cli = parser.parse(opts, args);
		} catch (ParseException e) {
			formatter.printHelp(formatstr, opts);
            return Constants.GENERAL_ERR;
		}

		if(cli.hasOption("providername")) {
			/* we want a custom provider, so install this now as a
			 * side-effect of an otherwise empty Keytool invocation */
			/* generate a keypair with the given arguments */
			List<String> providerArgs = new ArrayList<String>();
			addKeytoolArgs(providerArgs, new String[]{
				"providername", "providerarg", "providerclass", "providerpath"
			});
			try {
				KeyTool.main(providerArgs.toArray(new String[]{}));
			} catch (Exception e) {
				e.printStackTrace();
				return Constants.GENERAL_ERR;
			}
		}

		if(cli.hasOption("v")) {
			verbose = true;
		}

		if(cli.hasOption("init")) {
			int genErr = doInit();
			if(genErr != Constants.OK) {
				err.println(error);
				return genErr;
			}
		}

		if(cli.hasOption("generate")) {
			int genErr = doGenerate();
			if(genErr != Constants.OK) {
				err.println(error);
				return genErr;
			}
		}

		if(cli.hasOption("sign")) {
			int signErr = doSign();
			if(signErr != Constants.OK) {
				err.println(error);
				return signErr;
			}
		}

		return Constants.OK;
	}

    private void addKeytoolArgs(List<String> args, String[] expectedArgs) {
    	for(String arg : expectedArgs) {
    		if(cli.hasOption(arg)) {
				args.add('-' + arg);
				String optionValue = cli.getOptionValue(arg);
				if(optionValue != null)
					args.add(optionValue);
    		}
    	}
    }

    private int doInit() {
    	String iari = cli.getOptionValue("iari");
    	String range = cli.getOptionValue("range");
    	String packageName = cli.getOptionValue("pkgname");
    	String packageSigner = cli.getOptionValue("pkgsigner");
		String templatePath = cli.getOptionValue("template");

    	authDoc = new IARIAuthDocument();
		if(templatePath != null) {
			int readErr = authDoc.read(templatePath);
			if(readErr != Constants.OK) {
				error = authDoc.getError();
				return readErr;
			}
		} else {
			AuthType authType = (range != null) ? AuthType.RANGE : AuthType.STANDALONE;
			int initErr = authDoc.initAsDefault(authType);
			if(initErr != Constants.OK) {
				error = authDoc.getError();
				return initErr;
			}
		}

		if(iari != null)
			authDoc.setIari(iari);
		if(range != null)
			authDoc.setRange(range);
		if(packageName != null)
			authDoc.setPackageName(packageName);
		if(packageSigner != null)
			authDoc.setPackageSigner(packageSigner);

		/* write populated template */
		String destPath = cli.getOptionValue("dest");
		int writeErr = authDoc.write(destPath);
		if(writeErr != Constants.OK) {
			error = authDoc.getError();
			return writeErr;
		}

		if(verbose) {
			out.println("Written template to: " + authDoc.getDestPath());
		}

		return Constants.OK;
    }

    private int doGenerate() {
		String ksName = cli.getOptionValue("keystore");
		String storePass = cli.getOptionValue("storepass");
		String keyPass = cli.getOptionValue("keypass");
		String alias = cli.getOptionValue("alias");
		String dname = cli.getOptionValue("dname");
		String startDate = cli.getOptionValue("startdate");
		int validityInDays = Integer.parseInt(cli.getOptionValue("validity"));

		String keyAlgName, sigAlgName;
		X509Certificate x509Cert;
		KeyStore ks;

		/* we need a keystore */
		if(ksName == null) {
			error = "A keystore must be specified as a destination for IARI key/certificate";
			return Constants.GENERAL_ERR;
		}
		/* we need an alias */
		if(alias == null) {
			error = "An alias must be specified for the destination IARI key/certificate";
			return Constants.GENERAL_ERR;
		}

		/* decide if we're generating a keypair now, or we're using a given one */
		String srcKsName = cli.getOptionValue("srckeystore");
		if(srcKsName != null) {
			/* we need an alias */
			String srcStorePass = cli.getOptionValue("srcstorepass");
			String srcAlias = cli.getOptionValue("srcalias");
			if(srcAlias == null) {
				error = "An alias must be specified for the source key/certificate";
				return Constants.GENERAL_ERR;
			}
			/* get this certificate */
			ks = CertificateUtils.loadKeyStore(srcKsName, srcStorePass);
			x509Cert = CertificateUtils.getCertificate(ks, srcAlias);
			keyAlgName = x509Cert.getPublicKey().getAlgorithm();
			sigAlgName = x509Cert.getSigAlgName();
		} else {
			keyAlgName = cli.getOptionValue("keyalg");
			sigAlgName = cli.getOptionValue("sigalg");
			try {
				if(keyAlgName == null) {
					if(sigAlgName != null) {
						keyAlgName = CertificateUtils.getCompatibleKeyAlgName(sigAlgName);
					} else {
						keyAlgName = "RSA";
					}
				}
				if(sigAlgName == null) {
					sigAlgName = CertificateUtils.getCompatibleSigAlgName(keyAlgName);
				}
			} catch(Exception e) {
				error = e.getLocalizedMessage();
				return Constants.GENERAL_ERR;
			}
			/* generate a keypair with the given arguments */
			List<String> genkeyArgs = new ArrayList<String>();
			genkeyArgs.addAll(Arrays.asList(new String[]{"-genkey", "-keyalg", keyAlgName, "-sigalg", sigAlgName}));
			addKeytoolArgs(genkeyArgs, new String[]{
				"alias", "keysize", "destalias", "dname",
				"startdate", "ext","keypass", "keystore",
				"storepass","storetype", "v", "validity",
				"protected"
			});

			try {
				KeyTool.main(genkeyArgs.toArray(new String[]{}));
			} catch (Exception e) {
				error = e.getLocalizedMessage();
				return Constants.GENERAL_ERR;
			}

			/* read the certificate */
			ks = CertificateUtils.loadKeyStore(ksName, storePass);
			x509Cert = CertificateUtils.getCertificate(ks, alias);
		}

		/* generate the IARI string from the public key of the certificate */
		String iari = Constants.STANDALONE_IARI_PREFIX + CertificateUtils.hashPublicKey(x509Cert);
		List<String> subjectAltNames = Arrays.asList(new String[]{iari});
		if(verbose) {
			out.println("iari: " + iari);
		}

		/* add a SAN entry for this IARI to this certificate and sign */
		try {
			CertificateUtils.selfSignCertificate(ks, ksName, alias, storePass, keyPass, keyAlgName, sigAlgName, dname, subjectAltNames, startDate, validityInDays);
		} catch(Exception e) {
			e.printStackTrace();
			error = e.getLocalizedMessage();
			return Constants.CERTIFICATE_READ_ERR;
		}

		/* generate the template */
		if(authDoc == null) {
			authDoc = new IARIAuthDocument();
			if(cli.hasOption("template")) {
				String templatePath = cli.getOptionValue("template");
				int readErr = authDoc.read(templatePath);
				if(readErr != Constants.OK) {
					error = authDoc.getError();
					return readErr;
				}
			} else {
				int initErr = authDoc.initAsDefault(AuthType.STANDALONE);
				if(initErr != Constants.OK) {
					error = authDoc.getError();
					return initErr;
				}
			}
		}
		authDoc.setIari(iari);

		/* write populated template */
		String destPath = cli.getOptionValue("dest");
		int writeErr = authDoc.write(destPath);
		if(writeErr != Constants.OK) {
			error = authDoc.getError();
			return writeErr;
		}

		if(verbose) {
			out.println("Written template to: " + authDoc.getDestPath());
		}

		return Constants.OK;
	}

	private int doSign() {
		/* load doc */
		if(authDoc == null) {
			authDoc = new IARIAuthDocument();
			if(cli.hasOption("template")) {
				String templatePath = cli.getOptionValue("template");
				int readErr = authDoc.read(templatePath);
				if(readErr != Constants.OK) {
					error = authDoc.getError();
					return readErr;
				}
			} else {
				AuthType authType = (cli.hasOption("range")) ? AuthType.RANGE : AuthType.STANDALONE;
				authDoc.initAsDefault(authType);
			}
		}

		/* set iari */
		String templateIari = authDoc.getIari();
		String cmdIari = cli.getOptionValue("iari");
		if(templateIari != null && cmdIari != null) {
			error = "IARI is specified both in template and given params";
			return Constants.GENERAL_ERR;
		}
		if(templateIari == null && cmdIari == null) {
			error = "IARI must be specified if not specified in template";
			return Constants.GENERAL_ERR;
		}
		if(cmdIari != null) {
			authDoc.setIari(cmdIari);
		}

		/* set range */
		String templateRange = authDoc.getRange();
		String cmdRange = cli.getOptionValue("range");
		if(templateRange != null && cmdRange != null) {
			error = "Range is specified both in template and given params";
			return Constants.GENERAL_ERR;
		}
		if(cmdRange != null) {		
			authDoc.setRange(cmdRange);
		}

		/* set package name */
		String templatePackageName = authDoc.getPackageName();
		String cmdPackageName = cli.getOptionValue("pkgname");
		if(templatePackageName != null && cmdPackageName != null) {
			error = "Package name is specified both in template and given params";
			return Constants.GENERAL_ERR;
		}
		if(templatePackageName == null && cmdPackageName != null) {
			authDoc.setPackageName(cmdPackageName);
		}

		/* set package signer */
		String templatePackageSigner = authDoc.getPackageSigner();
		String cmdPackageSigner = cli.getOptionValue("pkgsigner");
		if(templatePackageSigner != null && cmdPackageSigner != null) {
			error = "Package signer is specified both in template and given params";
			return Constants.GENERAL_ERR;
		}
		if(cmdPackageSigner != null) {
			authDoc.setPackageSigner(cmdPackageSigner);
		} else if(templatePackageSigner == null) {
			/* see if package signer keystore is specified */
			String packageKeystoreName = cli.getOptionValue("pkgkeystore");
			if(packageKeystoreName == null) {
				error = "Package signer must be specified if not specified in template";
				return Constants.GENERAL_ERR;
			}
			String packageSignerKeystoreAlias = cli.getOptionValue("pkgalias");
			if(packageSignerKeystoreAlias == null) {
				error = "No alias given for package signing certificate";
				return Constants.GENERAL_ERR;
			}
			String packageSignerKeystorePasswd = cli.getOptionValue("pkgstorepass");
			if(packageSignerKeystorePasswd == null) {
				error = "No password given for package signing keystore";
				return Constants.GENERAL_ERR;
			}
			KeyStore packageKeystore = CertificateUtils.loadKeyStore(packageKeystoreName, packageSignerKeystorePasswd);
			if(packageKeystore == null) {
				error = "Unable to read package keystore";
				return Constants.KEYSTORE_LOAD_ERR;
			}
			try {
				X509Certificate c = (X509Certificate) packageKeystore.getCertificate(packageSignerKeystoreAlias);
				if(c == null) {
					error = "Unable to access package signing certificate";
					return Constants.CERTIFICATE_LOAD_ERR;
				}
				authDoc.setPackageSigner(CertificateUtils.getFingerprint(c));
			} catch (KeyStoreException e) {
				e.printStackTrace();
				error = "Unable to access package signing certificate";
				return Constants.CERTIFICATE_LOAD_ERR;
			} catch (CertificateEncodingException e) {
				e.printStackTrace();
				error = "Unable to read package signing certificate";
				return Constants.CERTIFICATE_READ_ERR;
			}
		}

		/* sign the document */
		IARISigner signer = new IARISigner(authDoc);
		signer.identifier = cli.getOptionValue("identifier");
		signer.ksPath = cli.getOptionValue("keystore");
		signer.storePass = cli.getOptionValue("storepass");
		signer.keyPass = cli.getOptionValue("keypass");
		signer.alias = cli.getOptionValue("alias");
		signer.subksPath = cli.getOptionValue("subcakeystore");
		signer.crlPath = cli.getOptionValue("crl");

		int signErr = signer.sign();
		if(signErr != Constants.OK) {
			error = signer.getError();
			return signErr;
		}

		/* write signed document */
		String destPath = cli.getOptionValue("dest");
		int writeErr = authDoc.write(destPath);
		if(writeErr != Constants.OK) {
			error = authDoc.getError();
			return writeErr;
		}

		if(verbose) {
			out.println("Written: " + authDoc.toString());
		}

		return Constants.OK;
	}

	/******************************
	 *          Private
	 ******************************/

	private PrintStream out;
	private PrintStream err;
	private String error;
	private CommandLine cli;
	private IARIAuthDocument authDoc;
	private boolean verbose;

    private IARITool(PrintStream out, PrintStream err) { this.out = out; this.err = err; }
}
