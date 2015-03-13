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
 * Represents a folder status at a requested time.
 * 
 * @see CpmObjectFolder#getCurrentStatus()
 * @see CpmObjectFolder#getStatus()
 */
public interface CpmFolderStatus {

    /**
     * UIDVALIDITY for IMAP
     * 
     * @return
     */
    public int getId();

    /**
     * Time of execution
     * 
     * @return the time
     */
    public long getTime();

    /**
     * Returns the number of messages contained in the folder
     * 
     * @return the number of messages
     */
    public int getMessageCount();

    /**
     * Returns the number of recent messages
     * 
     * @return number of recent messages
     */
    public int getRecent();

    /**
     * Returns the number of messages not flagged as Seen
     * 
     * @return the number of unseen messages
     */
    public int getUnseen();

}
