package com.gsma.iariauth.validator.dsig;

import java.security.PublicKey;
import com.gsma.contrib.javax.xml.crypto.KeySelector;

/**
 * An interface encapsulating the state of an ongoing
 * signature and certificate path validation
 *
 */
public interface ValidationContext {
	/**
	 * Get KeySelector
	 */
	public KeySelector getKeySelector();

	/**
	 * Get signing key
	 */
	public PublicKey getSigningKey();

	/**
	 * Get the result of key verification
	 */
	public int getVerificationResult();

	/**
	 * Get the root certificate if valid
	 */
	public CertificateInfo getRootCert();

	/**
	 * Get the entity certificate if valid
	 */
	public CertificateInfo getEntityCert();

	/**
	 * Get the valid policies for the certificate path
	 */
	public String[] getPolicies();

	/**
	 * Get the whole certificate path
	 */
	public CertificateInfo[] getCertificatePath();
}
