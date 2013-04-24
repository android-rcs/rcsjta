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

package com.orangelabs.rcs.core;

/**
 * Terminal information
 * 
 * @author JM. Auffret
 * @author Deutsche Telekom AG
 */
public class TerminalInfo {
	/**
	 * Product name
	 */
	private static final String productName = "OrangeLabs-RCS-client";

	/**
	 * Product version
	 */
	private static String productVersion = "v2.2";
	
	/**
	 * Returns the product name
	 * 
	 * @return Name
	 */
	public static String getProductName() {
		return productName;
	}

	/**
	 * Returns the product version
	 * 
	 * @return Version
	 */
	public static String getProductVersion() {
		return productVersion;
	}

	/**
	 * Set the product version
	 * 
	 * @param version Version
	 */
	public static void setProductVersion(String version) {
		TerminalInfo.productVersion = version;
	}

    /**
     * Returns the product name + version
     *
     * @return product information
     */
    public static String getProductInfo() {
        return productName + "/" + productVersion;
    }
}
