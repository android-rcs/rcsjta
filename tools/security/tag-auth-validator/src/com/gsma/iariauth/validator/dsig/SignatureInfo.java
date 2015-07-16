package com.gsma.iariauth.validator.dsig;

import com.gsma.iariauth.validator.IARIAuthDocument.AuthType;

/**
 * A class encapsulating the relevant details of a processed signature.
 */
public class SignatureInfo {
	
	/***********************
	 *     Public API
	 ***********************/

	static SignatureInfo create(AuthType authType, String id, CertificateInfo rootCert, CertificateInfo entityCert, CertificateInfo[] certPath, String[] policies) {
		SignatureInfo result = new SignatureInfo();
		result.authType = authType;
		result.id = id;
		result.rootCert = rootCert;
		result.entityCert = entityCert;
		result.certPath = certPath;
		return result;
	}

	public AuthType getAuthType() {
		return authType;
	}

	public String getId() {
		return id;
	}

	public CertificateInfo getRootCertificate() {
		return rootCert;
	}

	public CertificateInfo getEntityCertificate() {
		return entityCert;
	}

	public String[] getValidPolicies() {
		return policies;
	}
	
	public CertificateInfo[] getCertificatePath(){
		return certPath;
	}

	/*************************************
	 * stringify for error/warning dialogs
	 *************************************/
	
	public String toString() {
		StringBuffer details = new StringBuffer();
		details.append('\n');
		details.append(entityCert.toString());
		details.append(rootCert.toString());
		
		return details.toString();
	}

	/***********************
	 *      Internal
	 ***********************/

	private SignatureInfo() {}

	private AuthType authType;
	private String id;
	private CertificateInfo rootCert;
	private CertificateInfo entityCert;
	private String[] policies;
	private CertificateInfo[] certPath;

}
