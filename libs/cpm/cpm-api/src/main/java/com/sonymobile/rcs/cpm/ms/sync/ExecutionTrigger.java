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

package com.sonymobile.rcs.cpm.ms.sync;

/**
 * Enumeration of possible synchronization strategies
 */
public enum ExecutionTrigger {
    /**
     * Disables the automatic synchronization, the client will directly request the remote server
     * for every call
     */
    NONE,
    /**
	 * 
	 */
    ON_DEMAND,
    /**
     * Only when the first connection is done
     */
    ON_INIT,
    /**
     * After each connection
     */
    ON_RECONNECT,
    /**
     * On periodic
     */
    ON_INTERVAL,
    /**
     * Everytime something has changed in the remote server. This is detected either by (IMAP) IDLE
     * mode or NOTIFY
     */
    ON_NOTIFY

}
