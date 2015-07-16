package com.gsma.iariauth.signer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.TimeZone;

import org.apache.xml.security.c14n.CanonicalizationException;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.c14n.InvalidCanonicalizerException;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.keys.content.X509Data;
import org.apache.xml.security.signature.ObjectContainer;
import org.apache.xml.security.signature.SignatureProperties;
import org.apache.xml.security.signature.SignatureProperty;
import org.apache.xml.security.signature.SignedInfo;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.signature.XMLSignatureException;
import org.apache.xml.security.transforms.TransformationException;
import org.apache.xml.security.transforms.Transforms;
import org.apache.xml.security.utils.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.gsma.iariauth.util.CertificateUtils;
import com.gsma.iariauth.util.Constants;
import com.gsma.iariauth.util.IARIAuthDocument;
import com.gsma.iariauth.util.IARIAuthDocument.AuthType;

public final class IARISigner {

	/* public members */
	public String identifier;
	public String ksPath;
	public String storePass;
	public String keyPass;
	public String alias;
	public String subksPath;
	public String crlPath;

	public IARISigner(IARIAuthDocument authDoc) {
		this.authDoc = authDoc;
		this.doc = authDoc.getDocument();
	}

	public int sign() {
		/* obtain signing key */
		if(ksPath == null) {
			error = "keystore path must be provided";
			return Constants.GENERAL_ERR;
		}
		if(alias == null) {
			error = "alias should must be provided";
			return Constants.GENERAL_ERR;
		}

		KeyStore ks = CertificateUtils.loadKeyStore(ksPath, storePass);
		if(ks == null) {
			error = "Unable to load private keystore";
			return Constants.KEYSTORE_LOAD_ERR;
		}
		cert = CertificateUtils.getCertificate(ks, alias);
		if(cert == null) {
			error = "Unable to locate certificate with alias " + alias + " in keystore";
			return Constants.CERTIFICATE_LOAD_ERR;
		}

		privatekey = CertificateUtils.getPrivateKey(ks, alias, keyPass);
		if(privatekey == null) {
			error = "Unable to locate private key with alias " + alias + " in keystore";
			return Constants.KEY_LOAD_ERR;
		}

		/* obtain any sub-CA certs */
		if(subksPath != null)
			subks = CertificateUtils.loadKeyStore(subksPath, null);
		if(subks == null)
			subks = ks;

		if(identifier == null || identifier.isEmpty()) {
			identifier = DEFAULT_IDENTIFIER;
		}

		/* obtain any CRL */
		if(crlPath != null && crlPath.length() != 0) {
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(crlPath);
				CertificateFactory cf = CertificateFactory.getInstance("X.509");
				crl = (X509CRL) cf.generateCRL(fis);
			} catch (FileNotFoundException e) {
				error = "Unable to locate CRL at path " + crlPath;
				return Constants.CRL_LOAD_ERR;
			} catch (CertificateException e) {
				error = "Certificate exception processing CRL";
				return Constants.CERTIFICATE_READ_ERR;
			} catch (CRLException e) {
				error = "Certificate exception processing CRL";
				return Constants.CRL_READ_ERR;
			} finally {
				try { if(fis != null) fis.close(); } catch(Throwable t) {}
			}
		}
		
		/* add the signature content to the document */
		int err = createSignature();
		if(err != Constants.OK) {
			return err;
		}

