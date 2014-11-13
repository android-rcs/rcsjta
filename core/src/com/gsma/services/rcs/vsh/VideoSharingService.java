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
package com.gsma.services.rcs.vsh;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.IInterface;

import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceListener;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * This class offers the main entry point to share live video during a CS call.
 * Several applications may connect/disconnect to the API.
 * 
 * The parameter contact in the API supports the following formats: MSISDN in
 * national or international format, SIP address, SIP-URI or Tel-URI.
 * 
 * @author Jean-Marc AUFFRET
 */
public class VideoSharingService extends RcsService {
	/**
	 * API
	 */
	private IVideoSharingService api;

	/**
	 * Constructor
	 * 
	 * @param ctx
	 *            Application context
	 * @param listener
	 *            Service listener
	 */
	public VideoSharingService(Context ctx, RcsServiceListener listener) {
		super(ctx, listener);
	}

	/**
	 * Connects to the API
	 */
	public void connect() {
		ctx.bindService(new Intent(IVideoSharingService.class.getName()),
				apiConnection, 0);
	}

	/**
	 * Disconnects from the API
	 */
	public void disconnect() {
		try {
			ctx.unbindService(apiConnection);
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
    	
        this.api = (IVideoSharingService)api;
    }
    
    /**
	 * Service connection
	 */
	private ServiceConnection apiConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			setApi(IVideoSharingService.Stub.asInterface(service));
			if (serviceListener != null) {
				serviceListener.onServiceConnected();
			}
		}

		public void onServiceDisconnected(ComponentName className) {
        	setApi(null);
			if (serviceListener != null) {
				serviceListener
						.onServiceDisconnected(RcsService.Error.CONNECTION_LOST);
			}
		}
	};

	/**
	 * Returns the configuration of video sharing service
	 * 
	 * @return Configuration
	 * @throws RcsServiceException
	 */
	public VideoSharingServiceConfiguration getConfiguration()
			throws RcsServiceException {
		if (api != null) {
			try {
				return api.getConfiguration();
			} catch (Exception e) {
				throw new RcsServiceException(e.getMessage());
			}
		} else {
			throw new RcsServiceNotAvailableException();
		}
	}

	/**
	 * Shares a live video with a contact. The parameter renderer contains the
	 * video player provided by the application. An exception if thrown if there
	 * is no ongoing CS call. The parameter contact supports the following
	 * formats: MSISDN in national or international format, SIP address, SIP-URI
	 * or Tel-URI. If the format of the contact is not supported an exception is
	 * thrown.
	 * 
	 * @param contact
	 *            Contact identifier
	 * @param player
	 *            Video player
	 * @return Video sharing
	 * @throws RcsServiceException
	 */
	public VideoSharing shareVideo(ContactId contact, VideoPlayer player) throws RcsServiceException {
		if (api != null) {
			try {
				IVideoSharing sharingIntf = api.shareVideo(contact, player);
				if (sharingIntf != null) {
					return new VideoSharing(sharingIntf);
				} else {
					return null;
				}
			} catch (Exception e) {
				throw new RcsServiceException(e.getMessage());
			}
		} else {
			throw new RcsServiceNotAvailableException();
		}
	}

	/**
	 * Returns the list of video sharings in progress
	 * 
	 * @return List of video sharings
	 * @throws RcsServiceException
	 */
	public Set<VideoSharing> getVideoSharings() throws RcsServiceException {
		if (api != null) {
			try {
				Set<VideoSharing> result = new HashSet<VideoSharing>();
				List<IBinder> vshList = api.getVideoSharings();
				for (IBinder binder : vshList) {
					VideoSharing sharing = new VideoSharing(
							IVideoSharing.Stub.asInterface(binder));
					result.add(sharing);
				}
				return result;
			} catch (Exception e) {
				throw new RcsServiceException(e.getMessage());
			}
		} else {
			throw new RcsServiceNotAvailableException();
		}
	}

	/**
	 * Returns a current video sharing from its unique ID
	 * 
	 * @param sharingId
	 *            Sharing ID
	 * @return Video sharing or null if not found
	 * @throws RcsServiceException
	 */
	public VideoSharing getVideoSharing(String sharingId)
			throws RcsServiceException {
		if (api != null) {
			try {
				IVideoSharing sharingIntf = api.getVideoSharing(sharingId);
				if (sharingIntf != null) {
					return new VideoSharing(sharingIntf);
				} else {
					return null;
				}
			} catch (Exception e) {
				throw new RcsServiceException(e.getMessage());
			}
		} else {
			throw new RcsServiceNotAvailableException();
		}
	}

	/**
	 * Adds a listener on video sharing events
	 * 
	 * @param listener Listener
	 * @throws RcsServiceException
	 */
	public void addEventListener(VideoSharingListener listener) throws RcsServiceException {
		if (api != null) {
			try {
				api.addEventListener2(listener);
			} catch (Exception e) {
				throw new RcsServiceException(e.getMessage());
			}
		} else {
			throw new RcsServiceNotAvailableException();
		}
	}

	/**
	 * Removes a listener on video sharing events
	 * 
	 * @param listener Listener
	 * @throws RcsServiceException
	 */
	public void removeEventListener(VideoSharingListener listener) throws RcsServiceException {
		if (api != null) {
			try {
				api.removeEventListener2(listener);
			} catch (Exception e) {
				throw new RcsServiceException(e.getMessage());
			}
		} else {
			throw new RcsServiceNotAvailableException();
		}
	}
}
