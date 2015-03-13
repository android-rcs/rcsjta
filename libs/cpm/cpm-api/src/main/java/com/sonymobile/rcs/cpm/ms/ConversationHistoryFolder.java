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

/**
 * (CPM) The conversation history folder is realized via the mailbox concept of IMAP4. A
 * conversation history folder stores all items related to a single CPM Conversation as objects
 * (e.g. message objects, file transfer history objects and standalone Media Objects) and
 * sub-folders (i.e. session history folders). The name of the conversation history folder is set to
 * the CPM Conversation Identity used in that CPM Conversation (RCS) To preserve the integrity of a
 * conversation history, Objects stored in a Conversation History folder shall not be moved to
 * another place, instead the whole folder should be moved together. An Object stored in a
 * Conversation History folder can be copied to a user defined folder, in this case it might lose
 * its association with the original conversation history. RCS Note about the relation between the
 * default folder and the RCSMessageStore root folder: From a user perspective the only difference
 * between an object or subfolder in a conversation history in the "default" system folder and a
 * similar object or subfolder in a conversation history in the "RCSMessageStore" folder is that he
 * (or his client) has selected the latter object or subfolder for permanent storage. An RCS client
 * will therefore present those messages and session histories as if they were in the same location.
 * That is in the example Message B will always be presented in the same conversational view as
 * Message A and Message C.
 * 
 * @see ConversationHistoryFolder#isPersisted()
 * @see ConversationHistoryFolder#markAsPermanent()
 */
public interface ConversationHistoryFolder extends CpmObjectFolder {

    /**
     * Returns the conversation ID which is the name of the folder.
     * 
     * @return the conversation id
     */
    public String getId();

    /**
     * Returns the list of session history folders
     * 
     * @return the list of sessions
     * @throws MessageStoreException
     */
    public Set<? extends SessionHistoryFolder> getSessionHistoryFolders()
            throws CpmMessageStoreException;

    /**
     * Returns the session history folder by contribution id
     * 
     * @param contributionId
     * @return the associated session or null if not found
     * @throws MessageStoreException
     */
    public SessionHistoryFolder getSessionHistoryFolder(String contributionId)
            throws CpmMessageStoreException;

}
