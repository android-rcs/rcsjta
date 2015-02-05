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

package com.gsma.services.rcs.ft;

/**
 * Intent for file transfer invitation
 * 
 * @author Jean-Marc AUFFRET
 */
public class FileTransferIntent {
    /**
     * Broadcast action: a new file transfer has been received.
     * <p>
     * Intent includes the following extras:
     * <ul>
     * <li> {@link #EXTRA_TRANSFER_ID} containing the unique ID of the file transfer.
     * </ul>
     */
    public final static String ACTION_NEW_INVITATION = "com.gsma.services.rcs.ft.action.NEW_FILE_TRANSFER";

    /**
     * Broadcast action: a file transfer is resuming.
     * <p>
     * Intent includes the following extras:
     * <ul>
     * <li> {@link #EXTRA_TRANSFER_ID} containing the unique ID of the file transfer.
     * </ul>
     */
    public final static String ACTION_RESUME = "com.gsma.services.rcs.ft.action.RESUME_FILE_TRANSFER";

    /**
     * Unique ID of the file transfer
     */
    public final static String EXTRA_TRANSFER_ID = "transferId";
}
