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
package org.gsma.joyn.ish;

/**
 * Image sharing service configuration
 * 
 * @author Jean-Marc AUFFRET
 */
public class ImageSharingServiceConfiguration {
	/**
	 * Returns the Image Size Warning configuration. It returns 0 if this
	 * value was not set by the autoconfiguration server (no need to warn).
	 * 
	 * @return Size in kilobytes 
	 */
	public long getWarnSize() {
		return 2048; // TODO RcsSettings.getInstance().getWarningMaxImageSharingSize();
	}
			
	/**
	 * Returns the Max Image Size configuration. It returns 0 if this
	 * value was not set by the autoconfiguration server.
	 * 
	 * @return Size in kilobytes
	 */
	public long getMaxSize() {
		return 4096; // TODO RcsSettings.getInstance().getMaxImageSharingSize();
	}
}
