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

package com.orangelabs.rcs.service.api.client;

import java.util.Vector;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;

/**
 * Client API
 * 
 * @author jexa7410
 */
public abstract class ClientApi {
	/**
	 * API event listeners
	 */
	private Vector<ClientApiListener> listeners = new Vector<ClientApiListener>();
	
	/**
	 * IMS API event listeners
	 */
	private Vector<ImsEventListener> imsListeners = new Vector<ImsEventListener>();
		
	/**
	 * Application context
	 */
	protected Context ctx;
	
	/**
	 * IMS core API
	 */
	protected IImsApi imsCoreApi;

    /**
     * Last IMS status
     */
    private boolean lastImsStatus = false;

	/**
	 * Constructor
	 */
	public ClientApi(Context ctx) {
		this.ctx = ctx;
	}
	
    /**
     * Connect API
     */
    public void connectApi() {
    	// Connect to IMS API
    	ctx.bindService(new Intent(IImsApi.class.getName()), imsApiConnection, 0);

    	// Register the IMS connection broadcast receiver
		ctx.registerReceiver(imsConnectionReceiver, new IntentFilter(ImsApiIntents.IMS_STATUS));

		if (!ClientApiUtils.isServiceStarted(ctx)) {
        	// Notify event listener
        	notifyEventApiDisabled();
		}
    }
    
    /**
     * Disconnect API
     */
    public void disconnectApi() {
		// Unregister the broadcast receiver
    	try {
	    	ctx.unregisterReceiver(imsConnectionReceiver);
	    } catch (IllegalArgumentException e) {
	    	// Nothing to do
	    }

    	// Disconnect from IMS API
	    try {
	    	ctx.unbindService(imsApiConnection);
		} catch (IllegalArgumentException e) {
			// Nothing to do
		}
    }
	
	/**
	 * IMS API connection
	 */
	protected ServiceConnection imsApiConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            imsCoreApi = IImsApi.Stub.asInterface(service);

            try {
				if (imsCoreApi.isImsConnected()) {
                    lastImsStatus = true;
					notifyEventImsConnected();
				} else {
                    lastImsStatus = false;
					notifyEventImsDisconnected(ImsDisconnectionReason.UNKNOWN);
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
        }

        public void onServiceDisconnected(ComponentName className) {
        	imsCoreApi = null;
        }
    };
    
    /**
	 * Add an API event listener
	 * 
	 * @param listener Listener
	 */
	public void addApiEventListener(ClientApiListener listener) {
		listeners.addElement(listener);
	}

	/**
	 * Remove an API event listener
	 * 
	 * @param listener Listener
	 */
	public void removeApiEventListener(ClientApiListener listener) {
		listeners.removeElement(listener);
	}
	
	/**
	 * Add an IMS event listener
	 * 
	 * @param listener Listener
	 */
	public void addImsEventListener(ImsEventListener listener) {
		imsListeners.addElement(listener);
	}

	/**
	 * Remove an IMS event listener
	 * 
	 * @param listener Listener
	 */
	public void removeImsEventListener(ImsEventListener listener) {
		imsListeners.removeElement(listener);
	}

	/**
	 * Remove all API event listeners
	 */
	public void removeAllApiEventListeners() {
		listeners.removeAllElements();
		imsListeners.removeAllElements();
	}
	
	/**
	 * Notify listeners when API is disabled
	 */
	protected void notifyEventApiDisabled() {
		for(int i=0; i < listeners.size(); i++) {
			ClientApiListener listener = (ClientApiListener)listeners.elementAt(i);
			listener.handleApiDisabled();
		}
	}

	/**
	 * Notify listeners when API is connected to the server
	 */
	protected void notifyEventApiConnected() {
		for(int i=0; i < listeners.size(); i++) {
			ClientApiListener listener = (ClientApiListener)listeners.elementAt(i);
			listener.handleApiConnected();
		}
	}

	/**
	 * Notify listeners when API is disconnected from the server
	 */
	protected void notifyEventApiDisconnected() {
		for(int i=0; i < listeners.size(); i++) {
			ClientApiListener listener = (ClientApiListener)listeners.elementAt(i);
			listener.handleApiDisconnected();
		}
	}

	/**
	 * Broadcast receiver to be aware of IMS connection changes
	 */
	protected BroadcastReceiver imsConnectionReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, final Intent intent) {
			if (intent.getBooleanExtra("status", false)) {
                if (!lastImsStatus) {
                    lastImsStatus = true;
                    // Connected to IMS
                    notifyEventImsConnected();
                }
			} else {
                if (lastImsStatus) {
                    lastImsStatus = false;
                    // Disconnected from IMS
                    notifyEventImsDisconnected(intent.getIntExtra("reason", ImsDisconnectionReason.UNKNOWN));
                }
			}
		}
	};	
	
	/**
	 * Notify listeners when client is registered to the IMS
	 */
	private void notifyEventImsConnected() {
		for(int i=0; i < imsListeners.size(); i++) {
			ImsEventListener imsListener = (ImsEventListener)imsListeners.elementAt(i);
			imsListener.handleImsConnected();
		}
	}

	/**
	 * Notify listeners when client is not registered to the IMS
	 * 
	 * @param reason Disconnection reason
	 */
	private void notifyEventImsDisconnected(int reason) {
		for(int i=0; i < imsListeners.size(); i++) {
			ImsEventListener imsListener = (ImsEventListener)imsListeners.elementAt(i);
			imsListener.handleImsDisconnected(reason);
		}
	}	

	/**
	 * Is service connected to the IMS
	 * 
	 * @param ctx Context
	 * @return Boolean
	 */
	public boolean isImsConnected(Context ctx) throws ClientApiException {
		if (imsCoreApi != null) {
			try {
				return imsCoreApi.isImsConnected();
			} catch(Exception e) {
				throw new ClientApiException(e.getMessage());
			}
		} else {
			throw new CoreServiceNotAvailableException();
		}
	}
}
