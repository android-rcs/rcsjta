package com.orangelabs.rcs.service.api;

import android.os.RemoteException;

import com.gsma.services.rcs.upload.IFileUploadServiceConfiguration;
import com.orangelabs.rcs.provider.settings.RcsSettings;

/**
 * A class that implements interface to allow access to file upload service configuration from API
 * 
 * @author yplo6403
 *
 */
public class IFileUploadServiceConfigurationImpl extends IFileUploadServiceConfiguration.Stub {

	private RcsSettings mRcsSettings;

	/**
	 * Constructor
	 */
	public IFileUploadServiceConfigurationImpl() {
		mRcsSettings = RcsSettings.getInstance();
	}

	@Override
	public long getMaxSize() throws RemoteException {
		return mRcsSettings.getMaxImageSharingSize();
	}

}
