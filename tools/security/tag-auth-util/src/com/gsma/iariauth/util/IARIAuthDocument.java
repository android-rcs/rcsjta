package com.gsma.iariauth.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

import com.gsma.iariauth.signer.SignatureConstants;

public class IARIAuthDocument {

	/**
	 * The type of this IARI Authorization
	 */
	public static enum AuthType { STANDALONE, RANGE }

	public IARIAuthDocument() {}

	public int initAsDefault(AuthType authType) {
		this.authType = authType;
		String templatePath = (authType == AuthType.STANDALONE) ? DEFAULT_TEMPLATE_STANDALONE : DEFAULT_TEMPLATE_RANGE;
		InputStream is = jarResourceLoader.getResourceAsStream(templatePath);
		if(is == null && (new File(templatePath)).exists()) {
			try { is = new FileInputStream(templatePath); } catch (FileNotFoundException e) {}
		}
		if(is == null) {
			error = "Internal error: unable to find template file";
			return Constants.INTERNAL_ERR;
		}
		return read(is);
	}

	public int read(String templatePath) {
		File templateFile = new File(templatePath);
		if(!templateFile.exists()) {
			error = "Unable to find specified template file: "+templatePath;
			return Constants.FILE_NOT_FOUND_ERR;
		}
		if(!templateFile.isFile()) {
			error = "Specified template location is not a file: "+templatePath;
			return Constants.FILE_NOT_FOUND_ERR;
		}
		FileInputStream is = null;
		try {
			is = new FileInputStream(templateFile);
			int readErr = read(is);
			if(readErr != Constants.OK) {
				error = "Unable to read template";
				return readErr;
			}
		} catch(IOException e) {
			error = e.getLocalizedMessage();
			return Constants.IO_ERR;
		} finally {
			if(is != null) {
				try { is.close(); } catch(IOException e) {}
			}
		}

		return Constants.OK;
	}

	int read(InputStream is) {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			dbf.setFeature("http://apache.org/xml/features/dom/defer-node-expansion", false );
			DocumentBuilder db = dbf.newDocumentBuilder();
			doc = db.parse(is);
		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
			error = "Unexpected exception reading template: " + pce.getLocalizedMessage();
			return Constants.GENERAL_ERR;
		} catch (SAXException se) {
			se.printStackTrace();
			error = "Unexpected exception reading template: " + se.getLocalizedMessage();
			return Constants.GENERAL_ERR;
		} catch (IOException ioe) {
			error = "Unexpected exception reading template file: " + ioe.getLocalizedMessage();
			return Constants.IO_ERR;
		}

		/* obtain iari from template or given params */
		NodeList iariNodes = doc.getElementsByTagNameNS(SignatureConstants.IARI_AUTH_NS, SignatureConstants.IARI_ELT);
		int iariNodeCount = iariNodes.getLength();
		if(iariNodeCount == 0) {
			error = "Template does not contain iari element";
			return Constants.TEMPLATE_FORMAT_ERR;
		}
		if(iariNodeCount > 1) {
			error = "Template contains multiple iari elements";
			return Constants.TEMPLATE_FORMAT_ERR;
		}
		iariNode = (Element) iariNodes.item(0);
		iariNode.setIdAttribute(SignatureConstants.ID, true);
		String templateIari = iariNode.getTextContent();
		if(templateIari != null && !templateIari.isEmpty()) {
			iari = templateIari;
		}

		/* obtain iari range from template or given params */
		NodeList rangeNodes = doc.getElementsByTagNameNS(SignatureConstants.IARI_AUTH_NS, SignatureConstants.IARI_RANGE_ELT);
		int iariRangeCount = rangeNodes.getLength();
		if(iariRangeCount > 1) {
			error = "Template contains multiple iari elements";
			return Constants.TEMPLATE_FORMAT_ERR;
		}
		if(iariRangeCount == 1) {
			rangeNode = (Element) rangeNodes.item(0);
			rangeNode.setIdAttribute(SignatureConstants.ID, true);
			String templateRange = rangeNode.getTextContent();
			if(templateRange != null && !templateRange.isEmpty()) {
				range = templateRange;
			}
		}
		authType = (range == null) ? AuthType.STANDALONE : AuthType.RANGE;

		/* obtain package name from template or given params */
		NodeList packageNameNodes = doc.getElementsByTagNameNS(SignatureConstants.IARI_AUTH_NS, SignatureConstants.PACKAGE_NAME_ELT);
		int packageNameCount = packageNameNodes.getLength();
		if(packageNameCount > 1) {
			error = "Template contains multiple package-name elements";
			return Constants.TEMPLATE_FORMAT_ERR;
		}
		if(packageNameCount == 1) {
			packageNameNode = (Element) packageNameNodes.item(0);
			packageNameNode.setIdAttribute(SignatureConstants.ID, true);
			String templatePackageName = packageNameNode.getTextContent();
			if(templatePackageName != null && !templatePackageName.isEmpty()) {
				packageName = templatePackageName;
			}
		}

