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

package com.orangelabs.rcs.service.api.client.richcall;

import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.orangelabs.rcs.service.api.client.ClientApi;
import com.orangelabs.rcs.service.api.client.ClientApiException;
import com.orangelabs.rcs.service.api.client.CoreServiceNotAvailableException;
import com.orangelabs.rcs.service.api.client.media.IMediaPlayer;
import com.orangelabs.rcs.service.api.client.messaging.GeolocPush;

/**
 * Rich call API
 * 
 * @author jexa7410
 */
public class RichCallApi extends ClientApi {
	/**
	 * Core service API
	 */
	private IRichCallApi coreApi = null;

	/**
     * Constructor
     * 
     * @param ctx Application context
     */
    public RichCallApi(Context ctx) {
    	super(ctx);
    }
    
    /**
     * Connect API
     */
    public void connectApi() {
    	super.connectApi();
    	
		ctx.bindService(new Intent(IRichCallApi.class.getName()), apiConnection, 0);
    }
    
    /**
     * Disconnect API
     */
    public void disconnectApi() {
    	super.disconnectApi();
    	
    	try {
    		ctx.unbindService(apiConnection);
        } catch (IllegalArgumentException e) {
        	// Nothing to do
        }
    }
	
	/**
	 * Core service API connection
	 */
	private ServiceConnection apiConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
        	coreApi = IRichCallApi.Stub.asInterface(service);

