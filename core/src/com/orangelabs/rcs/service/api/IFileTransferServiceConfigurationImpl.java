/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.orangelabs.rcs.service.api;

import android.os.RemoteException;

import com.gsma.services.rcs.ft.FileTransferServiceConfiguration.ImageResizeOption;
import com.gsma.services.rcs.ft.IFileTransferServiceConfiguration;
import com.orangelabs.rcs.provider.settings.RcsSettings;

/**
 * A class that implements interface to allow access to file transfer service configuration from API
 *
 * @author yplo6403
 *
 */
public class IFileTransferServiceConfigurationImpl extends IFileTransferServiceConfiguration.Stub {
	private final RcsSettings mRcsSettings;

	/**
	 * @param rcsSettings
	 */
	public IFileTransferServiceConfigurationImpl(RcsSettings rcsSettings) {
		mRcsSettings = rcsSettings;
	}

	@Override
	public void setImageResizeOption(int option) throws RemoteException {
		mRcsSettings.setImageResizeOption(ImageResizeOption.valueOf(option));
	}

	@Override
	public void setAutoAcceptInRoaming(boolean enable) throws RemoteException {
		mRcsSettings.setFileTransferAutoAcceptedInRoaming(enable);
	}

	@Override
	public void setAutoAccept(boolean enable) throws RemoteException {
		mRcsSettings.setFileTransferAutoAccepted(enable);
	}

	@Override
	public boolean isGroupFileTransferSupported() throws RemoteException {
		return mRcsSettings.getMyCapabilities().isFileTransferHttpSupported() && mRcsSettings.isGroupChatActivated();
	}

	@Override
	public boolean isAutoAcceptModeChangeable() throws RemoteException {
		return mRcsSettings.isFtAutoAcceptedModeChangeable();
	}

	@Override
	public boolean isAutoAcceptInRoamingEnabled() throws RemoteException {
		return mRcsSettings.isFileTransferAutoAcceptedInRoaming();
	}

	@Override
	public boolean isAutoAcceptEnabled() throws RemoteException {
		return mRcsSettings.isFileTransferAutoAccepted();
	}

	@Override
	public long getWarnSize() throws RemoteException {
		return mRcsSettings.getWarningMaxFileTransferSize();
	}

	@Override
	public long getMaxSize() throws RemoteException {
		return mRcsSettings.getMaxFileTransferSize();
	}

	@Override
	public int getMaxFileTransfers() throws RemoteException {
		return mRcsSettings.getMaxFileTransferSessions();
	}

	@Override
	public int getImageResizeOption() throws RemoteException {
		return mRcsSettings.getImageResizeOption().toInt();
	}

}
