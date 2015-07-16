package com.gsma.iariauth.util;

public class Constants {
	/* error codes */
	public static final int OK = 0;
	public static final int GENERAL_ERR = 1;
	public static final int FILE_NOT_FOUND_ERR = 2;
	public static final int TEMPLATE_FORMAT_ERR = 3;
	public static final int KEYSTORE_LOAD_ERR = 4;
	public static final int CERTIFICATE_LOAD_ERR = 5;
	public static final int KEY_LOAD_ERR = 6;
	public static final int CRL_LOAD_ERR = 7;
	public static final int CERTIFICATE_READ_ERR = 8;
	public static final int CRL_READ_ERR = 9;
	public static final int IO_ERR = 10;
	public static final int SIGNATURE_CREATE_ERR = 11;
	public static final int INTERNAL_ERR = 12;

	public static final String STANDALONE_IARI_PREFIX = "urn:urn-7:3gpp-application.ims.iari.rcs.ext.ss.";
	public static final String DEFAULT_DEST_FILE = "iari-authorization.xml";
}
