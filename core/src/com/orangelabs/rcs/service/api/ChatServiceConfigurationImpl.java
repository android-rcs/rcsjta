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

import com.gsma.services.rcs.chat.IChatServiceConfiguration;
import com.orangelabs.rcs.provider.settings.RcsSettings;

/**
 * A class that implements interface to allow access to chat service configuration from API
 * 
 * @author yplo6403
 *
 */
public class ChatServiceConfigurationImpl extends IChatServiceConfiguration.Stub {

	private final RcsSettings mRcsSettings;

	/**
	 * Constructor
	 * 
	 */
	public ChatServiceConfigurationImpl() {
		mRcsSettings = RcsSettings.getInstance();
	}

	@Override
	public int getChatTimeout() throws RemoteException {
		return mRcsSettings.getChatIdleDuration();
	}

	@Override
	public int getGeolocExpirationTime() throws RemoteException {
		return mRcsSettings.getGeolocExpirationTime();
	}

	@Override
	public int getGeolocLabelMaxLength() throws RemoteException {
		return mRcsSettings.getMaxGeolocLabelLength();
	}

	@Override
	public int getGroupChatMaxParticipants() throws RemoteException {
		return mRcsSettings.getMaxChatParticipants();
	}

	@Override
	public int getGroupChatMessageMaxLength() throws RemoteException {
		return mRcsSettings.getMaxGroupChatMessageLength();
	}

	@Override
	public int getGroupChatMinParticipants() throws RemoteException {
		return mRcsSettings.getMinGroupChatParticipants();
	}

	@Override
	public int getGroupChatSubjectMaxLength() throws RemoteException {
		return mRcsSettings.getGroupChatSubjectMaxLength();
	}

	@Override
	public int getIsComposingTimeout() throws RemoteException {
		return mRcsSettings.getIsComposingTimeout();
	}

	@Override
	public int getOneToOneChatMessageMaxLength() throws RemoteException {
		return mRcsSettings.getMaxChatMessageLength();
	}

	@Override
	public boolean isChatSf() throws RemoteException {
		return mRcsSettings.isStoreForwardWarningActivated();
	}

	@Override
	public boolean isChatWarnSF() throws RemoteException {
		return mRcsSettings.isStoreForwardWarningActivated();
	}

	@Override
	public boolean isGroupChatSupported() throws RemoteException {
		return mRcsSettings.isGroupChatActivated();
	}

	@Override
	public boolean isRespondToDisplayReportsEnabled() throws RemoteException {
		return mRcsSettings.isRespondToDisplayReports();
	}

	@Override
	public boolean isSmsFallback() throws RemoteException {
		return mRcsSettings.isSmsFallbackServiceActivated();
	}

	@Override
	public void setRespondToDisplayReports(boolean enable) throws RemoteException {
		mRcsSettings.setRespondToDisplayReports(enable);
	}

}
