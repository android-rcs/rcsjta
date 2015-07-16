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

import com.gsma.iariauth.validator.Constants;

/**
 * Constants relevant to the processing of a signature document.
 */
public interface SignatureConstants {

    static final String DIGEST_ALGORITHM_NAME = "SHA-256";

    static final String DOCUMENT_ENCODING_NAME = "UTF-8";

    static final String SIG_PROPERTY_NS = "http://www.w3.org/2009/xmldsig-properties";

    static final String SIG_PROPERTY_PROFILE_NAME = "Profile";
    static final String SIG_PROPERTY_ROLE_NAME = "Role";
    static final String SIG_PROPERTY_IDENTIFIER_NAME = "Identifier";
    static final String SIG_PROPERTY_URI_NAME = "URI";

    static final String SIG_PROPERTY_PROFILE_URI = Constants.IARI_AUTH_NS + "profile";
    static final String SIG_PROPERTY_ROLE_RANGE = Constants.IARI_AUTH_NS + "role-range-owner";
    static final String SIG_PROPERTY_ROLE_SELF_SIGNED = Constants.IARI_AUTH_NS + "role-iari-owner";

    static final String SIG_PROPERTY_PROFILE = "http://gsma.com/ns/iari-authorization#profile";

    static final String SIG_ALGORITHM_RSAwithSHA256 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";
    static final String SIG_ALGORITHM_DSAwithSHA1 = "http://www.w3.org/2000/09/xmldsig#dsa-sha1";
    static final String SIG_ALGORITHM_ECDSAwithSHA256 = "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha256";
    static final String DIGEST_ALGORITHM_SHA256 = "http://www.w3.org/2001/04/xmlenc#sha256";

    static final String C14N_ALGORITHM_XML11 = "http://www.w3.org/2006/12/xml-c14n11";

    static final String C14N_ALGORITHMS[] = {
        C14N_ALGORITHM_XML11
    };
}
