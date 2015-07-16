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

/**
 * Constant strings relevant to processing of IARI Auth documents
 */
public interface Constants {
    public static final String UTF8 = "UTF-8";
    public static final String ID = "Id";
    public static final String IARI_AUTH_NS = "http://gsma.com/ns/iari-authorization#";
    public static final String IARI_AUTH_ELT = "iari-authorization";
    public static final String IARI_ELT = "iari";
    public static final String PACKAGE_NAME_ELT = "package-name";
    public static final String PACKAGE_SIGNER_ELT = "package-signer";
    public static final String SIGNATURE_ELT = "Signature";
    public static final String SELF_SIGNED_IARI_PREFIX = "urn:urn-7:3gpp-application.ims.iari.rcs.ext.ss.";
}
