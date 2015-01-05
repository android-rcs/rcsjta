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

package com.gsma.services.rcs.extension;

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
 * This class offers the main entry point to initiate and to manage
 * multimedia sessions. Several applications may connect/disconnect
 * to the API.
 * 
 * @author Jean-Marc AUFFRET
 */
public class MultimediaSessionService extends RcsService {
	/**
	 * API
	 */
	private IMultimediaSessionService api;
	
    /**
     * Constructor
     * 
     * @param ctx Application context
     * @param listener Service listener
     */
    public MultimediaSessionService(Context ctx, RcsServiceListener listener) {
    	super(ctx, listener);
    }

    /**
     * Connects to the API
     */
    public void connect() {
    	ctx.bindService(new Intent(IMultimediaSessionService.class.getName()), apiConnection, 0);
    }
    
    /**
     * Disconnects from the API
     */
    public void disconnect() {
    	try {
    		ctx.unbindService(apiConnection);
        } catch(IllegalArgumentException e) {
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
    	
        this.api = (IMultimediaSessionService)api;
    }
    
    /**
	 * Service connection
	 */
	private ServiceConnection apiConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
        	setApi(IMultimediaSessionService.Stub.asInterface(service));
        	if (serviceListener != null) {
        		serviceListener.onServiceConnected();
        	}
        }

