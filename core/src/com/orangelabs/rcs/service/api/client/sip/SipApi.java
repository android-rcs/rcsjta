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

package com.orangelabs.rcs.service.api.client.sip;

import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.orangelabs.rcs.service.api.client.ClientApi;
import com.orangelabs.rcs.service.api.client.ClientApiException;
import com.orangelabs.rcs.service.api.client.CoreServiceNotAvailableException;

/**
 * SIP API
 * 
 * @author jexa7410
 */
public class SipApi extends ClientApi {
	/**
	 * Core service API
	 */
	private ISipApi coreApi = null;

	/**
     * Constructor
     * 
     * @param ctx Application context
     */
    public SipApi(Context ctx) {
    	super(ctx);
    }
    
    /**
     * Connect API
     */
    public void connectApi() {
    	super.connectApi();
    	
		ctx.bindService(new Intent(ISipApi.class.getName()), apiConnection, 0);
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
        	coreApi = ISipApi.Stub.asInterface(service);

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
	 * Initiate a new SIP session
	 * 
	 * @param contact Contact
	 * @param featureTag Feature tag of the service
	 * @param sdp SDP
	 * @return Session
	 * @throws ClientApiException
	 */
	public ISipSession initiateSession(String contact, String featureTag, String sdp) throws ClientApiException {
    	if (coreApi != null) {
			try {
		    	return coreApi.initiateSession(contact, featureTag, sdp);
			} catch(Exception e) {
				throw new ClientApiException(e.getMessage());
			}
		} else {
			throw new CoreServiceNotAvailableException();
		}
	}

	/**
	 * Get current SIP session from its session ID
	 *
	 * @param id Session ID
	 * @return Session
	 * @throws ClientApiException
	 */
	public ISipSession getSession(String id) throws ClientApiException {
    	if (coreApi != null) {
			try {
		    	return coreApi.getSession(id);
			} catch(Exception e) {
				throw new ClientApiException(e.getMessage());
			}
		} else {
			throw new CoreServiceNotAvailableException();
		}
	}

	/**
	 * Get list of current SIP sessions with a contact
	 * 
	 * @param contact Contact
	 * @return Session
	 * @throws ClientApiException
	 */
	public List<IBinder> getSessionsWith(String contact) throws ClientApiException {
    	if (coreApi != null) {
			try {
		    	return coreApi.getSessionsWith(contact);
			} catch(Exception e) {
				throw new ClientApiException(e.getMessage());
			}
		} else {
			throw new CoreServiceNotAvailableException();
		}
	}

	/**
	 * Get list of current SIP sessions
	 * 
	 * @return List of sessions
	 * @throws ClientApiException
	 */
	public List<IBinder> getSessions() throws ClientApiException {
		if (coreApi != null) {
			try {
		    	return coreApi.getSessions();
			} catch(Exception e) {
				throw new ClientApiException(e.getMessage());
			}
		} else {
			throw new CoreServiceNotAvailableException();
		}
	}
	
	/**
	 * Send an instant message (SIP MESSAGE)
	 * 
     * @param contact Contact
	 * @param featureTag Feature tag of the service
     * @param content Content
     * @param contentType Content type
	 * @return True if successful else returns false
	 * @throws ClientApiException
	 */
	public boolean sendSipInstantMessage(String contact, String featureTag, String content, String contentType) throws ClientApiException {
		if (coreApi != null) {
			try {
		    	return coreApi.sendSipInstantMessage(contact, featureTag, content, contentType);
			} catch(Exception e) {
				throw new ClientApiException(e.getMessage());
			}
		} else {
			throw new CoreServiceNotAvailableException();
		}
	}
}
