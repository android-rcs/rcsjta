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
package com.gsma.services.rcs;

/**
 * Rcs contact format exception. This exception is thrown when the
 * contact format is not supported or not well formatted. The supported
 * formats are:<br>
 * - Phone number in national or international format (e.g. +33xxx).<br>
 * - SIP address (eg. "John" <sip:+33xxx@domain.com>).<br>
 * - SIP-URI (e.g. sip:+33xxx@domain.com).<br>
 * - Tel-URI (eg. tel:+33xxx).
 *  
 * @author Jean-Marc AUFFRET
 */
public class RcsContactFormatException extends RuntimeException {
	static final long serialVersionUID = 1L;
	
	/**
	 * Constructor
	 */
	public RcsContactFormatException() {
		super("rcs contact format not supported");
	}
	
	/**
	 * Constructor
	 * 
	 * @param message
	 */
	public RcsContactFormatException(String message) {
		super(message);
	}
	
}
