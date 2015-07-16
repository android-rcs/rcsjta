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

package com.gsma.iariauth.validator.dsig;

import com.gsma.contrib.javax.xml.crypto.Data;
import com.gsma.contrib.javax.xml.crypto.KeySelector;
import com.gsma.contrib.javax.xml.crypto.MarshalException;
import com.gsma.contrib.javax.xml.crypto.URIDereferencer;
import com.gsma.contrib.javax.xml.crypto.URIReference;
import com.gsma.contrib.javax.xml.crypto.URIReferenceException;
import com.gsma.contrib.javax.xml.crypto.XMLCryptoContext;
import com.gsma.contrib.javax.xml.crypto.XMLStructure;
import com.gsma.contrib.javax.xml.crypto.dom.DOMStructure;
import com.gsma.contrib.javax.xml.crypto.dsig.DigestMethod;
import com.gsma.contrib.javax.xml.crypto.dsig.Reference;
import com.gsma.contrib.javax.xml.crypto.dsig.SignatureMethod;
import com.gsma.contrib.javax.xml.crypto.dsig.SignatureProperties;
import com.gsma.contrib.javax.xml.crypto.dsig.SignatureProperty;
import com.gsma.contrib.javax.xml.crypto.dsig.Transform;
import com.gsma.contrib.javax.xml.crypto.dsig.XMLObject;
import com.gsma.contrib.javax.xml.crypto.dsig.XMLSignature;
import com.gsma.contrib.javax.xml.crypto.dsig.XMLSignatureException;
import com.gsma.contrib.javax.xml.crypto.dsig.XMLSignatureFactory;
import com.gsma.contrib.javax.xml.crypto.dsig.dom.DOMValidateContext;
import com.gsma.contrib.javax.xml.crypto.dsig.keyinfo.PGPData;
import com.gsma.iariauth.validator.Artifact;
import com.gsma.iariauth.validator.IARIAuthDocument.AuthType;
import com.gsma.iariauth.validator.ProcessingResult;

import org.apache.jcp.xml.dsig.internal.dom.ApacheData;
import org.apache.xml.security.keys.content.MgmtData;
import org.apache.xml.security.keys.content.SPKIData;
import org.apache.xml.security.signature.XMLSignatureInput;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.security.Provider;
import java.security.PublicKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A class that performs validation of a signature in an IARI Authorization document
 */
public class SignatureValidator {

    /*****************************************
     * External (package) API
     *****************************************/

    /**
     * Construct a SignatureValidator to validate a single signature
     * 
     * @param trustedCerts: the known and enabled trusted certificates.
     * @param signatureNode: the DOM node of the signature within the Authorization document
     * @param authType: the type of this signature.
     * @param range: the IARI range, if specified.
     */
    public SignatureValidator(Document doc, Element signatureNode, Set<String> expectedRefs,
            AuthType authType) {
        this.certMgr = new PKIXCertificateManager();
        this.doc = doc;
        this.signatureNode = signatureNode;
        this.expectedRefs = expectedRefs;
        this.authType = authType;
    }

    /**
     * Perform the validation The result is one of: STATUS_VALID: signature validation and
     * verification were both completed successfully STATUS_INVALID: signature validation failed
     * STATUS_REVOKED: signature verification failed as a result of there being a revoked
     * certificate STATUS_UNSIGNED: signature validation succeeded, but verification failed because
     * it was not possible to build a path (such as expired certificate, no suitable trust anchor,
     * etc)
     */
    public int validate() {
        /* all validation checks */
        int result = validateXMLSignature();
        if (result == ProcessingResult.STATUS_OK) {
            result = validateSignatureProperties();
            if (result == ProcessingResult.STATUS_OK) {
                result = validateReferences();
            }
        }

        /* get result of signature verification */
        if (result == ProcessingResult.STATUS_OK) {
            result = validationContext.getVerificationResult();
            if (result == ProcessingResult.STATUS_VALID_HAS_ANCHOR) {
                signatureInfo = SignatureInfo.create(authType, signatureId,
                        validationContext.getRootCert(), validationContext.getEntityCert(),
                        validationContext.getCertificatePath(), validationContext.getPolicies());
            }
        }
        certMgr.releaseContext(validationContext);
        return result;
    }

