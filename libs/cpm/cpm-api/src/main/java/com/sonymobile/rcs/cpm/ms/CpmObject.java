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

/**
 * Per the description provided in section 5.5 of the CPM System Description [OMA-CPM-SD], a CPM
 * Message Storage Server may contain the following items. - message object, - session history
 * folder, - file transfer history object, - conversation history folder, - stand-alone Media
 * Object, - user folder, - session info object, - group state object.
 * 
 * @see CpmFileTransfer
 * @see CpmMessage
 * @see SessionInfo
 * @see CpmGroupState
 * @see CpmMediaObject
 */
public interface CpmObject {

    /**
     * Returns the IMAP UID associated to this message
     * 
     * @return the UID
     */
    public int getStorageId();

    /**
     * Returns either the Conversation or Session parent history folder
     * 
     * @return containing folder
     */
    public CpmObjectFolder getFolder();

    /**
     * Returns the containing conversation ID
     * 
     * @return
     */
    public String getConversationId();

    /**
     * Returns the contribution ID if this message belong to a session or null
     * 
     * @return
     */
    public String getContributionId();

}
