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

import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsIllegalArgumentException;
import com.gsma.services.rcs.RcsMaxAllowedSessionLimitReachedException;
import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.RcsPersistentStorageException;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsServiceControl;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceListener;
import com.gsma.services.rcs.RcsServiceListener.ReasonCode;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.RcsServiceNotRegisteredException;
import com.gsma.services.rcs.contact.ContactId;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.IInterface;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Chat service offers the main entry point to initiate chat 1-1 and group conversations with
 * contacts. Several applications may connect/disconnect to the API. The parameter contact in the
 * API supports the following formats: MSISDN in national or international format, SIP address,
 * SIP-URI or Tel-URI.
 * 
 * @author Jean-Marc AUFFRET
 */
public class ChatService extends RcsService {
    /**
     * API
     */
    private IChatService mApi;

    private final Map<OneToOneChatListener, WeakReference<IOneToOneChatListener>> mOneToOneChatListeners = new WeakHashMap<OneToOneChatListener, WeakReference<IOneToOneChatListener>>();
    private final Map<GroupChatListener, WeakReference<IGroupChatListener>> mGroupChatListeners = new WeakHashMap<GroupChatListener, WeakReference<IGroupChatListener>>();

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
        Intent serviceIntent = new Intent(IChatService.class.getName());
        serviceIntent.setPackage(RcsServiceControl.RCS_STACK_PACKAGENAME);
        mCtx.bindService(serviceIntent, apiConnection, 0);
    }

    /**
     * Disconnects from the API
     */
    public void disconnect() {
        try {
            mCtx.unbindService(apiConnection);
        } catch (IllegalArgumentException e) {
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
        mApi = (IChatService) api;
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
            if (mListener == null) {
                return;
            }
            ReasonCode reasonCode = ReasonCode.CONNECTION_LOST;
            try {
                if (!mRcsServiceControl.isActivated()) {
                    reasonCode = ReasonCode.SERVICE_DISABLED;
                }
            } catch (RcsServiceException e) {
                // Do nothing
            }
            mListener.onServiceDisconnected(reasonCode);
        }
    };

    /**
     * Returns the configuration of the chat service
     * 
     * @return Configuration
     * @throws RcsServiceException
     */
    public ChatServiceConfiguration getConfiguration() throws RcsServiceException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            return new ChatServiceConfiguration(mApi.getConfiguration());

        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }

    /**
     * Initiates a group chat with a group of contact and returns a GroupChat instance. The subject
     * is optional and may be null.
     * 
     * @param contacts Set of contact identifiers
     * @param subject The subject is optional and may be null
     * @return a GroupChat instance
     * @throws RcsServiceException
     */
    public GroupChat initiateGroupChat(Set<ContactId> contacts, String subject)
            throws RcsServiceException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            IGroupChat chatIntf = mApi.initiateGroupChat(new ArrayList<ContactId>(contacts),
                    subject);
            return new GroupChat(chatIntf);

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsPermissionDeniedException.assertException(e);
            RcsServiceNotRegisteredException.assertException(e);
            RcsMaxAllowedSessionLimitReachedException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
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
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            return new OneToOneChat(mApi.getOneToOneChat(contact));

        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }

    /**
     * Returns a group chat from its unique ID. An exception is thrown if the chat ID does not exist
     * 
     * @param chatId Chat ID
     * @return GroupChat
     * @throws RcsServiceException
     */
    public GroupChat getGroupChat(String chatId) throws RcsServiceException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            return new GroupChat(mApi.getGroupChat(chatId));

        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }

    /**
     * Returns true if it is possible to initiate a new group chat now else returns false.
     * 
     * @return boolean
     * @throws RcsServiceException
     */
    public boolean isAllowedToInitiateGroupChat() throws RcsServiceException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            return mApi.isAllowedToInitiateGroupChat();

        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }

    /**
     * Returns true if it's possible to initiate a new group chat with the specified contactId right
     * now, else returns false.
     * 
     * @param contact
     * @return boolean
     * @throws RcsServiceException
     */
    public boolean isAllowedToInitiateGroupChat(ContactId contact) throws RcsServiceException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            return mApi.isAllowedToInitiateGroupChat2(contact);

        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }

    /**
     * Deletes all one to one chat from history and abort/reject any associated ongoing session if
     * such exists.
     * 
     * @throws RcsServiceException
     */
    public void deleteOneToOneChats() throws RcsServiceException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            mApi.deleteOneToOneChats();
        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }

    /**
     * Deletes all group chat from history and abort/reject any associated ongoing session if such
     * exists.
     * 
     * @throws RcsServiceException
     */
    public void deleteGroupChats() throws RcsServiceException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            mApi.deleteGroupChats();
        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }

    /**
     * Deletes a one to one chat with a given contact from history and abort/reject any associated
     * ongoing session if such exists.
     * 
     * @param contact
     * @throws RcsServiceException
     */
    public void deleteOneToOneChat(ContactId contact) throws RcsServiceException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            mApi.deleteOneToOneChat(contact);
        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }

    /**
     * Delete a group chat by its chat id from history and abort/reject any associated ongoing
     * session if such exists.
     * 
     * @param chatId
     * @throws RcsServiceException
     */
    public void deleteGroupChat(String chatId) throws RcsServiceException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            mApi.deleteGroupChat(chatId);
        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }

    /**
     * Delete a message from its message id from history.
     * 
     * @param msgId
     * @throws RcsServiceException
     */
    public void deleteMessage(String msgId) throws RcsServiceException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            mApi.deleteMessage(msgId);
        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }

    /**
     * Marks undelivered chat messages to indicate that messages have been processed.
     * 
     * @param msgIds
     * @throws RcsServiceException
     */
    public void markUndeliveredMessagesAsProcessed(Set<String> msgIds) throws RcsServiceException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            mApi.markUndeliveredMessagesAsProcessed(new ArrayList<String>(msgIds));
        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }

    /**
     * Mark a received message as read (ie. displayed in the UI)
     * 
     * @param msgId Message id
     * @throws RcsServiceException
     */
    public void markMessageAsRead(String msgId) throws RcsServiceException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            mApi.markMessageAsRead(msgId);
        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }

    /**
     * Adds a listener on group chat events
     * 
     * @param listener Group chat listener
     * @throws RcsServiceException
     */
    public void addEventListener(GroupChatListener listener) throws RcsServiceException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            IGroupChatListener rcsListener = new GroupChatListenerImpl(listener);
            mGroupChatListeners.put(listener, new WeakReference<IGroupChatListener>(rcsListener));
            mApi.addEventListener3(rcsListener);
        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }

    /**
     * Removes a listener on group chat events
     * 
     * @param listener Group chat event listener
     * @throws RcsServiceException
     */
    public void removeEventListener(GroupChatListener listener) throws RcsServiceException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            WeakReference<IGroupChatListener> weakRef = mGroupChatListeners.remove(listener);
            if (weakRef == null) {
                return;
            }
            IGroupChatListener rcsListener = weakRef.get();
            if (rcsListener != null) {
                mApi.removeEventListener3(rcsListener);
            }
        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }

    /**
     * Adds a listener for one-to-one chat events
     * 
     * @param listener One-to-one chat listener
     * @throws RcsServiceException
     */
    public void addEventListener(OneToOneChatListener listener) throws RcsServiceException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            IOneToOneChatListener rcsListener = new OneToOneChatListenerImpl(listener);
            mOneToOneChatListeners.put(listener, new WeakReference<IOneToOneChatListener>(
                    rcsListener));
            mApi.addEventListener2(rcsListener);
        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }

    /**
     * Removes a listener for one-to-one chat events
     * 
     * @param listener One-to-one chat listener
     * @throws RcsServiceException
     */
    public void removeEventListener(OneToOneChatListener listener) throws RcsServiceException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            WeakReference<IOneToOneChatListener> weakRef = mOneToOneChatListeners.remove(listener);
            if (weakRef == null) {
                return;
            }
            IOneToOneChatListener rcsListener = weakRef.get();
            if (rcsListener != null) {
                mApi.removeEventListener2(rcsListener);
            }
        } catch (Exception e) {
            throw new RcsServiceException(e);
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
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            return new ChatMessage(mApi.getChatMessage(msgId));

        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }
}