        	// Notify event listener
        	notifyEventApiConnected();
        }

        public void onServiceDisconnected(ComponentName className) {
        	// Notify event listener
        	notifyEventApiDisconnected();

        	coreApi = null;
        }
    };
    
	/**
	 * Get the remote phone number involved in the current call
	 * 
	 * @return Phone number or null if there is no call in progress
	 * @throws ClientApiException
	 */
	public String getRemotePhoneNumber() throws ClientApiException {
		if (coreApi != null) {
			try {
		    	return coreApi.getRemotePhoneNumber();
			} catch(Exception e) {
				throw new ClientApiException(e.getMessage());
			}
		} else {
			throw new CoreServiceNotAvailableException();
		}
	}
	
	/**
	 * Initiate a live video sharing session
	 * 
	 * @param contact Contact
	 * @param player Media player
	 * @return Video sharing session
	 * @throws ClientApiException
	 */
	public IVideoSharingSession initiateLiveVideoSharing(String contact, IMediaPlayer player) throws ClientApiException {
    	if (coreApi != null) {
			try {
		    	return coreApi.initiateLiveVideoSharing(contact, player);
			} catch(Exception e) {
				throw new ClientApiException(e.getMessage());
			}
		} else {
			throw new CoreServiceNotAvailableException();
		}
	}

	/**
	 * Initiate a pre-recorded video sharing session
	 * 
 	 * @param contact Contact
 	 * @param file Video file
	 * @param player Media player
	 * @return Video sharing session
	 * @throws ClientApiException
 	 */
	public IVideoSharingSession initiateVideoSharing(String contact, String file, IMediaPlayer player) throws ClientApiException {
    	if (coreApi != null) {
			try {
		    	return coreApi.initiateVideoSharing(contact, file, player);
			} catch(Exception e) {
				throw new ClientApiException(e.getMessage());
			}
		} else {
			throw new CoreServiceNotAvailableException();
		}
	}

	/**
	 * Get a video sharing session from its session ID
	 *
	 * @param id Session ID
	 * @return Session
	 * @throws ClientApiException
	 */
	public IVideoSharingSession getVideoSharingSession(String id) throws ClientApiException {
    	if (coreApi != null) {
			try {
		    	return coreApi.getVideoSharingSession(id);
			} catch(Exception e) {
				throw new ClientApiException(e.getMessage());
			}
		} else {
			throw new CoreServiceNotAvailableException();
		}
	}
	
	/**
	 * Get list of current video sharing sessions with a contact
	 * 
	 * @param contact Contact
	 * @return List of sessions
	 * @throws ClientApiException
	 */
	public List<IBinder> getVideoSharingSessionsWith(String contact) throws ClientApiException {
    	if (coreApi != null) {
			try {
		    	return coreApi.getVideoSharingSessionsWith(contact);
			} catch(Exception e) {
				throw new ClientApiException(e.getMessage());
			}
		} else {
			throw new CoreServiceNotAvailableException();
		}
	}	

	/**
	 * Initiate an image sharing session
	 * 
	 * @param contact Contact
	 * @param file Image file
	 * @return Image sharing session
	 * @throws ClientApiException
	 */
	public IImageSharingSession initiateImageSharing(String contact, String file) throws ClientApiException {
    	if (coreApi != null) {
			try {
		    	return coreApi.initiateImageSharing(contact, file, false);
			} catch(Exception e) {
				throw new ClientApiException(e.getMessage());
			}
		} else {
			throw new CoreServiceNotAvailableException();
		}
	}
	
	/**
	 * Initiate an image sharing session
	 * 
	 * @param contact Contact
	 * @param file Image file
	 * @param thumbnail Thumbnail option
	 * @return Image sharing session
	 * @throws ClientApiException
	 */
	public IImageSharingSession initiateImageSharing(String contact, String file, boolean thumbnail) throws ClientApiException {
    	if (coreApi != null) {
			try {
		    	return coreApi.initiateImageSharing(contact, file, thumbnail);
			} catch(Exception e) {
				throw new ClientApiException(e.getMessage());
			}
		} else {
			throw new CoreServiceNotAvailableException();
		}
	}
	
	/**
	 * Get an image sharing session from its session ID
	 * 
	 * @param id Session ID
	 * @return Session
	 * @throws ClientApiException
	 */
	public IImageSharingSession getImageSharingSession(String id) throws ClientApiException {
    	if (coreApi != null) {
			try {
		    	return coreApi.getImageSharingSession(id);
			} catch(Exception e) {
				throw new ClientApiException(e.getMessage());
			}
		} else {
			throw new CoreServiceNotAvailableException();
		}
	}

	/**
	 * Get list of current image sharing sessions with a contact
	 * 
	 * @param contact Contact
	 * @return List of sessions
	 * @throws ClientApiException
	 */
	public List<IBinder> getImageSharingSessionsWith(String contact) throws ClientApiException {
   	if (coreApi != null) {
			try {
		    	return coreApi.getImageSharingSessionsWith(contact);
			} catch(Exception e) {
				throw new ClientApiException(e.getMessage());
			}
		} else {
			throw new CoreServiceNotAvailableException();
		}
	}
	
	/**
	 * Set multiparty call
	 * 
	 * @param state State
	 * @throws ClientApiException
	 */
	public void setMultiPartyCall(boolean state) throws ClientApiException {
    	if (coreApi != null) {
			try {
		    	coreApi.setMultiPartyCall(state);
			} catch(Exception e) {
				throw new ClientApiException(e.getMessage());
			}
		} else {
			throw new CoreServiceNotAvailableException();
		}
	}

	/**
	 * Set call hold
	 * 
	 * @param state State
	 * @throws ClientApiException
	 */
	public void setCallHold(boolean state) throws ClientApiException {
    	if (coreApi != null) {
			try {
		    	coreApi.setCallHold(state);
			} catch(Exception e) {
				throw new ClientApiException(e.getMessage());
			}
		} else {
			throw new CoreServiceNotAvailableException();
		}
	}
		
	/**
	 * Initiate a geoloc sharing session
	 * 
	 * @param contact Contact
	 * @param geoloc Geoloc info
	 * @return Geoloc sharing session
	 * @throws ClientApiException
	 */
	public IGeolocSharingSession initiateGeolocSharing(String contact, GeolocPush geoloc) throws ClientApiException {
    	if (coreApi != null) {
			try {
		    	return coreApi.initiateGeolocSharing(contact, geoloc);
			} catch(Exception e) {
				throw new ClientApiException(e.getMessage());
			}
		} else {
			throw new CoreServiceNotAvailableException();
		}
	}	
	
	/**
	 * Get a geoloc sharing session from its session ID
	 * 
	 * @param id Session ID
	 * @return Session
	 * @throws ClientApiException
	 */
	public IGeolocSharingSession getGeolocSharingSession(String id) throws ClientApiException {
    	if (coreApi != null) {
			try {
		    	return coreApi.getGeolocSharingSession(id);
			} catch(Exception e) {
				throw new ClientApiException(e.getMessage());
			}
		} else {
			throw new CoreServiceNotAvailableException();
		}
	}	
}