		/* obtain package signer from template or given params */
		NodeList packageSignerNodes = doc.getElementsByTagNameNS(SignatureConstants.IARI_AUTH_NS, SignatureConstants.PACKAGE_SIGNER_ELT);
		int packageSignerCount = packageSignerNodes.getLength();
		if(packageSignerCount == 0) {
			error = "Template does not contain package-signer element";
			return Constants.TEMPLATE_FORMAT_ERR;
		}
		if(packageSignerCount > 1) {
			error = "Template contains multiple package-signer elements";
			return Constants.TEMPLATE_FORMAT_ERR;
		}
		packageSignerNode = (Element) packageSignerNodes.item(0);
		packageSignerNode.setIdAttribute(SignatureConstants.ID, true);
		String templatePackageSigner = packageSignerNode.getTextContent();
		if(templatePackageSigner != null && !templatePackageSigner.isEmpty()) {
			packageSigner = templatePackageSigner;
		}

		return Constants.OK;
	}

	public int write(String destPath) {
		FileOutputStream os = null;
		try {
			if(destPath == null) {
				destPath = Constants.DEFAULT_DEST_FILE;
			}
			File destFile = new File(this.destPath = destPath);
			if(destFile.exists() && !destFile.isFile()) {
				error = "Specified template location is not a file";
				return Constants.FILE_NOT_FOUND_ERR;
			}
			File parentDir = destFile.getParentFile();
			if(parentDir != null && !parentDir.exists()) {
				parentDir.mkdirs();
			}
			os = new FileOutputStream(destFile);
			write(os);
		} catch(IOException ioe) {
			error = ioe.getLocalizedMessage();
			return Constants.IO_ERR;
		} finally {
			if(os != null) {
				try { os.flush(); os.close(); } catch(IOException e) {}
			}
		}

		return Constants.OK;
	}

	int write(OutputStream os) {
		LSSerializer writer = impl.createLSSerializer();
		LSOutput output = impl.createLSOutput();
		output.setByteStream(os);
		writer.write(doc, output);

		return Constants.OK;
	}

	public Document getDocument() { return doc; }
	public String getError() { return error; }
	public String getIari() { return iari; }
	public String getRange() { return range; }
	public AuthType getAuthType() { return authType; }
	public String getPackageName() { return packageName; }
	public String getPackageSigner() { return packageSigner; }
	public String getDestPath() { return destPath; }
	public Element getIariNode() { return iariNode; }
	public Element getRangeNode() { return rangeNode; }
	public Element getPackageNameNode() { return packageNameNode; }
	public Element getPackageSignerNode() { return packageSignerNode; }

	public void setIari(String iari) {
		iariNode.setTextContent(this.iari = iari);
	}

	public void setRange(String range) {
		rangeNode.setTextContent(this.range = range);
		authType = AuthType.RANGE;
	}

	public void setPackageName(String packageName) {
		packageNameNode.setTextContent(this.packageName = packageName);
	}

	public void setPackageSigner(String packageSigner) {
		packageSignerNode.setTextContent(this.packageSigner = packageSigner);
	}

	public String toString() {
		StringBuffer details = new StringBuffer("[IARIAuthDocument");
		details.append("\niari=" + iari);
		if(range != null)
			details.append("\nrange=" + range);
		if(packageName != null)
			details.append("\npackageName=" + packageName);
		if(packageSigner != null)
			details.append("\npackageSigner=" + packageSigner);
		details.append(']');

		return details.toString();
	}

	private Document doc;
	private String error;
	private String iari;
	private String range;
	private AuthType authType;
	private String packageName;
	private String packageSigner;
	private String destPath;

	private Element iariNode;
	private Element rangeNode;
	private Element packageNameNode;
	private Element packageSignerNode;

	private static final DOMImplementationLS impl;
	private static final ClassLoader jarResourceLoader = IARIAuthDocument.class.getClassLoader();
	private static final String DEFAULT_TEMPLATE_STANDALONE = "res/default-standalone-auth.xml";
	private static final String DEFAULT_TEMPLATE_RANGE = "res/default-range-auth.xml";

	static {
		DOMImplementationRegistry registry = null;
		try { registry = DOMImplementationRegistry.newInstance(); } catch (Exception e) {}
		impl = (DOMImplementationLS)registry.getDOMImplementation("LS");
	}
}
