/*
 * Copyright (C) 2015 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.sonymobile.rcs.cpm.ms;

import java.util.Set;

import javax.management.NotificationListener;

/**
 * This is an abstraction of the folder with common features shared by Session history folder and
 * Conversation history folder as well as User folder, such as adding and removing messages. An RCS
 * client shall not store objects in Conversation Histories in the "default" system folder of the
 * Message Store.
 * 
 * @see ConversationHistoryFolder
 * @see SessionHistoryFolder
 * @see UserFolder
 */
public interface CpmObjectFolder {

    public int getVersion();

    /**
     * Returns the metadata associated to this folder
     * 
     * @return
     * @throws MessageStoreException
     */
    public String getMetadata() throws CpmMessageStoreException;

    /**
     * Sets the metadata string to this folder
     * 
     * @param comment
     * @throws MessageStoreException
     */
    public void setMetadata(String comment) throws CpmMessageStoreException;

    /**
     * Returns the last requested folder status
     * 
     * @return the folder status
     * @throws CpmMessageStoreException
     */
    public CpmFolderStatus getStatus() throws CpmMessageStoreException;

    /**
     * Returns the folder name
     * 
     * @return the folder name
     */
    public String getName();

    // /**
    // * Returns the messages contained in this folder using the search criteria
    // * @see com.sonymobile.rcs.core.messagestore.imap.Search
    // * @return the messages
    // * @throws MessageStoreException
    // */
    // public List<CPMMessage> findMessages(String searchSpec) throws CPMMessageStoreException;

    /**
     * Get all objects
     * 
     * @return cpm objects
     * @throws MessageStoreException
     */
    public Set<CpmObject> getCpmObjects() throws CpmMessageStoreException;

    public Set<CpmObjectMetadata> getObjectMetadata() throws CpmMessageStoreException;

    /**
     * Get the message by its UID
     * 
     * @param id the UID
     * @return the message
     * @throws MessageStoreException
     */
    public CpmObject getObjectByStorageId(int id) throws CpmMessageStoreException;

    // /**
    // * Looks up the message using its CPM ID (Message-ID)
    // * @param id the CMP header ID
    // * @return the message
    // * @throws MessageStoreException
    // */
    // public CPMChatMessage getMessageByMessageId(String id) throws CPMMessageStoreException;

    /**
     * Set the listening mode
     * 
     * @param listening
     * @see RemoteStore#setNotificationListener(NotificationListener)
     */
    public void setListening(boolean listening);

    /**
     * Returns true if the notification listener is open to events from this folder
     * 
     * @return true if listening
     * @see RemoteStore#setNotificationListener(NotificationListener)
     */
    public boolean isListening();

}
