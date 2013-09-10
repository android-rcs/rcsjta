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
package org.gsma.joyn.ish;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.gsma.joyn.JoynContactFormatException;
import org.gsma.joyn.JoynService;
import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.JoynServiceListener;
import org.gsma.joyn.JoynServiceNotAvailableException;
import org.gsma.joyn.JoynServiceRegistrationListener;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

/**
 * This class offers the main entry point to transfer image during
 * a CS call. Several applications may connect/disconnect to the API.
 * 
 * The parameter contact in the API supports the following formats:
 * MSISDN in national or international format, SIP address, SIP-URI
 * or Tel-URI.
 */
public class ImageSharingService extends JoynService {
	/**
	 * API
	 */
	private IImageSharingService api = null;
	
    /**
     * Constructor
     * 
     * @param ctx Application context
     * @param listener Service listener
     */
    public ImageSharingService(Context ctx, JoynServiceListener listener) {
    	super(ctx, listener);
    }

    /**
     * Connects to the API
     */
    public void connect() {
    	ctx.bindService(new Intent(IImageSharingService.class.getName()), apiConnection, 0);
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
     * Returns true if connected to the service, else returns false
     * 
	 * @return Returns true if connected else returns false
     */
    public boolean isServiceConnected() {
    	return (api != null);
    }

    /**
	 * Service connection
	 */
	private ServiceConnection apiConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
        	api = IImageSharingService.Stub.asInterface(service);
        	if (serviceListener != null) {
        		serviceListener.onServiceConnected();
        	}
        }

        public void onServiceDisconnected(ComponentName className) {
        	api = null;
        	if (serviceListener != null) {
        		serviceListener.onServiceDisconnected(JoynService.Error.CONNECTION_LOST);
        	}
        }
    };
    
    /**
     * Returns true if the service is registered to the platform, else returns false
     * 
	 * @return Returns true if registered else returns false
     * @throws JoynServiceException
     */
    public boolean isServiceRegistered() throws JoynServiceException {
		if (api != null) {
			try {
				return api.isServiceRegistered();
			} catch(Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
    }

	/**
	 * Registers a listener on service registration events
	 * 
	 * @param listener Service registration listener
     * @throws JoynServiceException
	 */
	public void addServiceRegistrationListener(JoynServiceRegistrationListener listener) throws JoynServiceException {
		if (api != null) {
			try {
				api.addServiceRegistrationListener(listener);
			} catch(Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
	}
	
	/**
	 * Unregisters a listener on service registration events
	 * 
	 * @param listener Service registration listener
     * @throws JoynServiceException
	 */
	public void removeServiceRegistrationListener(JoynServiceRegistrationListener listener) throws JoynServiceException {
		if (api != null) {
			try {
				api.removeServiceRegistrationListener(listener);
			} catch(Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
	}     
    
    /**
     * Returns the configuration of image sharing service
     * 
     * @return Configuration
     * @throws JoynServiceException
     */
    public ImageSharingServiceConfiguration getConfiguration() throws JoynServiceException {
		if (api != null) {
			try {
				return api.getConfiguration();
			} catch(Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
	}

    /**
     * Shares an image with a contact. The parameter file contains the complete filename
     * including the path of the image to be shared. An exception if thrown if there is
     * no ongoing CS call. The parameter contact supports the following formats: MSISDN
     * in national or international format, SIP address, SIP-URI or Tel-URI. If the format
     * of the contact is not supported an exception is thrown.
     * 
     * @param contact Contact
     * @param filename Filename to share
     * @param listener Image sharing event listener
     * @return Image sharing
     * @throws JoynServiceException
	 * @throws JoynContactFormatException
     */
    public ImageSharing shareImage(String contact, String filename, ImageSharingListener listener) throws JoynServiceException, JoynContactFormatException {
		if (api != null) {
			try {
				IImageSharing sharingIntf = api.shareImage(contact, filename, listener);
				if (sharingIntf != null) {
					return new ImageSharing(sharingIntf);
				} else {
					return null;
				}
			} catch(Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
    }    
    
    /**
     * Returns the list of image sharings in progress
     * 
     * @return List of image sharings
     * @throws JoynServiceException
     */
    public Set<ImageSharing> getImageShares() throws JoynServiceException {
		if (api != null) {
			try {
	    		Set<ImageSharing> result = new HashSet<ImageSharing>();
				List<IBinder> ishList = api.getImageSharings();
				for (IBinder binder : ishList) {
					ImageSharing sharing = new ImageSharing(IImageSharing.Stub.asInterface(binder));
					result.add(sharing);
				}
				return result;
			} catch(Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
    }    

    /**
     * Returns a current image sharing from its unique ID
     * 
     * @return Image sharing or null if not found
     * @throws JoynServiceException
     */
    public ImageSharing getImageSharing(String sharingId) throws JoynServiceException {
		if (api != null) {
			try {
				IImageSharing sharingIntf = api.getImageSharing(sharingId);
				if (sharingIntf != null) {
					return new ImageSharing(sharingIntf);
				} else {
					return null;
				}
			} catch(Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
    }    
    
    /**
     * Returns a current image sharing from its invitation Intent
     * 
     * @param intent Invitation intent
     * @return Image sharing or null if not found
     * @throws JoynServiceException
     */
    public ImageSharing getImageSharingFor(Intent intent) throws JoynServiceException {
		if (api != null) {
			try {
				String sharingId = intent.getStringExtra(ImageSharingIntent.EXTRA_SHARING_ID);
				if (sharingId != null) {
					return getImageSharing(sharingId);
				} else {
					return null;
				}
			} catch(Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
    }     
    
    /**
	 * Registers a new image sharing invitation listener
	 * 
	 * @param listener New image sharing listener
	 * @throws JoynServiceException
	 */
	public void addNewImageSharingListener(NewImageSharingListener listener) throws JoynServiceException {
		if (api != null) {
			try {
				api.addNewImageSharingListener(listener);
			} catch(Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
	}

	/**
	 * Unregisters a new image sharing invitation listener
	 * 
	 * @param listener New image sharing listener
	 * @throws JoynServiceException
	 */
	public void removeNewImageSharingListener(NewImageSharingListener listener) throws JoynServiceException {
		if (api != null) {
			try {
				api.removeNewImageSharingListener(listener);
			} catch(Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
	}
}
