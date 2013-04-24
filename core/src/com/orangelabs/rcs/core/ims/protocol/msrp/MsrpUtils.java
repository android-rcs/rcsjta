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
package com.orangelabs.rcs.core.ims.protocol.msrp;

/**
 * MSRP utility functions
 * 
 * @author jexa7410
 */
public class MsrpUtils {
	/**
	 * Get the chunk size
	 *
	 * @param header MSRP header
	 * @return Size in bytes or 0 for "*" range
	 */
	public static int getChunkSize(String header) {
		if (header == null) {
			return -1;
		}
		int index1 = header.indexOf("-");
		int index2 = header.indexOf("/");
		if ((index1 != -1) && (index2 != -1)) {
			try {
				int lowByte = Integer.parseInt(header.substring(0, index1));
                String highByteString = header.substring(index1+1, index2);
                if (highByteString.equals("*")) {
                    return 0;
                }
				int highByte = Integer.parseInt(highByteString);
				return (highByte - lowByte) + 1;
			} catch (NumberFormatException e) {
				return -1;
			}
		}
		return -1;
	}
	
	/**
	 * Get the total size
	 *
	 * @param header MSRP header
	 * @return Size in bytes
	 */
	public static int getTotalSize(String header) {
		if (header == null) {
			return -1;
		}
		int index = header.indexOf("/");
		if (index != -1) {
			try {
				return Integer.parseInt(header.substring(index+1));
			} catch (NumberFormatException e) {
				return -1;
			}
		}
		return -1;
	}
}