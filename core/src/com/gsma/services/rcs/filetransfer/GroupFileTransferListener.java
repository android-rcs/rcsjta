/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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

package com.gsma.services.rcs.filetransfer;

import com.gsma.services.rcs.GroupDeliveryInfo;
import com.gsma.services.rcs.contact.ContactId;

import java.util.Set;

/**
 * Group file transfer event listener
 */
public abstract class GroupFileTransferListener {

    /**
     * Callback called when the group file transfer status/reasonCode is changed.
     * 
     * @param chatId Id of chat
     * @param transferId Id of file transfer
     * @param state State of file transfer after change
     * @param reasonCode Reason code of file transfer after change
     */
    public abstract void onStateChanged(String chatId, String transferId, FileTransfer.State state,
            FileTransfer.ReasonCode reasonCode);

    /**
     * Callback called when a group file transfer state/reasonCode is changed for a single recipient
     * only.
     * 
     * @param chatId Id of chat
     * @param contact Contact ID
     * @param transferId Id of file transfer
     * @param status state of file transfer after change
     * @param reasonCode Reason code of state after change
     */
    public abstract void onDeliveryInfoChanged(String chatId, ContactId contact, String transferId,
            GroupDeliveryInfo.Status status, GroupDeliveryInfo.ReasonCode reasonCode);

    /**
     * Callback called during the transfer progress of group file transfer
     * 
     * @param chatId Id of chat
     * @param transferId Id of file transfer
     * @param currentSize Current transferred size in bytes
     * @param totalSize Total size to transfer in bytes
     */
    public abstract void onProgressUpdate(String chatId, String transferId, long currentSize,
            long totalSize);

    /**
     * Callback called when a delete operation completed that resulted in that one or several group
     * file transfers was deleted specified by the transferIds parameter corresponding to a specific
     * group chat.
     * 
     * @param chatId id of the chat
     * @param transferIds ids of those deleted file transfers
     */
    public abstract void onDeleted(String chatId, Set<String> transferIds);
}