    public int getStatus() {
        return status;
    }

    public Artifact getError() {
        return error;
    }

    public SignatureInfo getSignatureInfo() {
        return signatureInfo;
    }

    /*****************************************
     * Perform Core Validation
     *****************************************/

    private int validateXMLSignature() {

        /* locate the provider */
        String providerName = System.getProperty("jsr105Provider",
                "org.apache.jcp.xml.dsig.internal.dom.XMLDSigRI");
        XMLSignatureFactory factory;
        try {
            factory = XMLSignatureFactory.getInstance("DOM", (Provider) Class.forName(providerName)
                    .newInstance());
        } catch (InstantiationException e) {
            return ProcessingResult.STATUS_INTERNAL_ERROR;
        } catch (IllegalAccessException e) {
            return ProcessingResult.STATUS_INTERNAL_ERROR;
        } catch (ClassNotFoundException e) {
            return ProcessingResult.STATUS_INTERNAL_ERROR;
        }

        /*
         * ASSERTION The implementation MUST ensure that each ds:Signature property required by this
         * specification meets the syntax requirements of [XMLDSIG-Properties].
         */
        DOMValidateContext domValidateContext = null;
        try {
            validationContext = certMgr.getValidationContext(authType);
            KeySelector keySelector = validationContext.getKeySelector();
            domValidateContext = new DOMValidateContext(keySelector, signatureNode);
            signature = factory.unmarshalXMLSignature(domValidateContext);
        } catch (MarshalException e) {
            error = new Artifact("Unexpected exception processing signature: "
                    + e.getLocalizedMessage());
            return (status = error.status);
        }
        domValidateContext.setURIDereferencer(new RefMapDereferencer());

        /*
         * ASSERTION A validator validates a signature according to Core Validation including
         * Reference Validation and Signature Validation, all of which are defined in [XMLDSIG11].
         * ASSERTION If a ds:KeyInfo element is present, then a validator MUST check that the
         * ds:KeyInfo conforms to the [XMLDSIG11] specification by performing Basic Path Validation
         * [RFC5280] on the signing key. A validator SHOULD perform revocation checking as
         * appropriate. ASSERTION A validator MUST support processing X.509 v3 certificates for when
         * certificates are used in the ds:X509Data of a digital signature.
         */
        boolean sigValidity = false;
        try {
            sigValidity = signature.validate(domValidateContext);
        } catch (XMLSignatureException e) {
            error = new Artifact("Unexpected exception checking signature validity: "
                    + e.getLocalizedMessage());
            return (status = error.status);
        }

        if (!sigValidity) {
            error = new Artifact("Invalid signature");
            return (status = error.status);
        }

        /*
         * ASSERTION The implementation MUST ensure that the ds:SignatureMethod algorithm used in
         * the ds:SignatureValue element is one of the signature algorithms. The ds:Signature MUST
         * be produced using a key of the recommended key length or stronger (meaning larger than
         * 2048 bits).
         */
        PublicKey signingKey = validationContext.getSigningKey();
        if (!checkKeyLength(signingKey)) {
            error = new Artifact("Signing key too short");
            return (status = error.status);
        }
        return ProcessingResult.STATUS_OK;
    }

    /*****************************************
     * Validate Signature Properties
     *****************************************/

