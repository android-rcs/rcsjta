
package com.gsma.rcs.service.api;

import android.os.RemoteException;

import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.services.rcs.upload.IFileUploadServiceConfiguration;

/**
 * A class that implements interface to allow access to file upload service configuration from API
 * 
 * @author yplo6403
 */
public class IFileUploadServiceConfigurationImpl extends IFileUploadServiceConfiguration.Stub {

    private final RcsSettings mRcsSettings;

    /**
     * Constructor
     * 
     * @param rcsSettings
     */
    public IFileUploadServiceConfigurationImpl(RcsSettings rcsSettings) {
        mRcsSettings = rcsSettings;
    }

    @Override
    public long getMaxSize() throws RemoteException {
        return mRcsSettings.getMaxImageSharingSize();
    }

}
