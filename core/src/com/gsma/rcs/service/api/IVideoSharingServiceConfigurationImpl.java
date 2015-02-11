
package com.gsma.rcs.service.api;

import android.os.RemoteException;

import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.services.rcs.sharing.video.IVideoSharingServiceConfiguration;

/**
 * A class that implements interface to allow access to video sharing service configuration from API
 * 
 * @author yplo6403
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