    @SuppressWarnings("unchecked")
    private int validateSignatureProperties() {

        SignatureProperties signatureProperties = null;
        String profileURI = null;
        String roleURI = null;
        int signaturePropertiesCount = 0;
        int profileCount = 0;
        int roleCount = 0;
        int identifierCount = 0;

        /*
         * ASSERTION The implementation MUST ensure that the Algorithm attribute of the
         * ds:CanonicalizationMethod element is one of the canonicalization algorithms.
         */
        String c14nAlgorithm = signature.getSignedInfo().getCanonicalizationMethod().getAlgorithm();

        if (!isContained(SignatureConstants.C14N_ALGORITHMS, c14nAlgorithm)) {
            error = new Artifact("Invalid signature canolicalisation algorithm");
            return (status = error.status);
        }

        /*
         * ASSERTION The implementation MUST ensure that this ds:Object element contains a single
         * ds:SignatureProperties element that contains a different ds:SignatureProperty element for
         * each property required by this specification. ASSERTION The implementation MUST ensure
         * that a signature includes a ds:Object element within the ds:Signature element. This
         * ds:Object element MUST have an Id attribute that is referenced by a ds:Reference element
         * within the signature ds:SignedInfo element.
         */
        List<XMLObject> objectsList = signature.getObjects();
        int obCount = objectsList.size();
        if (obCount == 0) {
            error = new Artifact("Signature Properties object element missing");
            return (status = error.status);
        }
        /* find the SignatureProperties */
        for (int i = 0; i < objectsList.size(); i++) {
            XMLObject ob = objectsList.get(i);
            List<XMLStructure> contentList = ob.getContent();
            int contentCount = contentList.size();
            if (contentCount > 0) {
                for (int j = 0; j < contentCount; j++) {
                    XMLStructure cont = contentList.get(j);
                    if (cont instanceof SignatureProperties) {
                        signaturePropertiesCount++;
                        propertiesObject = ob;
                        propertiesId = ob.getId();
                        signatureProperties = (SignatureProperties) cont;
                        continue;
                    }
                }
            }
        }
        if (propertiesObject == null || propertiesId == null || signatureProperties == null) {
            error = new Artifact("SignatureProperties element missing");
            return (status = error.status);
        }

        /*
         * ASSERTION The implementation MUST ensure that this ds:Object element contains a single
         * ds:SignatureProperties element that contains a different ds:SignatureProperty element for
         * each property required by this specification.
         */
        if (signaturePropertiesCount != 1) {
            error = new Artifact("Multiple SignatureProperties elements present");
            return (status = error.status);
        }

        /* find all of the SignatureProperty elements */
        List<SignatureProperty> propertyList = signatureProperties.getProperties();
        for (int i = 0; i < propertyList.size(); i++) {
            SignatureProperty prop = propertyList.get(i);
            List<XMLStructure> propContent = prop.getContent();

            for (int j = 0; j < propContent.size(); j++) {
                XMLStructure contentOb = propContent.get(j);
                if (contentOb instanceof DOMStructure) {
                    Node propNode = ((DOMStructure) contentOb).getNode();
                    String nsURI = propNode.getNamespaceURI();
                    if (nsURI != null && nsURI.equals(SignatureConstants.SIG_PROPERTY_NS)) {
                        String nodeName = propNode.getLocalName();
                        if (nodeName.equals(SignatureConstants.SIG_PROPERTY_PROFILE_NAME)) {
                            Node URINode = propNode.getAttributes().getNamedItem(
                                    SignatureConstants.SIG_PROPERTY_URI_NAME);
                            if (URINode != null) {
                                profileCount++;
                                profileURI = URINode.getNodeValue();
                                continue;
                            }
                        }
                        if (nodeName.equals(SignatureConstants.SIG_PROPERTY_ROLE_NAME)) {
                            Node URINode = propNode.getAttributes().getNamedItem(
                                    SignatureConstants.SIG_PROPERTY_URI_NAME);
                            if (URINode != null) {
                                roleCount++;
                                roleURI = URINode.getNodeValue();
                                continue;
                            }
                        }
                        if (nodeName.equals(SignatureConstants.SIG_PROPERTY_IDENTIFIER_NAME)) {
                            signatureId = propNode.getTextContent();
                            identifierCount++;
                            continue;
                        }
                    }
                }
            }
        }

        /*
         * ASSERTION If a validator finds that a file entry matching the naming convention for a
         * distributor signature that does not contain a dsp:Role signature property having the URI
         * for a distributor role, then the validator MUST treat the signature as being in error.
         * ASSERTION If a validator finds that a file entry matching the naming convention for an
         * author signature that does not contain a dsp:Role signature property having the URI for a
         * author role, then the validator MUST treat the signature as being in error.
         */
        String expectedRole = SignatureConstants.SIG_PROPERTY_ROLE_SELF_SIGNED;
        if (roleCount != 1 || roleURI == null || !roleURI.equals(expectedRole)) {
            error = new Artifact("Missing or unexpected Role signature property");
            return (status = error.status);
        }

        /*
         * ASSERTION Tests that each signature contains a dsp:Profile signature properties element
         * with the correct URI attribute value.
         */
        if (profileCount != 1 || profileURI == null
                || !profileURI.equals(SignatureConstants.SIG_PROPERTY_PROFILE_URI)) {
            error = new Artifact("Missing or unexpected Profile signature property");
            return (status = error.status);
        }

        /*
         * ASSERTION Tests that each signature contains a dsp:Identifier signature properties
         * element compliant with XMLDSIG-Properties. To pass a validator must fail to validate this
         * document if there are two dsp:Identifier elements.
         */
        if (identifierCount != 1) {
            error = new Artifact("Missing or unexpected Identifier signature property");
            return (status = error.status);
        }

        /*
         * ASSERTION A validator MUST support the RSAwithSHA256 and DSAwithSHA1 signature
         * algorithms. ASSERTION An implementation SHOULD support the ECDSAwithSHA256 signature
         * algorithm. ASSERTION The implementation MUST ensure that the ds:SignatureMethod algorithm
         * used in the ds:SignatureValue element is one of the signature algorithms. The
         * ds:Signature MUST be produced using a key of the recommended key length or stronger
         * (meaning larger than 2048 bits).
         */
        SignatureMethod method = signature.getSignedInfo().getSignatureMethod();
        String algorithm = method.getAlgorithm();
        if (!algorithm.equals(SignatureConstants.SIG_ALGORITHM_DSAwithSHA1)
                && !algorithm.equals(SignatureConstants.SIG_ALGORITHM_RSAwithSHA256)
                && !algorithm.equals(SignatureConstants.SIG_ALGORITHM_ECDSAwithSHA256)) {
            error = new Artifact("Unexpected signature algorithm");
            return (status = error.status);
        }

        /*
         * ASSERTION If one or more certificates are in the ds:KeyInfo element, the implementation
         * MUST ensure that they are of the mandatory certificate format.
         */
        Iterator<XMLStructure> keyInfoItems = signature.getKeyInfo().getContent().iterator();
        while (keyInfoItems.hasNext()) {
            XMLStructure item = keyInfoItems.next();
            if (item instanceof PGPData || item instanceof SPKIData || item instanceof MgmtData) {
                error = new Artifact("Unexpected certificate format");
                return (status = error.status);
            }
        }
        return ProcessingResult.STATUS_OK;
    }

