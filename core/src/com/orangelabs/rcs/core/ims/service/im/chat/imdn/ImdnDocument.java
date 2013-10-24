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
package com.orangelabs.rcs.core.ims.service.im.chat.imdn;

/**
 * IMDN document
 * 
 * @author jexa7410
 */
public class ImdnDocument {
	/**
	 * MIME type
	 */
	public static final String MIME_TYPE = "message/imdn+xml";
	
	/**
	 * Delivery status delivered
	 */
	public static final String DELIVERY_STATUS_DELIVERED = "delivered";
	
	/**
	 * Delivery status displayed
	 */
	public static final String DELIVERY_STATUS_DISPLAYED = "displayed";
	
	/**
	 * Delivery status failed
	 */
	public static final String DELIVERY_STATUS_FAILED = "failed";
	
	/**
	 * Delivery status error
	 */
	public static final String DELIVERY_STATUS_ERROR = "error";

	/**
	 * Delivery status forbidden
	 */
	public static final String DELIVERY_STATUS_FORBIDDEN = "forbidden";

	/**
	 * Namespace value
	 */
	public static final String IMDN_NAMESPACE = "imdn <urn:ietf:params:imdn>";
	
	/**
	 * Disposition notification header positive delivery value
	 */
	public static final String POSITIVE_DELIVERY = "positive-delivery";
		
	/**
	 * Disposition notification header display value
	 */
	public static final String DISPLAY = "display";
	
	/**
	 * Content-Disposition header notification value
	 */
	public static final String NOTIFICATION = "notification";

	/**
	 * Message ID
	 */
	private String msgId = null;
	
	/**
	 * Status
	 */
	private String status = null;
	
	/**
	 * Constructor
	 */
	public ImdnDocument() {
	}

	/**
	 * Get message ID
	 * 
	 * @return Message ID
	 */
	public String getMsgId() {
		return msgId;
	}

	/**
	 * Set message ID
	 * 
	 * @param msgId Message ID
	 */
	public void setMsgId(String msgId) {
		this.msgId = msgId;
	}
	
	/**
	 * Get delivery status
	 * 
	 * @return Status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * Set delivery status
	 * 
	 * @param status Status
	 */
	public void setStatus(String status) {
		this.status = status;
	}
}
