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
package com.orangelabs.rcs.provisioning;

/**
 * Provisioning info
 *  
 * @author jexa7410
 */
public class ProvisioningInfo {
	/**
	 * Version of the provisioning document
	 */
    private String version = null;
	
	/**
	 * Validity of the provisioning document
	 */
    private long validity = 0L;
	
	/**
	 * Title for terms and conditions
	 */
    private String title = null;
	
	/**
	 * Message for terms and conditions 
	 */
    private String message = null;
	
	/**
	 * Accept button for terms and conditions
	 */
    private boolean acceptBtn = false;
	
	/**
	 * Reject button for terms and conditions
	 */
    private boolean rejectBtn = false;

    /**
     * Set version
     *
     * @param version
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Set valaidity
     *
     * @param validity
     */
    public void setValidity(long validity) {
        this.validity = validity;
    }

    /**
     * Set title
     *
     * @param title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Set message
     *
     * @param message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Set AcceptBtn
     *
     * @param acceptBtn
     */
    public void setAcceptBtn(boolean acceptBtn) {
        this.acceptBtn = acceptBtn;
    }

    /**
     * Set RejectBtn
     *
     * @param rejectBtn
     */
    public void setRejectBtn(boolean rejectBtn) {
        this.rejectBtn = rejectBtn;
    }

    /**
     * Get version
     *
     * @return version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Get validity
     *
     * @return validity
     */
    public long getValidity() {
        return validity;
    }

    /**
     * Get title
     *
     * @return title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Get message
     *
     * @return message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Get acceptBtn
     *
     * @return acceptBtn
     */
    public boolean getAcceptBtn() {
        return acceptBtn;
    }

    /**
     * Get rejectBtn
     *
     * @return rejectBtn
     */
    public boolean getRejectBtn() {
        return rejectBtn;
    }
}