    /*****************************************
     * Validate References
     *****************************************/

    @SuppressWarnings("unchecked")
    private int validateReferences() {

        /*
         * ASSERTION Tests if every ds:Reference used within a signature has a URI attribute.
         * ASSERTION Tests if every ds:Reference used within a signature is a reference to known
         * element in the enclosing XML document.
         */
        refsFound = new HashSet<String>();
        List<Reference> sigReferences = signature.getSignedInfo().getReferences();
        for (int i = 0; i < sigReferences.size(); i++) {
            Reference ref = sigReferences.get(i);
            String refURI = ref.getURI();
            if (refURI == null || refURI.equals("")) {
                error = new Artifact("Missing or empty reference URI attribute");
                return (status = error.status);
            }
            if (refURI.charAt(0) != '#') {
                error = new Artifact("Unexpected external reference");
                return (status = error.status);
            }
            /* same-document reference */
            String refId = refURI.substring(1);
            if (!validateReference(refId, ref)) {
                return (status = error.status);
            }
            refsFound.add(refId);
        }

        /*
         * ASSERTION The implementation MUST ensure that a signature includes a ds:Object element
         * within the ds:Signature element. This ds:Object element MUST have an xml:id attribute
         * that is referenced by a ds:Reference element within the signature ds:SignedInfo element.
         */
        if (!refsFound.contains(propertiesId)) {
            error = new Artifact("Missing reference to Signature properties object");
            return (status = error.status);
        }

        /*
         * ASSERTION The implementation MUST ensure that a signature includes references to each of
         * the required members of the containing iari-authorization element.
         */
        if (!refsFound.containsAll(expectedRefs)) {
            error = new Artifact("Missing reference(s) to required iari-authorization child");
            return (status = error.status);
        }

        return ProcessingResult.STATUS_OK;
    }

