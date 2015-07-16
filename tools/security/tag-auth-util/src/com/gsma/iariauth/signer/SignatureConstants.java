package com.gsma.iariauth.signer;

public interface SignatureConstants {
	public static final String ID                          = "Id";
	public static final String IARI_AUTH_NS                = "http://gsma.com/ns/iari-authorization#";
	public static final String IARI_ELT                    = "iari";
	public static final String IARI_RANGE_ELT              = "iari-range";
	public static final String PACKAGE_NAME_ELT            = "package-name";
	public static final String PACKAGE_SIGNER_ELT          = "package-signer";
	public static final String XML_DSIG_NS                 = "http://www.w3.org/2000/09/xmldsig#";
	public static final String XML_DSIG_NS_ID              = "ds";
	public static final String SIGNATURE_ID                = "Signature";
	public static final String SIG_ELT                     = "Signature";
	public static final String ROLE_PROPERTY               = "dsp:Role";
	public static final String IDENTIFIER_PROPERTY         = "dsp:Identifier";
	public static final String CREATED_PROPERTY            = "dsp:Created";
	public static final String PROFILE_PROPERTY            = "dsp:Profile";
	public static final String URI                         = "URI";
	public static final String PROFILE_URI                 = IARI_AUTH_NS + "profile";
	public static final String RANGE_ROLE_URI              = IARI_AUTH_NS + "role-range-owner";
	public static final String STANDALONE_ROLE_URI         = IARI_AUTH_NS + "role-iari-owner";
	public static final String SIGNATURE_PROPERTIES_URI    = "http://www.w3.org/2009/xmldsig-properties";
	public static final String SIGNATURE_PROPERTIES_PREFIX = "xmlns:dsp";
	public static final String PROFILE                     = "profile";
	public static final String IDENTIFIER                  = "identifier";
	public static final String ROLE                        = "role";
	public static final String CREATED                     = "created";
}
