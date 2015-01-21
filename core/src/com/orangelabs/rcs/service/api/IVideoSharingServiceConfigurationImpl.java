package com.orangelabs.rcs.service.api;

import android.os.RemoteException;

import com.gsma.services.rcs.vsh.IVideoSharingServiceConfiguration;
import com.orangelabs.rcs.provider.settings.RcsSettings;

/**
 * A class that implements interface to allow access to video sharing service configuration from API
 * 
 * @author yplo6403
 *
 */
public class IVideoSharingServiceConfigurationImpl extends IVideoSharingServiceConfiguration.Stub {

	private RcsSettings mRcsSettings;

	/**
	 * Constructor
	 * 
	 * @param rcsSettings
	 */
	public IVideoSharingServiceConfigurationImpl(RcsSettings rcsSettings) {
		mRcsSettings = rcsSettings;
	}

	@Override
	public long getMaxTime() throws RemoteException {
		return mRcsSettings.getMaxVideoShareDuration();
	}

}
