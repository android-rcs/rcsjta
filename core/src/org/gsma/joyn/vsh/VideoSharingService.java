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
package org.gsma.joyn.vsh;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.gsma.joyn.JoynContactFormatException;
import org.gsma.joyn.JoynService;
import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.JoynServiceListener;
import org.gsma.joyn.JoynServiceNotAvailableException;
import org.gsma.joyn.JoynServiceRegistrationListener;
import org.gsma.joyn.capability.ICapabilityService;
import org.gsma.joyn.ish.IImageSharingService;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.IInterface;

/**
 * This class offers the main entry point to share live video during a CS call.
 * Several applications may connect/disconnect to the API.
 * 
 * The parameter contact in the API supports the following formats: MSISDN in
 * national or international format, SIP address, SIP-URI or Tel-URI.
 * 
 * @author Jean-Marc AUFFRET
 */
public class VideoSharingService extends JoynService {
	/**
	 * API
	 */
	private IVideoSharingService api = null;

	/**
	 * Constructor
	 * 
	 * @param ctx
	 *            Application context
	 * @param listener
	 *            Service listener
	 */
	public VideoSharingService(Context ctx, JoynServiceListener listener) {
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
			api = null;
			if (serviceListener != null) {
				serviceListener
						.onServiceDisconnected(JoynService.Error.CONNECTION_LOST);
			}
		}
	};

	/**
	 * Registers a listener on service registration events
	 * 
	 * @param listener
	 *            Service registration listener
	 * @throws JoynServiceException
	 */
	public void addServiceRegistrationListener(
			JoynServiceRegistrationListener listener)
			throws JoynServiceException {
		if (api != null) {
			try {
				api.addServiceRegistrationListener(listener);
			} catch (Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
	}

	/**
	 * Unregisters a listener on service registration events
	 * 
	 * @param listener
	 *            Service registration listener
	 * @throws JoynServiceException
	 */
	public void removeServiceRegistrationListener(
			JoynServiceRegistrationListener listener)
			throws JoynServiceException {
		if (api != null) {
			try {
				api.removeServiceRegistrationListener(listener);
			} catch (Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
	}

	/**
	 * Returns the configuration of video sharing service
	 * 
	 * @return Configuration
	 * @throws JoynServiceException
	 */
	public VideoSharingServiceConfiguration getConfiguration()
			throws JoynServiceException {
		if (api != null) {
			try {
				return api.getConfiguration();
			} catch (Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
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
	 *            Contact
	 * @param player
	 *            Video player
	 * @param listener
	 *            Video sharing event listener
	 * @return Video sharing
	 * @throws JoynServiceException
	 * @throws JoynContactFormatException
	 */
	public VideoSharing shareVideo(String contact, VideoPlayer player,
			VideoSharingListener listener) throws JoynServiceException,
			JoynContactFormatException {
		if (api != null) {
			try {
				IVideoSharing sharingIntf = api.shareVideo(contact, player,
						listener);
				if (sharingIntf != null) {
					return new VideoSharing(sharingIntf);
				} else {
					return null;
				}
			} catch (Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
	}

	/**
	 * Returns the list of video sharings in progress
	 * 
	 * @return List of video sharings
	 * @throws JoynServiceException
	 */
	public Set<VideoSharing> getVideoSharings() throws JoynServiceException {
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
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
	}

	/**
	 * Returns a current video sharing from its unique ID
	 * 
	 * @param sharingId
	 *            Sharing ID
	 * @return Video sharing or null if not found
	 * @throws JoynServiceException
	 */
	public VideoSharing getVideoSharing(String sharingId)
			throws JoynServiceException {
		if (api != null) {
			try {
				IVideoSharing sharingIntf = api.getVideoSharing(sharingId);
				if (sharingIntf != null) {
					return new VideoSharing(sharingIntf);
				} else {
					return null;
				}
			} catch (Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
	}

	/**
	 * Returns a current video sharing from its invitation Intent
	 * 
	 * @param intent
	 *            Invitation intent
	 * @return Video sharing or null if not found
	 * @throws JoynServiceException
	 */
	public VideoSharing getVideoSharingFor(Intent intent)
			throws JoynServiceException {
		if (api != null) {
			try {
				String sharingId = intent
						.getStringExtra(VideoSharingIntent.EXTRA_SHARING_ID);
				if (sharingId != null) {
					return getVideoSharing(sharingId);
				} else {
					return null;
				}
			} catch (Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
	}

	/**
	 * Registers a new video sharing invitation listener
	 * 
	 * @param listener
	 *            New video sharing listener
	 * @throws JoynServiceException
	 */
	public void addNewVideoSharingListener(NewVideoSharingListener listener)
			throws JoynServiceException {
		if (api != null) {
			try {
				api.addNewVideoSharingListener(listener);
			} catch (Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
	}

	/**
	 * Unregisters a new video sharing invitation listener
	 * 
	 * @param listener
	 *            New video sharing listener
	 * @throws JoynServiceException
	 */
	public void removeNewVideoSharingListener(NewVideoSharingListener listener)
			throws JoynServiceException {
		if (api != null) {
			try {
				api.removeNewVideoSharingListener(listener);
			} catch (Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
	}

	@Override
	protected void setApi(IInterface api) {
		this.api = (IVideoSharingService) api;
		setGenericApi(api);
	}
}
