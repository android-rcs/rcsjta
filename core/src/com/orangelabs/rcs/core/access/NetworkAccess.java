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

package com.orangelabs.rcs.core.access;

/**
 * Abstract network access
 * 
 * @author jexa7410
 */
public abstract class NetworkAccess {
	
    /**
     * Local IP address given to the network access
     */
	protected String ipAddress = null;

	/**
	 * Type of access
	 */
	protected String type = null;
	
	/**
	 * Constructor
	 */
	public NetworkAccess() {
	}

	/**
	 * Return the local IP address
	 * 
	 * @return IP address
	 */
	public String getIpAddress() {
		return ipAddress;
	}

	/**
	 * Return the type of access
	 * 
	 * @return Type
	 */
	public abstract String getType();

	/**
	 * Return the network name
	 * 
	 * @return Name
	 */
	public abstract String getNetworkName();	
	
	/**
     * Connect to the network access
     * 
     * @param ipAddress Local IP address
     */
    public abstract void connect(String ipAddress);
    
	/**
     * Disconnect from the network access
     */
    public abstract void disconnect();
    
}
