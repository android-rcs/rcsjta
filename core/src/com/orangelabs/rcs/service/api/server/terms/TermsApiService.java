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

package com.orangelabs.rcs.service.api.server.terms;

import android.content.Intent;

import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.service.api.client.terms.ITermsApi;
import com.orangelabs.rcs.service.api.client.terms.TermsApiIntents;
import com.orangelabs.rcs.service.api.server.ServerApiException;
import com.orangelabs.rcs.service.api.server.ServerApiUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Terms & conditions API service
 * 
 * @author jexa7410
 */
public class TermsApiService extends ITermsApi.Stub {
	/**
	 * The logger
	 */
	private Logger logger = Logger.getLogger(this.getClass().getName());

	/**
	 * Constructor
	 */
	public TermsApiService() {
		if (logger.isActivated()) {
			logger.info("Terms API is loaded");
		}
	}

	/**
	 * Close API
	 */
	public void close() {
	}
   
	/**
     * Accept terms and conditions via SIP
     *
	 * @param id Request id
	 * @param pin PIN
	 * @return Boolean result
     * @throws ServerApiException
     */
    public boolean acceptTerms(String id, String pin) throws ServerApiException {	
		if (logger.isActivated()) {
			logger.info("Accept terms");
		}

    	// Check permission
		ServerApiUtils.testPermission();

		// Test IMS connection
		ServerApiUtils.testIms();
		
		try {
			// Accept terms
			return Core.getInstance().getTermsConditionsService().acceptTerms(id, pin);
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
	}

	/**
     * Reject terms and conditions via SIP
     *
	 * @param id Request id
	 * @param pin PIN
	 * @return Boolean result
     * @throws ServerApiException
     */
    public boolean rejectTerms(String id, String pin) throws ServerApiException {	
		if (logger.isActivated()) {
			logger.info("Reject terms");
		}

    	// Check permission
		ServerApiUtils.testPermission();

		// Test IMS connection
		ServerApiUtils.testIms();
		
		try {
			// Decline terms
			return Core.getInstance().getTermsConditionsService().rejectTerms(id, pin);
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
	}

    /**
     * Receive terms and conditions request via SIP
     * 
     * @param remote Remote server
     * @param id Request ID
     * @param type Type of request
     * @param pin PIN number requested
     * @param subject Subject
     * @param text Text
     * @param acceptButtonLabel 
     * @param rejectButtonLabel 
     * @param timeout 
     */
    public void receiveTermsRequest(String remote, String id, String type,
            boolean pin, String subject, String text, String acceptButtonLabel,
            String rejectButtonLabel, int timeout) {
		// Broadcast intent related to the received request
    	Intent intent = new Intent(TermsApiIntents.TERMS_SIP_REQUEST);
    	intent.putExtra("id", id);
    	intent.putExtra("type", type);
    	intent.putExtra("pin", pin);
    	intent.putExtra("subject", subject);
    	intent.putExtra("text", text);
        intent.putExtra("acceptButtonLabel", acceptButtonLabel);
        intent.putExtra("rejectButtonLabel", rejectButtonLabel);
        intent.putExtra("timeout", timeout);
    	AndroidFactory.getApplicationContext().sendBroadcast(intent);
    }

    /**
     * Receive terms and conditions ack via SIP
     * 
     * @param remote Remote server
     * @param id Request ID
     * @param status Status
     * @param subject Subject
     * @param text Text
     */
    public void receiveTermsAck(String remote, String id, String status, String subject, String text) {
		// Broadcast intent related to the received request
    	Intent intent = new Intent(TermsApiIntents.TERMS_SIP_ACK);
    	intent.putExtra("id", id);
    	intent.putExtra("status", status);
    	intent.putExtra("subject", subject);
    	intent.putExtra("text", text);
    	AndroidFactory.getApplicationContext().sendBroadcast(intent);
    }

    /**
     * Receive user notification via SIP
     *
     * @param remote Remote server
     * @param id Request ID
     * @param subject Subject
     * @param text Text
     * @param okButtonLabel Label of OK button
     */
    public void receiveUserNotification(String remote, String id, String subject, String text, String okButtonLabel) {
        // Broadcast intent related to the received request
        Intent intent = new Intent(TermsApiIntents.TERMS_SIP_USER_NOTIFICATION);
    	intent.putExtra("id", id);
        intent.putExtra("subject", subject);
        intent.putExtra("text", text);
        intent.putExtra("okButtonLabel", okButtonLabel);
        AndroidFactory.getApplicationContext().sendBroadcast(intent);
    }
}
