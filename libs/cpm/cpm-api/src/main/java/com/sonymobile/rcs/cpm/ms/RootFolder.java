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

public interface RootFolder {

    /**
     * Root folder must return a reference to the containing message store
     * 
     * @return
     */
    public CpmMessageStore getMessageStore();

    /**
     * Returns the Conversation History folders
     * 
     * @return conversation collection
     * @throws MessageStoreException
     */
    public Set<? extends ConversationHistoryFolder> getConversationHistoryFolders()
            throws CpmMessageStoreException;

    /**
     * Returns the conversation history folder by id
     * 
     * @param conversationId
     * @return the conversation
     * @throws MessageStoreException
     */
    public ConversationHistoryFolder getConversationHistoryFolder(String conversationId)
            throws CpmMessageStoreException;

}