		return Constants.OK;
	}

	public String getError() {
		return error;
	}

	private int createSignature() {
		/* create sig */
		try {
			if(privatekey.getAlgorithm().equals("RSA")) {
				xmlsig = new XMLSignature(doc, null,
						XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA256,
						Canonicalizer.ALGO_ID_C14N11_OMIT_COMMENTS);
			} else if(privatekey.getAlgorithm().equals("DSA")) {
				xmlsig = new XMLSignature(doc, null,
						XMLSignature.ALGO_ID_SIGNATURE_DSA,
						Canonicalizer.ALGO_ID_C14N11_OMIT_COMMENTS);
			}
		} catch (XMLSecurityException e) {
			e.printStackTrace();
			error = e.getMessage();
			return Constants.SIGNATURE_CREATE_ERR;
		}

		/* locate/create Signature element */
		Element sigNode = xmlsig.getElement();
		sigNode.setAttribute(SignatureConstants.ID, SignatureConstants.SIGNATURE_ID);

		NodeList sigNodes = doc.getElementsByTagNameNS(SignatureConstants.XML_DSIG_NS, SignatureConstants.SIG_ELT);
		if(sigNodes == null || sigNodes.getLength() > 1) {
			error = "Template contains multiple iari elements";
			return Constants.TEMPLATE_FORMAT_ERR;
		}
		if(sigNodes.getLength() == 1) {
			Element existingSigNode = (Element) sigNodes.item(0);
			existingSigNode.getParentNode().replaceChild(sigNode, existingSigNode);
		} else {
			doc.getDocumentElement().appendChild(sigNode);
		}

		/* add all signature refs */
		try {
			/* iari */
			Element iariNode = authDoc.getIariNode();
			String iariEltId = iariNode.getAttribute(SignatureConstants.ID);
			if(iariEltId == null || iariEltId.isEmpty()) {
				iariNode.setAttribute(SignatureConstants.ID, (iariEltId = SignatureConstants.IARI_ELT));
			}
			addElementRef(iariEltId);

			/* iari-range */
			Element rangeNode = authDoc.getRangeNode();
			if(rangeNode != null) {
				String rangeEltId = rangeNode.getAttribute(SignatureConstants.ID);
				if(rangeEltId == null || rangeEltId.isEmpty()) {
					rangeNode.setAttribute(SignatureConstants.ID, (rangeEltId = SignatureConstants.IARI_RANGE_ELT));
				}
				addElementRef(rangeEltId);
			}

			/* package name */
			Element packageNameNode = authDoc.getPackageNameNode();
			if(packageNameNode != null) {
				String nameEltId = packageNameNode.getAttribute(SignatureConstants.ID);
				if(nameEltId == null || nameEltId.isEmpty()) {
					packageNameNode.setAttribute(SignatureConstants.ID, (nameEltId = SignatureConstants.PACKAGE_NAME_ELT));
				}
				addElementRef(nameEltId);
			}

			/* package signer */
			Element packageSignerNode = authDoc.getPackageSignerNode();
			if(packageSignerNode != null) {
				String signerEltId = packageSignerNode.getAttribute(SignatureConstants.ID);
				if(signerEltId == null || signerEltId.isEmpty()) {
					packageSignerNode.setAttribute(SignatureConstants.ID, (signerEltId = SignatureConstants.PACKAGE_SIGNER_ELT));
				}
				addElementRef(signerEltId);
			}

			/* signature properties */
			createSignatureProperties();
			addElementRef("prop");

		} catch (XMLSignatureException e) {
			e.printStackTrace();
			error = e.getMessage();
			return Constants.SIGNATURE_CREATE_ERR;
		} catch (TransformationException e) {
			e.printStackTrace();
			error = e.getMessage();
			return Constants.CRL_READ_ERR;
		}

		/* add X509Data */
		try {
			X509Data x509Data = new X509Data(doc);
			X509Certificate[] certChain = getCertificateChain(cert);
			for (X509Certificate c : certChain) {
				x509Data.addCertificate(c);
			}
			if(crl != null) {
				x509Data.addCRL(crl.getEncoded());
			}
			xmlsig.getKeyInfo().add(x509Data);
		} catch (XMLSecurityException e) {
			e.printStackTrace();
			error = e.getMessage();
			return Constants.SIGNATURE_CREATE_ERR;
		} catch (CRLException e) {
			error = e.getMessage();
			return Constants.SIGNATURE_CREATE_ERR;
		}

		/* sign */
		try {
			xmlsig.sign(privatekey);
		} catch (XMLSignatureException e) {
			e.printStackTrace();
			error = e.getMessage();
			return Constants.SIGNATURE_CREATE_ERR;
		}

		return Constants.OK;
	}

	private void addElementRef(String elementId) throws TransformationException, XMLSignatureException {
		Transforms transforms = new Transforms(doc);
		transforms.addTransform(Transforms.TRANSFORM_C14N11_OMIT_COMMENTS);
		xmlsig.addDocument(targetUri(elementId), transforms, "http://www.w3.org/2001/04/xmlenc#sha256");
	}

	private ObjectContainer createSignatureProperties() throws XMLSignatureException {
		String signTypeTarget = targetUri(SignatureConstants.SIGNATURE_ID);
		String signTypeRoleURI = ((authDoc.getAuthType() == AuthType.RANGE) ? SignatureConstants.RANGE_ROLE_URI : SignatureConstants.STANDALONE_ROLE_URI);

		ObjectContainer container = new ObjectContainer(doc);
		container.setId("prop");
		SignatureProperties properties = new SignatureProperties(doc);

		/* profile */
		SignatureProperty profileProperty = new SignatureProperty(doc, signTypeTarget);
		profileProperty.setId(SignatureConstants.PROFILE);
		Element profileElement = doc.createElement(SignatureConstants.PROFILE_PROPERTY);
		profileElement.setAttribute(SignatureConstants.URI, SignatureConstants.PROFILE_URI);
		profileProperty.appendChild(profileElement);
		properties.addSignatureProperty(profileProperty);

		/* role */
		SignatureProperty roleProperty = new SignatureProperty(doc, signTypeTarget);
		roleProperty.setId(SignatureConstants.ROLE);
		Element roleElement = doc.createElement(SignatureConstants.ROLE_PROPERTY);
		roleElement.setAttribute(SignatureConstants.URI, signTypeRoleURI);
		roleProperty.appendChild(roleElement);
		properties.addSignatureProperty(roleProperty);

		/* identifier */
		SignatureProperty identifierProperty = new SignatureProperty(doc, signTypeTarget);
		identifierProperty.setId(SignatureConstants.IDENTIFIER);
		Element identifierElement = doc.createElement(SignatureConstants.IDENTIFIER_PROPERTY);
		identifierProperty.appendChild(identifierElement);
		String identifierString = "";
		identifierString = generateIdentifierString();
		identifierElement.setTextContent(identifierString);
		properties.addSignatureProperty(identifierProperty);

		/* created */
		SignatureProperty createdProperty = new SignatureProperty(doc, signTypeTarget);
		createdProperty.setId(SignatureConstants.CREATED);
		Element createdElement = doc.createElement(SignatureConstants.CREATED_PROPERTY);
		createdElement.setTextContent(getCurrentTime());
		createdProperty.appendChild(createdElement);
		properties.addSignatureProperty(createdProperty);

		/* finish */
		properties.getElement().setAttribute(SignatureConstants.SIGNATURE_PROPERTIES_PREFIX, SignatureConstants.SIGNATURE_PROPERTIES_URI);
		container.appendChild(properties.getElement());
		xmlsig.appendObject(container);
		return container;
	}

	private String getCurrentTime() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		dateFormat.setTimeZone(TimeZone.getDefault());
		String date = dateFormat.format(new Date());
		SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm:ss");
		timeFormat.setTimeZone(TimeZone.getDefault());
		String time = timeFormat.format(new Date());
		String finalDateTime = date + "T" + time + "Z";
		timeFormat.format(new Date());
		return finalDateTime;
	}

	private String generateIdentifierString() {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < identifier.length(); i++) {
			char ch = identifier.charAt(i);
			if(ch == '%') {
				if(i + 1 < identifier.length()) {
					char formatChar = identifier.charAt(i + 1);
					if(formatChar == 'c' || formatChar == 'i' || formatChar == 't' || formatChar == 's') {
						String expandedStr = getExpandedString(formatChar);
						if(expandedStr != null) {
							buffer.append(expandedStr);
						}
						i = i + 1;
					} else {
						buffer.append(ch);
						buffer.append(formatChar);
						i = i + 1;
					}
				} else {
					buffer.append(ch);
				}
			} else {
				buffer.append(ch);
			}
		}
		return buffer.toString();
	}

	private String getExpandedString(char formatChar) {
		switch (formatChar) {
		case 'c':
			return CertificateUtils.getCertificateHash(cert);
		case 'i':
			return authDoc.getIari();
		case 't':
			return getCurrentTime();
		case 's':
			return getSignedInfoHash();
		default:
		}
		return null;
	}

	private String getSignedInfoHash() {
		SignedInfo signedInfo = xmlsig.getSignedInfo();
		Element signedInfoElement = signedInfo.getElement();
		try {
			Canonicalizer canonicalizer = Canonicalizer
					.getInstance(Canonicalizer.ALGO_ID_C14N11_OMIT_COMMENTS);
			byte[] bytes = canonicalizer.canonicalizeSubtree(signedInfoElement);
			try {
				MessageDigest digest = MessageDigest.getInstance("SHA-256");
				byte[] signedInfoBytes = digest.digest(bytes);
				String signedInfoString = Base64.encode(signedInfoBytes);
				return signedInfoString;
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		} catch (InvalidCanonicalizerException e) {
			e.printStackTrace();
		} catch (CanonicalizationException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static String targetUri(String id) {
		return '#' + id;
	}

	private X509Certificate[] getCertificateChain(X509Certificate cert) {
		ArrayList<X509Certificate> list = new ArrayList<X509Certificate>();
		list.add(cert);
		while ((cert = getIssuer(cert)) != null && !CertificateUtils.isCA(cert)) {
			list.add(cert);
		}
		X509Certificate[] ret = new X509Certificate[list.size()];
		return list.toArray(ret);
	}

	private X509Certificate getIssuer(X509Certificate cert) {
		try {
			Enumeration<String> aliases = subks.aliases();
			while (aliases.hasMoreElements()) {
				String alias = aliases.nextElement();
				X509Certificate c = (X509Certificate) subks.getCertificate(alias);
				if(c.getSubjectX500Principal().equals(
						cert.getIssuerX500Principal())) {
					return c;
				}
			}
		} catch (KeyStoreException e) {
			e.printStackTrace();
		}
		return null;
	}

	/* private members */
	private IARIAuthDocument authDoc;
	private String error;
	private KeyStore subks;
	private X509Certificate cert;
	private X509CRL crl;
	private PrivateKey privatekey;
	private Document doc;
	private XMLSignature xmlsig;

	/* defaults */
	private static final String DEFAULT_IDENTIFIER = "%i:%t";

	static {
		org.apache.xml.security.Init.init();
	}
}
