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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.IInterface;

import com.gsma.services.rcs.JoynService;
import com.gsma.services.rcs.JoynServiceException;
import com.gsma.services.rcs.JoynServiceListener;
import com.gsma.services.rcs.JoynServiceNotAvailableException;
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
public class ChatService extends JoynService {
	/**
	 * API
	 */
	private IChatService api;

	/**
     * Constructor
     * 
     * @param ctx Application context
     * @param listener Service listener
     */
    public ChatService(Context ctx, JoynServiceListener listener) {
    	super(ctx, listener);
    }

    /**
     * Connects to the API
     */
    public void connect() {
    	ctx.bindService(new Intent(IChatService.class.getName()), apiConnection, 0);
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
    	
        this.api = (IChatService)api;
    }

    /**
	 * Service connection
	 */
	private ServiceConnection apiConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
        	setApi(IChatService.Stub.asInterface(service));
        	if (serviceListener != null) {
        		serviceListener.onServiceConnected();
        	}
        }

        public void onServiceDisconnected(ComponentName className) {
        	setApi(null);
        	if (serviceListener != null) {
        		serviceListener.onServiceDisconnected(Error.CONNECTION_LOST);
        	}
        }
    };
	
	/**
     * Returns the configuration of the chat service
     * 
     * @return Configuration
     * @throws JoynServiceException
     */
    public ChatServiceConfiguration getConfiguration() throws JoynServiceException {
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
     * Open a single chat with a given contact and returns a Chat instance.
     * The parameter contact supports the following formats: MSISDN in national
     * or international format, SIP address, SIP-URI or Tel-URI.
     * 
     * @param contact the ContactId
     * @return Chat or null 
     * @throws JoynServiceException
     */
    public Chat openSingleChat(ContactId contact) throws JoynServiceException {
		if (api != null) {
			try {
				IChat chatIntf = api.openSingleChat(contact);
				if (chatIntf != null) {
					return new Chat(chatIntf);
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
     * Initiates a group chat with a group of contact and returns a GroupChat
     * instance. The subject is optional and may be null.
     * 
     * @param contacts Set of contact identifiers
     * @param subject Subject
     * @throws JoynServiceException
     */
    public GroupChat initiateGroupChat(Set<ContactId> contacts, String subject) throws JoynServiceException {
    	if (api != null) {
			try {
				IGroupChat chatIntf = api.initiateGroupChat(new ArrayList<ContactId>(contacts), subject);
				if (chatIntf != null) {
					return new GroupChat(chatIntf);
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
     * Rejoins an existing group chat from its unique chat ID
     * 
     * @param chatId Chat ID
     * @return Group chat
     * @throws JoynServiceException
     */
    public GroupChat rejoinGroupChat(String chatId) throws JoynServiceException {
		if (api != null) {
			try {
				IGroupChat chatIntf = api.rejoinGroupChat(chatId);
				if (chatIntf != null) {
					return new GroupChat(chatIntf);
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
     * Restarts a previous group chat from its unique chat ID
     * 
     * @param chatId Chat ID
     * @return Group chat
     * @throws JoynServiceException
     */
    public GroupChat restartGroupChat(String chatId) throws JoynServiceException {
		if (api != null) {
			try {
				IGroupChat chatIntf = api.restartGroupChat(chatId);
				if (chatIntf != null) {
					return new GroupChat(chatIntf);
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
     * Returns the list of single chats in progress
     * 
     * @return List of chats
     * @throws JoynServiceException
     */
    public Set<Chat> getChats() throws JoynServiceException {
		if (api != null) {
			try {
	    		Set<Chat> result = new HashSet<Chat>();
				List<IBinder> chatList = api.getChats();
				for (IBinder binder : chatList) {
					Chat chat = new Chat(IChat.Stub.asInterface(binder));
					result.add(chat);
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
     * Returns a chat in progress with a given contact
     * 
     * @param contact ContactId
     * @return Chat or null if not found
     * @throws JoynServiceException
     */
    public Chat getChat(ContactId contact) throws JoynServiceException {
		if (api != null) {
			try {
				IChat chatIntf = api.getChat(contact);
				if (chatIntf != null) {
					return new Chat(chatIntf);
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
     * Returns the list of group chats in progress
     * 
     * @return List of group chat
     * @throws JoynServiceException
     */
    public Set<GroupChat> getGroupChats() throws JoynServiceException {
		if (api != null) {
			try {
	    		Set<GroupChat> result = new HashSet<GroupChat>();
				List<IBinder> chatList = api.getGroupChats();
				for (IBinder binder : chatList) {
					GroupChat chat = new GroupChat(IGroupChat.Stub.asInterface(binder));
					result.add(chat);
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
     * Returns a group chat in progress from its unique ID
     * 
     * @param chatId Chat ID
     * @return Group chat or null if not found
     * @throws JoynServiceException
     */
    public GroupChat getGroupChat(String chatId) throws JoynServiceException {
		if (api != null) {
			try {
				IGroupChat chatIntf = api.getGroupChat(chatId);
				if (chatIntf != null) {
					return new GroupChat(chatIntf);
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
     * Mark a received message as read (ie. displayed in the UI)
     *
     * @param msgId Message id
     * @throws JoynServiceException
     */
    public void markMessageAsRead(String msgId) throws JoynServiceException {
        if (api != null) {
            try {
                api.markMessageAsRead(msgId);
            } catch(Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        } else {
            throw new JoynServiceNotAvailableException();
        }
    }

	/**
	 * Set the parameter that controls whether to respond or not to display reports when requested by the remote.
	 * <p>
	 * Only applicable to one to one chat messages.
	 * 
	 * @param enable
	 *            true if respond to display reports
	 * @throws JoynServiceException
	 */
	public void setRespondToDisplayReports(boolean enable) throws JoynServiceException {
		if (api != null) {
			try {
				api.setRespondToDisplayReports(enable);
			} catch (Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
	}

	/**
	 * Adds an event listener for group chat events
	 *
	 * @param listener Group chat event listener
	 * @throws JoynServiceException
	 */
	public void addGroupChatEventListener(GroupChatListener listener) throws JoynServiceException {
		if (api != null) {
			try {
				api.addGroupChatEventListener(listener);
			} catch (Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
	}

	/**
	 * Removes an event listener for group chat events
	 *
	 * @param listener Group chat event listener
	 * @throws JoynServiceException
	 */
	public void removeGroupChatEventListener(GroupChatListener listener)
			throws JoynServiceException {
		if (api != null) {
			try {
				api.removeGroupChatEventListener(listener);
			} catch (Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
	}

	/**
	 * Adds a listener for OneToOne chat events
	 *
	 * @param listener OneToOne Chat event listener
	 * @throws JoynServiceException
	 */
	public void addOneToOneChatEventListener(ChatListener listener) throws JoynServiceException {
		if (api != null) {
			try {
				api.addOneToOneChatEventListener(listener);
			} catch (Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
	}

	/**
	 * Removes a listener for OneToOne chat events
	 *
	 * @param listener OneToOne Chat event listener
	 * @throws JoynServiceException
	 */
	public void removeOneToOneChatEventListener(ChatListener listener) throws JoynServiceException {
		if (api != null) {
			try {
				api.removeOneToOneChatEventListener(listener);
			} catch (Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
	}
}