    @SuppressWarnings("unchecked")
    private boolean validateReference(String id, Reference ref) {
        /*
         * ASSERTION The implementation MUST ensure that the Algorithm attribute of the
         * ds:digestMethod is the digest algorithm.
         */
        DigestMethod dm = ref.getDigestMethod();
        if (dm == null) {
            error = new Artifact("Missing or invalid digest method for reference");
            return false;
        }
        String alg = dm.getAlgorithm();
        if (alg == null || !alg.equals(SignatureConstants.DIGEST_ALGORITHM_SHA256)) {
            error = new Artifact("Unexpected digest algorithm for reference");
            return false;
        }

        /*
         * ASSERTION A validator MUST support [C14N11] to process a ds:Reference that specifies
         * [C14N11] as a canonicalization method. ASSERTION The implementation MUST ensure that a
         * ds:Reference to same-document XML content has a ds:Transform element child that specifies
         * the canonicalization method. Canonical XML 1.1 MUST be specified as the Canonicalization
         * Algorithm for this transform. A ds:Reference that is not to same-document XML content
         * MUST NOT have any ds:Transform elements.
         */
        List<Transform> transforms = ref.getTransforms();
        if (transforms != null && transforms.size() > 0) {
            if (transforms.size() > 1) {
                error = new Artifact("Multiple transforms for reference");
                return false;
            }
            Transform transform = transforms.get(0);
            String c14nAlgorithm = transform.getAlgorithm();
            if (c14nAlgorithm == null) {
                error = new Artifact("Missing algorithm for transform for reference");
                return false;
            }
            if (!isContained(SignatureConstants.C14N_ALGORITHMS, c14nAlgorithm)) {
                error = new Artifact("Unexpected algorithm for transform for reference");
                return false;
            }
        }

        return true;
    }

    private static boolean checkKeyLength(PublicKey key) {
        boolean result = false;
        if (key instanceof RSAPublicKey) {
            int keyLen = ((RSAPublicKey) key).getModulus().bitLength();
            if (keyLen >= 2048)
                result = true;
        } else if (key instanceof DSAPublicKey) {
            int keyLen = ((DSAPublicKey) key).getParams().getP().bitLength();
            if (keyLen >= 1024)
                result = true;
        } else if (key instanceof ECPublicKey) {
            int keyLen = ((ECPublicKey) key).getParams().getOrder().bitLength();
            if (keyLen >= 2048)
                result = true;
        }
        return result;
    }

    private static boolean isContained(String[] c14nAlgorithms, String c14nAlgorithm) {
        for (String s : c14nAlgorithms) {
            if (s.equalsIgnoreCase(c14nAlgorithm)) {
                return true;
            }
        }
        return false;
    }

    private class RefMapDereferencer implements URIDereferencer {

        // @Override
        public Data dereference(URIReference uriReference, XMLCryptoContext context)
                throws URIReferenceException {
            Data result = null;
            String refURI = uriReference.getURI();
            if (refURI == null || refURI.equals("")) {
                error = new Artifact("Invalid URI reference");
                return result;
            }
            if (refURI.charAt(0) == '#') {
                /* same-document reference */
                final Element refNode = doc.getElementById(refURI.substring(1));
                if (refNode == null) {
                    error = new Artifact("URI same-document reference to nonexistent node");
                    throw new URIReferenceException();
                }
                result = new ApacheData() {
                    public XMLSignatureInput getXMLSignatureInput() {
                        return new XMLSignatureInput(refNode);
                    }
                };
            }
            return result;
        }
    }

    private final CertificateManager certMgr;
    private final Document doc;
    private final Element signatureNode;
    private final Set<String> expectedRefs;
    private final AuthType authType;

    private XMLSignature signature;
    private XMLObject propertiesObject;
    private String propertiesId;
    private String signatureId;
    private ValidationContext validationContext;
    private Set<String> refsFound;

    private int status = ProcessingResult.STATUS_NOT_PROCESSED;
    private Artifact error;
    private SignatureInfo signatureInfo;
}
