/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.services.rcs.sharing.video;

import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsServiceControl;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceListener;
import com.gsma.services.rcs.RcsServiceListener.ReasonCode;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.contact.ContactId;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.IInterface;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * This class offers the main entry point to share live video during a CS call. Several applications
 * may connect/disconnect to the API. The parameter contact in the API supports the following
 * formats: MSISDN in national or international format, SIP address, SIP-URI or Tel-URI.
 * 
 * @author Jean-Marc AUFFRET
 */
public final class VideoSharingService extends RcsService {
    /**
     * API
     */
    private IVideoSharingService mApi;

    private final Map<VideoSharingListener, WeakReference<IVideoSharingListener>> mVideoSharingListeners = new WeakHashMap<VideoSharingListener, WeakReference<IVideoSharingListener>>();

    private static final String ERROR_CNX = "VideoSharing service not connected";

    private static boolean sApiCompatible = false;

    /**
     * Constructor
     * 
     * @param ctx Application context
     * @param listener Service listener
     */
    public VideoSharingService(Context ctx, RcsServiceListener listener) {
        super(ctx, listener);
    }

    /**
     * Connects to the API
     * 
     * @throws RcsPermissionDeniedException
     */
    public final void connect() throws RcsPermissionDeniedException {
        if (!sApiCompatible) {
            try {
                sApiCompatible = mRcsServiceControl.isCompatible(this);
                if (!sApiCompatible) {
                    throw new RcsPermissionDeniedException(
                            "The TAPI client version of the video sharing service is not compatible with the TAPI service implementation version on this device!");
                }
            } catch (RcsServiceException e) {
                throw new RcsPermissionDeniedException(
                        "The compatibility of TAPI client version with the TAPI service implementation version of this device cannot be checked for the video sharing service!");
            }
        }
        Intent serviceIntent = new Intent(IVideoSharingService.class.getName());
        serviceIntent.setPackage(RcsServiceControl.RCS_STACK_PACKAGENAME);
        mCtx.bindService(serviceIntent, apiConnection, 0);
    }

    /**
     * Disconnects from the API
     */
    public void disconnect() {
        try {
            mCtx.unbindService(apiConnection);
        } catch (IllegalArgumentException e) {
            // Nothing to do
        }
    }

    /**
     * Set API interface
     * 
     * @param api API interface
     */
    protected void setApi(IInterface api) {
        super.setApi(api);
        mApi = (IVideoSharingService) api;
    }

    /**
     * Service connection
     */
    private ServiceConnection apiConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            setApi(IVideoSharingService.Stub.asInterface(service));
            if (mListener != null) {
                mListener.onServiceConnected();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            setApi(null);
            if (mListener == null) {
                return;
            }
            ReasonCode reasonCode = ReasonCode.CONNECTION_LOST;
            try {
                if (!mRcsServiceControl.isActivated()) {
                    reasonCode = ReasonCode.SERVICE_DISABLED;
                }
            } catch (RcsServiceException e) {
                // Do nothing
            }
            mListener.onServiceDisconnected(reasonCode);
        }
    };

    /**
     * Returns the configuration of video sharing service
     * 
     * @return Configuration
     * @throws RcsServiceException
     */
    public VideoSharingServiceConfiguration getConfiguration() throws RcsServiceException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
        try {
            return new VideoSharingServiceConfiguration(mApi.getConfiguration());
        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }

    /**
     * Shares a live video with a contact. The parameter renderer contains the video player provided
     * by the application. An exception if thrown if there is no ongoing CS call. The parameter
     * contact supports the following formats: MSISDN in national or international format, SIP
     * address, SIP-URI or Tel-URI. If the format of the contact is not supported an exception is
     * thrown.
     * 
     * @param contact Contact identifier
     * @param player Video player
     * @return Video sharing
     * @throws RcsServiceException
     */
    public VideoSharing shareVideo(ContactId contact, VideoPlayer player)
            throws RcsServiceException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
        try {
            IVideoSharing sharingIntf = mApi.shareVideo(contact, player);
            if (sharingIntf != null) {
                return new VideoSharing(sharingIntf);
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }

    /**
     * Returns the list of video sharings in progress
     * 
     * @return List of video sharings
     * @throws RcsServiceException
     */
    public Set<VideoSharing> getVideoSharings() throws RcsServiceException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
        try {
            Set<VideoSharing> result = new HashSet<VideoSharing>();
            List<IBinder> vshList = mApi.getVideoSharings();
            for (IBinder binder : vshList) {
                VideoSharing sharing = new VideoSharing(IVideoSharing.Stub.asInterface(binder));
                result.add(sharing);
            }
            return result;
        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }

    /**
     * Returns a current video sharing from its unique ID
     * 
     * @param sharingId Sharing ID
     * @return Video sharing or null if not found
     * @throws RcsServiceException
     */
    public VideoSharing getVideoSharing(String sharingId) throws RcsServiceException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
        try {
            return new VideoSharing(mApi.getVideoSharing(sharingId));
        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }

    /**
     * Adds a listener on video sharing events
     * 
     * @param listener Listener
     * @throws RcsServiceException
     */
    public void addEventListener(VideoSharingListener listener) throws RcsServiceException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
        try {
            IVideoSharingListener rcsListener = new VideoSharingListenerImpl(listener);
            mVideoSharingListeners.put(listener, new WeakReference<IVideoSharingListener>(
                    rcsListener));
            mApi.addEventListener2(rcsListener);
        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }

    /**
     * Removes a listener on video sharing events
     * 
     * @param listener Listener
     * @throws RcsServiceException
     */
    public void removeEventListener(VideoSharingListener listener) throws RcsServiceException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
        try {
            WeakReference<IVideoSharingListener> weakRef = mVideoSharingListeners.remove(listener);
            if (weakRef == null) {
                return;
            }
            IVideoSharingListener rcsListener = weakRef.get();
            if (rcsListener != null) {
                mApi.removeEventListener2(rcsListener);
            }
        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }

    /**
     * Deletes all video sharing from history and abort/reject any associated ongoing session if
     * such exists.
     * 
     * @throws RcsServiceException
     */
    public void deleteVideoSharings() throws RcsServiceException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
        try {
            mApi.deleteVideoSharings();
        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }

    /**
     * Delete video sharing associated with a given contact from history and abort/reject any
     * associated ongoing session if such exists.
     * 
     * @param contact
     * @throws RcsServiceException
     */
    public void deleteVideoSharings(ContactId contact) throws RcsServiceException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
        try {
            mApi.deleteVideoSharings2(contact);
        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }

    /**
     * Deletes a video sharing by its sharing ID from history and abort/reject any associated
     * ongoing session if such exists.
     * 
     * @param sharingId
     * @throws RcsServiceException
     */
    public void deleteVideoSharing(String sharingId) throws RcsServiceException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
        try {
            mApi.deleteVideoSharing(sharingId);
        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }
}
