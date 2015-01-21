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
package com.gsma.services.rcs.ipcall;

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
 * This class offers the main entry point to initiate IP calls. Several
 * applications may connect/disconnect to the API.
 * 
 * The parameter contact in the API supports the following formats:
 * MSISDN in national or international format, SIP address, SIP-URI
 * or Tel-URI.
 * 
 * @author Jean-Marc AUFFRET
 */
public class IPCallService extends RcsService {
	/**
	 * API
	 */
	private IIPCallService mApi;
	
	private static final String ERROR_CNX = "IPCall service not connected";
	
    /**
     * Constructor
     * 
     * @param ctx Application context
     * @param listener Service listener
     */
    public IPCallService(Context ctx, RcsServiceListener listener) {
    	super(ctx, listener);
    }

    /**
     * Connects to the API
     */
    public void connect() {
    	mCtx.bindService(new Intent(IIPCallService.class.getName()), apiConnection, 0);
    }
    
    /**
     * Disconnects from the API
     */
    public void disconnect() {
    	try {
    		mCtx.unbindService(apiConnection);
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
        mApi = (IIPCallService)api;
    }

    /**
	 * Service connection
	 */
	private ServiceConnection apiConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
        	setApi(IIPCallService.Stub.asInterface(service));
        	if (mListener != null) {
        		mListener.onServiceConnected();
        	}
        }

        public void onServiceDisconnected(ComponentName className) {
        	setApi(null);
        	if (mListener != null) {
        		mListener.onServiceDisconnected(RcsService.Error.CONNECTION_LOST);
        	}
        }
    };
	
    /**
     * Returns the configuration of IP call service
     * 
     * @return Configuration
     * @throws RcsServiceException
     */
    public IPCallServiceConfiguration getConfiguration() throws RcsServiceException {
		if (mApi != null) {
			try {
				return new IPCallServiceConfiguration(mApi.getConfiguration());
			} catch(Exception e) {
				throw new RcsServiceException(e);
			}
		} else {
			throw new RcsServiceNotAvailableException(ERROR_CNX);
		}
	}

    /**
     * Initiates an IP call with a contact (audio only). The parameter contact supports the following
     * formats: MSISDN in national or international format, SIP address, SIP-URI or Tel-URI. If the
     * format of the contact is not supported an exception is thrown.
     * 
     * @param contact Contact identifier
     * @param player IP call player
     * @param renderer IP call renderer
     * @return IP call
     * @throws RcsServiceException
     */
    public IPCall initiateCall(ContactId contact, IPCallPlayer player, IPCallRenderer renderer) throws RcsServiceException {
		if (mApi != null) {
			try {
				IIPCall callIntf = mApi.initiateCall(contact, player, renderer);
				if (callIntf != null) {
					return new IPCall(callIntf);
				} else {
					return null;
				}
			} catch(Exception e) {
				throw new RcsServiceException(e);
			}
		} else {
			throw new RcsServiceNotAvailableException(ERROR_CNX);
		}
    }    
    
    /**
     * Initiates an IP call visio with a contact (audio and video). The parameter contact supports the following
     * formats: MSISDN in national or international format, SIP address, SIP-URI or Tel-URI. If the format of
     * the contact is not supported an exception is thrown.
     * 
     * @param contact Contact identifier
     * @param player IP call player
     * @param renderer IP call renderer
     * @return IP call
     * @throws RcsServiceException
     */
    public IPCall initiateVisioCall(ContactId contact, IPCallPlayer player, IPCallRenderer renderer) throws RcsServiceException {
		if (mApi != null) {
			try {
				IIPCall callIntf = mApi.initiateVisioCall(contact, player, renderer);
				if (callIntf != null) {
					return new IPCall(callIntf);
				} else {
					return null;
				}
			} catch(Exception e) {
				throw new RcsServiceException(e);
			}
		} else {
			throw new RcsServiceNotAvailableException(ERROR_CNX);
		}
    }    
    
    /**
     * Returns the list of IP calls in progress
     * 
     * @return List of IP calls
     * @throws RcsServiceException
     */
    public Set<IPCall> getIPCalls() throws RcsServiceException {
		if (mApi != null) {
			try {
	    		Set<IPCall> result = new HashSet<IPCall>();
				List<IBinder> vshList = mApi.getIPCalls();
				for (IBinder binder : vshList) {
					IPCall call = new IPCall(IIPCall.Stub.asInterface(binder));
					result.add(call);
				}
				return result;
			} catch(Exception e) {
				throw new RcsServiceException(e);
			}
		} else {
			throw new RcsServiceNotAvailableException(ERROR_CNX);
		}
    }    

    /**
     * Returns a current IP call from its unique ID
     * 
     * @param callId Call ID
     * @return IP call or null if not found
     * @throws RcsServiceException
     */
    public IPCall getIPCall(String callId) throws RcsServiceException {
		if (mApi != null) {
			try {
				return new IPCall(mApi.getIPCall(callId));
			} catch(Exception e) {
				throw new RcsServiceException(e);
			}
		} else {
			throw new RcsServiceNotAvailableException(ERROR_CNX);
		}
    }    
    
	/**
	 * Adds an event listener on IP call events
	 * 
	 * @param listener Listener
	 * @throws RcsServiceException
	 */
	public void addEventListener(IPCallListener listener) throws RcsServiceException {
		if (mApi != null) {
			try {
				mApi.addEventListener2(listener);
			} catch (Exception e) {
				throw new RcsServiceException(e);
			}
		} else {
			throw new RcsServiceNotAvailableException(ERROR_CNX);
		}
	}

	/**
	 * Removes an event listener from IP call events
	 * 
	 * @param listener Listener
	 * @throws RcsServiceException
	 */
	public void removeEventListener(IPCallListener listener) throws RcsServiceException {
		if (mApi != null) {
			try {
				mApi.removeEventListener2(listener);
			} catch (Exception e) {
				throw new RcsServiceException(e);
			}
		} else {
			throw new RcsServiceNotAvailableException(ERROR_CNX);
		}
	}
}
