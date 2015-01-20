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
package com.gsma.services.rcs.chat;

import java.util.ArrayList;
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
 * Chat service offers the main entry point to initiate chat 1-1 and group
 * conversations with contacts. Several applications may connect/disconnect
 * to the API.
 * 
 * The parameter contact in the API supports the following formats:
 * MSISDN in national or international format, SIP address, SIP-URI
 * or Tel-URI.
 * 
 * @author Jean-Marc AUFFRET
 */
public class ChatService extends RcsService {
	/**
	 * API
	 */
	private IChatService mApi;
	
	private static final String ERROR_CNX = "Chat service not connected";

	/**
     * Constructor
     * 
     * @param ctx Application context
     * @param listener Service listener
     */
    public ChatService(Context ctx, RcsServiceListener listener) {
    	super(ctx, listener);
    }

    /**
     * Connects to the API
     */
    public void connect() {
    	mCtx.bindService(new Intent(IChatService.class.getName()), apiConnection, 0);
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
        mApi = (IChatService)api;
    }

    /**
	 * Service connection
	 */
	private ServiceConnection apiConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
        	setApi(IChatService.Stub.asInterface(service));
        	if (mListener != null) {
        		mListener.onServiceConnected();
        	}
        }

        public void onServiceDisconnected(ComponentName className) {
        	setApi(null);
        	if (mListener != null) {
        		mListener.onServiceDisconnected(Error.CONNECTION_LOST);
        	}
        }
    };
	
	/**
     * Returns the configuration of the chat service
     * 
     * @return Configuration
     * @throws RcsServiceException
     */
    public ChatServiceConfiguration getConfiguration() throws RcsServiceException {
		if (mApi != null) {
			try {
				return new ChatServiceConfiguration(mApi.getConfiguration());
			} catch(Exception e) {
				throw new RcsServiceException(e);
			}
		} else {
			throw new RcsServiceNotAvailableException(ERROR_CNX);
		}
	}    
  
	/**
	 * Initiates a group chat with a group of contact and returns a GroupChat instance. The subject is optional and may be null.
	 * 
	 * @param contacts
	 *            Set of contact identifiers
	 * @param subject
	 *            The subject is optional and may be null
	 * @return a GroupChat instance
	 * @throws RcsServiceException
	 */
    public GroupChat initiateGroupChat(Set<ContactId> contacts, String subject) throws RcsServiceException {
    	if (mApi != null) {
			try {
				IGroupChat chatIntf = mApi.initiateGroupChat(new ArrayList<ContactId>(contacts), subject);
				if (chatIntf != null) {
					return new GroupChat(chatIntf);
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
     * Returns a chat with a given contact
     * 
     * @param contact ContactId
     * @return Chat
     * @throws RcsServiceException
     */
	public OneToOneChat getOneToOneChat(ContactId contact) throws RcsServiceException {
		if (mApi != null) {
			try {
				return new OneToOneChat(mApi.getOneToOneChat(contact));
			} catch (Exception e) {
				throw new RcsServiceException(e);
			}
		} else {
			throw new RcsServiceNotAvailableException(ERROR_CNX);
		}
	}

	/**
	 * Returns a group chat from its unique ID. An exception is thrown if the
	 * chat ID does not exist
	 *
	 * @param chatId Chat ID
	 * @return GroupChat
	 * @throws RcsServiceException
	 */
    public GroupChat getGroupChat(String chatId) throws RcsServiceException {
		if (mApi != null) {
			try {
				return new GroupChat(mApi.getGroupChat(chatId));
			} catch(Exception e) {
				throw new RcsServiceException(e);
			}
		} else {
			throw new RcsServiceNotAvailableException(ERROR_CNX);
		}
    }
    
    /**
     * Mark a received message as read (ie. displayed in the UI)
     *
     * @param msgId Message id
     * @throws RcsServiceException
     */
    public void markMessageAsRead(String msgId) throws RcsServiceException {
        if (mApi != null) {
            try {
                mApi.markMessageAsRead(msgId);
            } catch(Exception e) {
                throw new RcsServiceException(e);
            }
        } else {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
    }

	/**
	 * Set the parameter that controls whether to respond or not to display reports when requested by the remote.
	 * <p>
	 * Only applicable to one to one chat messages.
	 * 
	 * @param enable
	 *            true if respond to display reports
	 * @throws RcsServiceException
	 */
	public void setRespondToDisplayReports(boolean enable) throws RcsServiceException {
		if (mApi != null) {
			try {
				mApi.setRespondToDisplayReports(enable);
			} catch (Exception e) {
				throw new RcsServiceException(e);
			}
		} else {
			throw new RcsServiceNotAvailableException(ERROR_CNX);
		}
	}

	/**
	 * Adds a listener on group chat events
	 *
	 * @param listener Group chat listener
	 * @throws RcsServiceException
	 */
	public void addEventListener(GroupChatListener listener) throws RcsServiceException {
		if (mApi != null) {
			try {
				mApi.addEventListener3(listener);
			} catch (Exception e) {
				throw new RcsServiceException(e);
			}
		} else {
			throw new RcsServiceNotAvailableException(ERROR_CNX);
		}
	}

	/**
	 * Removes a listener on group chat events
	 *
	 * @param listener Group chat event listener
	 * @throws RcsServiceException
	 */
	public void removeEventListener(GroupChatListener listener)
			throws RcsServiceException {
		if (mApi != null) {
			try {
				mApi.removeEventListener3(listener);
			} catch (Exception e) {
				throw new RcsServiceException(e);
			}
		} else {
			throw new RcsServiceNotAvailableException(ERROR_CNX);
		}
	}

	/**
	 * Adds a listener for one-to-one chat events
	 *
	 * @param listener One-to-one chat listener
	 * @throws RcsServiceException
	 */
	public void addEventListener(OneToOneChatListener listener) throws RcsServiceException {
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
	 * Removes a listener for one-to-one chat events
	 *
	 * @param listener One-to-one chat listener
	 * @throws RcsServiceException
	 */
	public void removeEventListener(OneToOneChatListener listener) throws RcsServiceException {
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

	/**
	 * Returns a chat message from its unique ID
	 * 
	 * @param msgId
	 * @return ChatMessage
	 * @throws RcsServiceException
	 */
	public ChatMessage getChatMessage(String msgId) throws RcsServiceException {
		if (mApi != null) {
			try {
				return new ChatMessage(mApi.getChatMessage(msgId));
			} catch (Exception e) {
				throw new RcsServiceException(e);
			}
		} else {
			throw new RcsServiceNotAvailableException(ERROR_CNX);
		}
	}
}