        public void onServiceDisconnected(ComponentName className) {
        	setApi(null);
        	if (serviceListener != null) {
        		serviceListener.onServiceDisconnected(RcsService.Error.CONNECTION_LOST);
        	}
        }
    };
    
	/**
     * Returns the configuration of the multimedia session service
     * 
     * @return Configuration
     * @throws RcsServiceException
     */
    public MultimediaSessionServiceConfiguration getConfiguration() throws RcsServiceException {
		if (api != null) {
			try {
				return api.getConfiguration();
			} catch(Exception e) {
				throw new RcsServiceException(e.getMessage());
			}
		} else {
			throw new RcsServiceNotAvailableException();
		}
	}     
    
    /**
     * Initiates a new session for real time messaging with a remote contact and for a given
     * service extension. The messages are exchanged in real time during the session may be from
     * any type. The parameter contact supports the following formats: MSISDN in national or
     * international format, SIP address, SIP-URI or Tel-URI. If the format of the contact is
     * not supported an exception is thrown.
     * 
     * @param serviceId Service ID
     * @param contact Contact identifier
     * @return Multimedia messaging session
     * @throws RcsServiceException
     */
    public MultimediaMessagingSession initiateMessagingSession(String serviceId, ContactId contact) throws RcsServiceException {
		if (api != null) {
			try {
				IMultimediaMessagingSession sessionIntf = api.initiateMessagingSession(serviceId, contact);
				if (sessionIntf != null) {
					return new MultimediaMessagingSession(sessionIntf);
				} else {
					return null;
				}
			} catch(Exception e) {
				throw new RcsServiceException(e.getMessage());
			}
		} else {
			throw new RcsServiceNotAvailableException();
		}
    }    
    
    /**
     * Returns the list of messaging sessions associated to a given service ID
     * 
     * @param serviceId Service ID
     * @return List of messaging sessions
     * @throws RcsServiceException
     */
    public Set<MultimediaMessagingSession> getMessagingSessions(String serviceId) throws RcsServiceException {
		if (api != null) {
			try {
	    		Set<MultimediaMessagingSession> result = new HashSet<MultimediaMessagingSession>();
				List<IBinder> mmsList = api.getMessagingSessions(serviceId);
				for (IBinder binder : mmsList) {
					MultimediaMessagingSession session = new MultimediaMessagingSession(IMultimediaMessagingSession.Stub.asInterface(binder));
					result.add(session);
				}
				return result;
			} catch(Exception e) {
				throw new RcsServiceException(e.getMessage());
			}
		} else {
			throw new RcsServiceNotAvailableException();
		}
    }    

	/**
	 * Returns a current messaging session from its unique session ID
	 * 
	 * @param sessionId
	 * 
	 * @return Multimedia messaging session or null if not found
	 * @throws RcsServiceException
	 */
    public MultimediaMessagingSession getMessagingSession(String sessionId) throws RcsServiceException {
		if (api != null) {
			try {
				IMultimediaMessagingSession sessionIntf = api.getMessagingSession(sessionId);
				if (sessionIntf != null) {
					return new MultimediaMessagingSession(sessionIntf);
				} else {
					return null;
				}
			} catch(Exception e) {
				throw new RcsServiceException(e.getMessage());
			}
		} else {
			throw new RcsServiceNotAvailableException();
		}
    }    
    
    /**
     * Initiates a new session for real time streaming with a remote contact and for a given
     * service extension. The payload are exchanged in real time during the session and may be
     * from any type. The parameter contact supports the following formats: MSISDN in national or
     * international format, SIP address, SIP-URI or Tel-URI. If the format of the contact is
     * not supported an exception is thrown.
     * 
     * @param serviceId Service ID
     * @param contact Contact ID
     * @return Multimedia streaming session
     * @throws RcsServiceException
     */
    public MultimediaStreamingSession initiateStreamingSession(String serviceId, ContactId contact) throws RcsServiceException {
		if (api != null) {
			try {
				IMultimediaStreamingSession sessionIntf = api.initiateStreamingSession(serviceId, contact);
				if (sessionIntf != null) {
					return new MultimediaStreamingSession(sessionIntf);
				} else {
					return null;
				}
			} catch(Exception e) {
				throw new RcsServiceException(e.getMessage());
			}
		} else {
			throw new RcsServiceNotAvailableException();
		}
    }    
    
	/**
	 * Returns the list of streaming sessions associated to a given service ID
	 * 
	 * @param serviceId
	 *            Service ID
	 * @return List of streaming sessions
	 * @throws RcsServiceException
	 */
    public Set<MultimediaStreamingSession> getStreamingSessions(String serviceId) throws RcsServiceException {
		if (api != null) {
			try {
	    		Set<MultimediaStreamingSession> result = new HashSet<MultimediaStreamingSession>();
				List<IBinder> mmsList = api.getStreamingSessions(serviceId);
				for (IBinder binder : mmsList) {
					MultimediaStreamingSession session = new MultimediaStreamingSession(IMultimediaStreamingSession.Stub.asInterface(binder));
					result.add(session);
				}
				return result;
			} catch(Exception e) {
				throw new RcsServiceException(e.getMessage());
			}
		} else {
			throw new RcsServiceNotAvailableException();
		}
    }    

	/**
	 * Returns a current streaming session from its unique session ID
	 * 
	 * @param sessionId
	 * 
	 * @return Multimedia streaming session or null if not found
	 * @throws RcsServiceException
	 */
    public MultimediaStreamingSession getStreamingSession(String sessionId) throws RcsServiceException {
		if (api != null) {
			try {
				IMultimediaStreamingSession sessionIntf = api.getStreamingSession(sessionId);
				if (sessionIntf != null) {
					return new MultimediaStreamingSession(sessionIntf);
				} else {
					return null;
				}
			} catch(Exception e) {
				throw new RcsServiceException(e.getMessage());
			}
		} else {
			throw new RcsServiceNotAvailableException();
		}
    }    
    
	/**
	 * Adds a listener on multimedia messaging session events
	 *
	 * @param listener Session event listener
	 * @throws RcsServiceException
	 */
	public void addEventListener(MultimediaMessagingSessionListener listener)
			throws RcsServiceException {
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
	 * Removes a listener on multimedia messaging session events
	 *
	 * @param listener Session event listener
	 * @throws RcsServiceException
	 */
	public void removeEventListener(MultimediaMessagingSessionListener listener)
			throws RcsServiceException {
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

	/**
	 * Adds a listener on multimedia streaming session events
	 *
	 * @param listener Session event listener
	 * @throws RcsServiceException
	 */
	public void addEventListener(MultimediaStreamingSessionListener listener)
			throws RcsServiceException {
		if (api != null) {
			try {
				api.addEventListener3(listener);
			} catch (Exception e) {
				throw new RcsServiceException(e.getMessage());
			}
		} else {
			throw new RcsServiceNotAvailableException();
		}
	}

	/**
	 * Removes a listener on multimedia streaming session events
	 *
	 * @param listener Session event listener
	 * @throws RcsServiceException
	 */
	public void removeEventListener(MultimediaStreamingSessionListener listener)
			throws RcsServiceException {
		if (api != null) {
			try {
				api.removeEventListener3(listener);
			} catch (Exception e) {
				throw new RcsServiceException(e.getMessage());
			}
		} else {
			throw new RcsServiceNotAvailableException();
		}
	}
}
