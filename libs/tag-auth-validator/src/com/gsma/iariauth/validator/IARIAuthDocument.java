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

package com.gsma.iariauth.validator;

import com.gsma.contrib.javax.xml.crypto.dsig.XMLSignature;
import com.gsma.iariauth.validator.dsig.SignatureInfo;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * A class encapsulating relevant properties of an IARI Authorization document
 */
public class IARIAuthDocument {

    /**
     * The type of this IARI Authorization
     */
    public static enum AuthType {
        SELF_SIGNED
    }

    /**
     * Public members
     */
    public AuthType authType = AuthType.SELF_SIGNED;
    public String iari;
    public String packageName;
    public String packageSigner;
    public SignatureInfo signature;

    /**
     * Public methods
     */
    public String toString() {
        StringBuffer details = new StringBuffer();
        if (authType != null) {
            details.append("authType=");
            details.append(authType.name());
            details.append('\n');
        }

        if (iari != null) {
            details.append("iari=");
            details.append(iari);
            details.append('\n');
        }

        if (packageName != null) {
            details.append("packageName=");
            details.append(packageName);
            details.append('\n');
        }

        if (packageSigner != null) {
            details.append("packageSigner=");
            details.append(packageSigner);
            details.append('\n');
        }

        if (signature != null) {
            details.append(signature);
            details.append('\n');
        }

        return details.toString();
    }

    /**
     * Read an IARI Authorization document from an InputStream. It is the caller's responsibility to
     * close the stream after this method has returned.
     * 
     * @param is
     * @return status, indicating whether or not processing was successful. See ProcessingResult for
     *         result values.
     */
    public Artifact read(InputStream is) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse(is);
        } catch (SAXException e) {
            return new Artifact("Unexpected exception parsing IARI Authorization: "
                    + e.getLocalizedMessage());
        } catch (ParserConfigurationException e) {
            return new Artifact("Unexpected exception parsing IARI Authorization: "
                    + e.getLocalizedMessage());
        } catch (IOException e) {
            return new Artifact("Unexpected exception reading IARI Authorization: "
                    + e.getLocalizedMessage());
        }

        /* check encoding */
        String encoding = doc.getXmlEncoding();
        if (encoding != null && !encoding.equalsIgnoreCase(Constants.UTF8)) {
            return new Artifact(
                    "Invalid IARI authorization: iari-authorization is not encoded with UTF-8");
        }

        /* find the iari-authorizqtion element */
        NodeList authElements = doc.getElementsByTagNameNS(Constants.IARI_AUTH_NS,
                Constants.IARI_AUTH_ELT);
        if (authElements.getLength() != 1) {
            return new Artifact(
                    "Invalid IARI authorization: invalid number of iari-authorization elements");
        }
        Element authElement = (Element) authElements.item(0);
        if (authElement != doc.getDocumentElement()) {
            return new Artifact(
                    "Invalid IARI authorization: iari-authorization is not the root element");
        }

        /* find the iari element */
        NodeList iariElements = doc.getElementsByTagNameNS(Constants.IARI_AUTH_NS,
                Constants.IARI_ELT);
        if (iariElements.getLength() != 1) {
            return new Artifact("Invalid IARI authorization: invalid number of iari elements");
        }
        iariNode = (Element) iariElements.item(0);
        if (iariNode.getParentNode() != authElement) {
            return new Artifact(
                    "Invalid IARI authorization: iari must be a child of iari-authorization");
        }
        iariNode.setIdAttribute(Constants.ID, true);
        iari = iariNode.getTextContent();

        /* find the package-name element if present */
        NodeList nameElements = doc.getElementsByTagNameNS(Constants.IARI_AUTH_NS,
                Constants.PACKAGE_NAME_ELT);
        int nameElementCount = nameElements.getLength();
        if (nameElementCount > 1) {
            return new Artifact(
                    "Invalid IARI authorization: invalid number of package-name elements");
        }
        if (nameElementCount == 1) {
            packageNameNode = (Element) nameElements.item(0);
            if (packageNameNode.getParentNode() != authElement) {
                return new Artifact(
                        "Invalid IARI authorization: package-name must be a child of iari-authorization");
            }
            packageNameNode.setIdAttribute(Constants.ID, true);
            packageName = packageNameNode.getTextContent();
        }

        /* find the package-signer element */
        NodeList signerElements = doc.getElementsByTagNameNS(Constants.IARI_AUTH_NS,
                Constants.PACKAGE_SIGNER_ELT);
        if (signerElements.getLength() != 1) {
            return new Artifact(
                    "Invalid IARI authorization: invalid number of package-signer elements");
        }
        packageSignerNode = (Element) signerElements.item(0);
        if (packageSignerNode.getParentNode() != authElement) {
            return new Artifact(
                    "Invalid IARI authorization: package-signer must be a child of iari-authorization");
        }
        packageSignerNode.setIdAttribute(Constants.ID, true);
        packageSigner = packageSignerNode.getTextContent();

        /* find the Signature element */
        NodeList signatureElements = doc.getElementsByTagNameNS(XMLSignature.XMLNS,
                Constants.SIGNATURE_ELT);
        if (signatureElements.getLength() != 1) {
            return new Artifact(
                    "Invalid IARI authorization: invalid number of ds:Signature elements");
        }
        signatureNode = (Element) signatureElements.item(0);
        if (signatureNode.getParentNode() != authElement) {
            return new Artifact(
                    "Invalid IARI authorization: package-signer must be a child of iari-authorization");
        }

        return null;
    }

    public Document getDocument() {
        return doc;
    }

    public Element getIariNode() {
        return iariNode;
    }

    public Element getPackageNameNode() {
        return packageNameNode;
    }

    public Element getPackageSignerNode() {
        return packageSignerNode;
    }

    public Element getSignatureNode() {
        return signatureNode;
    }

    /**
     * Private
     */
    private Document doc;
    private Element iariNode;
    private Element packageNameNode;
    private Element packageSignerNode;
    private Element signatureNode;
}
