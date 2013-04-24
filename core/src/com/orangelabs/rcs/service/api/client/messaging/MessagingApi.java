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

package com.orangelabs.rcs.service.api.client.messaging;

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
 * Messaging API
 * 
 * @author jexa7410
 */
public class MessagingApi extends ClientApi {

	/**
	 * Core service API
	 */
	private IMessagingApi coreApi = null;
	
	/**
     * Constructor
     * 
     * @param ctx Application context
     */
    public MessagingApi(Context ctx) {
    	super(ctx);
    }
    
    /**
     * Connect API
     */
    public void connectApi() {
    	super.connectApi();

    	ctx.bindService(new Intent(IMessagingApi.class.getName()), apiConnection, 0);
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
            coreApi = IMessagingApi.Stub.asInterface(service);

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
     * Transfer a file
     *
     * @param contact Contact
     * @param file File to be transfered
	 * @return File transfer session
     * @throws ClientApiException
     */
    public IFileTransferSession transferFile(String contact, String file) throws ClientApiException {	
        return transferFile(contact, file, false);	
    }
    
    /**
     * Transfer a file
     *
     * @param contact Contact
     * @param file File to be transfered
     * @param thumbnail Thumbnail option
	 * @return File transfer session
     * @throws ClientApiException
     */
    public IFileTransferSession transferFile(String contact, String file, boolean thumbnail) throws ClientApiException {	
    	if (coreApi != null) {
			try {
				IFileTransferSession session = coreApi.transferFile(contact, file, thumbnail);
		    	return session;
			} catch(Exception e) {
				throw new ClientApiException(e.getMessage());
			}
		} else {
			throw new CoreServiceNotAvailableException();
		}
    }

	/**
	 * Get current file transfer session from its session ID
	 * 
	 * @param id Session ID
	 * @return Session
	 * @throws ClientApiException
	 */
	public IFileTransferSession getFileTransferSession(String id) throws ClientApiException {
    	if (coreApi != null) {
			try {
		    	return coreApi.getFileTransferSession(id);
			} catch(Exception e) {
				throw new ClientApiException(e.getMessage());
			}
		} else {
			throw new CoreServiceNotAvailableException();
		}
	}
	
	/**
	 * Get list of current file transfer sessions with a contact
	 * 
	 * @param contact Contact
	 * @return List of sessions
	 * @throws ClientApiException
	 */
	public List<IBinder> getFileTransferSessionsWith(String contact) throws ClientApiException {
    	if (coreApi != null) {
			try {
		    	return coreApi.getFileTransferSessionsWith(contact);
			} catch(Exception e) {
				throw new ClientApiException(e.getMessage());
			}
		} else {
			throw new CoreServiceNotAvailableException();
		}
    }
	
	/**
	 * Get list of current file transfer sessions
	 * 
	 * @return List of sessions
	 * @throws ClientApiException
	 */
	public List<IBinder> getFileTransferSessions() throws ClientApiException {
    	if (coreApi != null) {
			try {
		    	return coreApi.getFileTransferSessions();
			} catch(Exception e) {
				throw new ClientApiException(e.getMessage());
			}
		} else {
			throw new CoreServiceNotAvailableException();
		}
	}	

	/**
	 * Initiate a one-to-one chat session
	 * 
     * @param contact Contact
     * @param firstMsg First message exchanged during the session
   	 * @return Chat session
	 * @throws ClientApiException
	 */
	public IChatSession initiateOne2OneChatSession(String contact, String firstMsg) throws ClientApiException {
    	if (coreApi != null) {
			try {
		    	return coreApi.initiateOne2OneChatSession(contact, firstMsg);
			} catch(Exception e) {
				throw new ClientApiException(e.getMessage());
			}
		} else {
			throw new CoreServiceNotAvailableException();
		}
	}

	/**
	 * Initiate an ad-hoc group chat session
	 * 
     * @param participants List of participants
	 * @return Chat session
	 * @throws ClientApiException
	 */
	public IChatSession initiateAdhocGroupChatSession(List<String> participants) throws ClientApiException {
		return initiateAdhocGroupChatSession(participants, null);
	}
		
	/**
	 * Initiate an ad-hoc group chat session
	 * 
     * @param participants List of participants
     * @param subject Subject associated to the session
	 * @return Chat session
	 * @throws ClientApiException
	 */
	public IChatSession initiateAdhocGroupChatSession(List<String> participants, String subject) throws ClientApiException {
    	if (coreApi != null) {
			try {
		    	return coreApi.initiateAdhocGroupChatSession(participants, subject);
			} catch(Exception e) {
				throw new ClientApiException(e.getMessage());
			}
		} else {
			throw new CoreServiceNotAvailableException();
		}
	}
	
	/**
	 * Rejoin a group chat session
	 * 
	 * @param chatId Chat ID
	 * @return Chat session
	 * @throws ClientApiException
	 */
	public IChatSession rejoinGroupChatSession(String chatId) throws ClientApiException {
    	if (coreApi != null) {
			try {
		    	return coreApi.rejoinGroupChatSession(chatId);
			} catch(Exception e) {
				throw new ClientApiException(e.getMessage());
			}
		} else {
			throw new CoreServiceNotAvailableException();
		}
	}
	
	/**
	 * Restart a group chat session
	 * 
	 * @param chatId Chat ID
	 * @return Chat session
	 * @throws ClientApiException
	 */
	public IChatSession restartGroupChatSession(String chatId) throws ClientApiException {
    	if (coreApi != null) {
			try {
		    	return coreApi.restartGroupChatSession(chatId);
			} catch(Exception e) {
				throw new ClientApiException(e.getMessage());
			}
		} else {
			throw new CoreServiceNotAvailableException();
		}
	}	
	
	/**
	 * Get current chat session from its session ID
	 * 
	 * @param id Session ID
	 * @return Session
	 * @throws ClientApiException
	 */
	public IChatSession getChatSession(String id) throws ClientApiException {
    	if (coreApi != null) {
			try {
		    	return coreApi.getChatSession(id);
			} catch(Exception e) {
				throw new ClientApiException(e.getMessage());
			}
		} else {
			throw new CoreServiceNotAvailableException();
		}
	}	
	
	/**
	 * Get list of current chat sessions with a contact
	 * 
	 * @param contact Contact
	 * @return Session
	 * @throws ClientApiException
	 */
	public List<IBinder> getChatSessionsWith(String contact) throws ClientApiException {
    	if (coreApi != null) {
			try {
		    	return coreApi.getChatSessionsWith(contact);
			} catch(Exception e) {
				throw new ClientApiException(e.getMessage());
			}
		} else {
			throw new CoreServiceNotAvailableException();
		}
	}

	/**
	 * Get list of current chat sessions
	 * 
	 * @return List of sessions
	 * @throws ClientApiException
	 */
	public List<IBinder> getChatSessions() throws ClientApiException {
		if (coreApi != null) {
			try {
		    	return coreApi.getChatSessions();
			} catch(Exception e) {
				throw new ClientApiException(e.getMessage());
			}
		} else {
			throw new CoreServiceNotAvailableException();
		}
	}

	/**
	 * Get list of current group chat sessions
	 * 
	 * @return List of sessions
	 * @throws ClientApiException
	 */
	public List<IBinder> getGroupChatSessions() throws ClientApiException {
		if (coreApi != null) {
			try {
		    	return coreApi.getGroupChatSessions();
			} catch(Exception e) {
				throw new ClientApiException(e.getMessage());
			}
		} else {
			throw new CoreServiceNotAvailableException();
		}
	}

	/** 
	 * Get list of current group chat sessions for a given conversation
	 * 
	 * @return List of sessions
	 * @throws ClientApiException
	 */
	public List<IBinder> getGroupChatSessionsWith(String chatId) throws ClientApiException {
		if (coreApi != null) {
			try {
		    	return coreApi.getGroupChatSessionsWith(chatId);
			} catch(Exception e) {
				throw new ClientApiException(e.getMessage());
			}
		} else {
			throw new CoreServiceNotAvailableException();
		}
	}
	
	/**
	 * Set message delivery status outside of a chat session
	 * 
	 * @param contact Contact requesting a delivery status
	 * @param msgId Message ID
	 * @param status Delivery status
	 * @throws ClientApiException
	 */
	public void setMessageDeliveryStatus(String contact, String msgId, String status) throws ClientApiException {
		if (coreApi != null) {
			try {
		    	coreApi.setMessageDeliveryStatus(contact, msgId, status);
			} catch(Exception e) {
				throw new ClientApiException(e.getMessage());
			}
		} else {
			throw new CoreServiceNotAvailableException();
		}
	}	

	/**
	 * Add message delivery listener
	 * 
	 * @param listener Listener
	 * @throws ClientApiException
	 */
	public void addMessageDeliveryListener(IMessageDeliveryListener listener) throws ClientApiException {
		if (coreApi != null) {
			try {
		    	coreApi.addMessageDeliveryListener(listener);
			} catch(Exception e) {
				throw new ClientApiException(e.getMessage());
			}
		} else {
			throw new CoreServiceNotAvailableException();
		}
	}
	
	/**
	 * Remove message delivery listener
	 * 
	 * @param listener Listener
	 * @throws ClientApiException
	 */
	public void removeMessageDeliveryListener(IMessageDeliveryListener listener) throws ClientApiException {
		if (coreApi != null) {
			try {
		    	coreApi.removeMessageDeliveryListener(listener);
			} catch(Exception e) {
				throw new ClientApiException(e.getMessage());
			}
		} else {
			throw new CoreServiceNotAvailableException();
		}
	}
}
