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

import java.util.List;
import java.util.Set;

/**
 * (CPM) The user folder is a Message Storage folder realized by the mailbox concept of IMAP4,
 * described in [RFC3501]. The user folder is identified by the name given to it. The CPM user
 * folder aligns with the rules and procedures for names of the mailbox concept of IMAP4, as
 * described in [RFC3501]. For additional information, see section 5.5.1.1 of the CPM System
 * Description [OMA-CPM-SD]. A user may only create their folders under “RCSMessageStore” to better
 * manage their stored information. A user defined folder is similar to a folder created for a file
 * management system and the user can manipulate the contents in it freely; such as: - store a
 * standalone media object in it - copy any objects from other folders into it - remove any objects
 * in it and move any objects in it to other user defined folders
 */
public interface UserFolder extends CpmObjectFolder {

    /**
     * Same as CPMMessage.setDeleted(true)
     * 
     * @param message
     * @throws MessageStoreException
     */
    public void remove(CpmMessage message) throws CpmMessageStoreException;

    /**
     * Stores and create a file transfer object
     * 
     * @param cpmMessageId
     * @param type
     * @param participants
     * @param files
     * @return the file transfer
     * @throws MessageStoreException
     */
    public CpmFileTransfer addFileTransfer(String cpmMessageId, int type,
            List<Participant> participants, FileItem... files) throws CpmMessageStoreException;

    /**
     * Stores and create a chat message
     * 
     * @param cpmMessageId
     * @param from
     * @param text
     * @return the chat message
     * @throws MessageStoreException
     */
    public CpmChatMessage addChatMessage(String cpmMessageId, Participant from, String text)
            throws CpmMessageStoreException;

    /**
     * Copy the message along with all its headers into this folder
     * 
     * @param message the message to copy
     * @throws MessageStoreException
     */
    public void copy(CpmMessage message) throws CpmMessageStoreException;

    /**
     * Copy a Conversation history folder
     * 
     * @param folder the folder to copy
     * @throws MessageStoreException
     */
    public void copy(ConversationHistoryFolder folder) throws CpmMessageStoreException;

    /**
     * Returns the session and conversation history folders contained in this user folder
     * 
     * @return
     * @throws MessageStoreException
     */
    public Set<ConversationHistoryFolder> getConversations() throws CpmMessageStoreException;

    /**
     * @param folderName
     * @return
     * @throws MessageStoreException
     */
    public UserFolder createSubFolder(String folderName) throws CpmMessageStoreException;

    /**
     * @return sub folders
     * @throws MessageStoreException
     */
    public Set<UserFolder> getUserFolders() throws CpmMessageStoreException;

    /**
     * Stored and create a Standalone Media Object
     * 
     * @param cpmMessageId
     * @param from
     * @param contentType
     * @param data
     * @return the media object
     * @throws MessageStoreException
     */
    public CpmMediaObject addMediaObject(String cpmMessageId, Participant from, String contentType,
            byte[] data) throws CpmMessageStoreException;

}
