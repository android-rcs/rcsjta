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

package com.orangelabs.rcs.service.api;

import android.os.RemoteException;

import com.gsma.services.rcs.CommonServiceConfiguration.MessagingMethod;
import com.gsma.services.rcs.ICommonServiceConfiguration;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.ContactUtils;

/**
 * 
 * A class that implements interface to allow access to common service configuration from APIs
 * @author yplo6403
 *
 */
public class CommonServiceConfigurationImpl extends ICommonServiceConfiguration.Stub {

	private final RcsSettings mRcsSettings;
	
	/**
	 * Constructor
	 * 
	 */
	public CommonServiceConfigurationImpl() {
		mRcsSettings = RcsSettings.getInstance();
	}

	@Override
	public int getDefaultMessagingMethod() throws RemoteException {
		return mRcsSettings.getDefaultMessagingMethod().toInt();
	}

	@Override
	public int getMessagingUX() throws RemoteException {
		return mRcsSettings.getMessagingMode().toInt();
	}

	@Override
	public ContactId getMyContactId() throws RemoteException {
		String myContact = mRcsSettings.getUserProfileImsUserName();
		return ContactUtils.createContactId(myContact);
	}

	@Override
	public String getMyDisplayName() throws RemoteException {
		return mRcsSettings.getUserProfileImsDisplayName();
	}

	@Override
	public boolean isConfigValid() throws RemoteException {
		return mRcsSettings.isConfigurationValid();
	}

	@Override
	public void setDefaultMessagingMethod(int method) throws RemoteException {
		MessagingMethod messagingMethod = MessagingMethod.valueOf(method);
		mRcsSettings.setDefaultMessagingMethod(messagingMethod);
	}

	@Override
	public void setMyDisplayName(String name) throws RemoteException {
		mRcsSettings.setUserProfileImsDisplayName(name);
	}

}
