/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.orangelabs.rcs.service.api.client.terms;

/**
 * Terms API intents
 * 
 * @author jexa7410
 */
public class TermsApiIntents {
    /**
     * Terms and conditions request via SIP
	 */ 
	public final static String TERMS_SIP_REQUEST = "com.orangelabs.rcs.TERMS_SIP_REQUEST";

	/**
     * Terms and conditions acknowledgement via SIP
	 */ 
	public final static String TERMS_SIP_ACK = "com.orangelabs.rcs.TERMS_SIP_ACK";

    /**
     * Terms and conditions user notification via SIP
     */ 
    public final static String TERMS_SIP_USER_NOTIFICATION = "com.orangelabs.rcs.TERMS_SIP_USER_NOTIFICATION";
}
