package com.gsma.iariauth.validator;

/**
 * An interface containing the results of processing a package,
 * or of any intermediate step.
 */
public interface ProcessingResult {

	/**
	 * Processing results
	 */

	/* general - processing failures */
	public static final int STATUS_IO_ERROR         = -4;
	public static final int STATUS_CAPABILITY_ERROR = -3;
	public static final int STATUS_INTERNAL_ERROR   = -2;
	public static final int STATUS_NOT_PROCESSED    = -1;
	/* general */
	public static final int STATUS_OK               =  0;
	public static final int STATUS_INVALID          =  1;
	public static final int STATUS_DENIED           =  2;
	/* signature-related */
	public static final int STATUS_REVOKED          =  3;
	public static final int STATUS_VALID_NO_ANCHOR  =  4;
	public static final int STATUS_VALID_HAS_ANCHOR =  5;

	/**
	 * Retrieve the processing/validation status
	 */	
	public int getStatus();

	/**
	 * Retrieve the processing error Artifact. This will 
	 * reference the specific failure if document processing
	 * or signature validation or verification fail.
	 * Null if no error
	 */	
	public Artifact getError();

	/**
	 * Retrieve the processed document
	 */
	public IARIAuthDocument getAuthDocument();
}
