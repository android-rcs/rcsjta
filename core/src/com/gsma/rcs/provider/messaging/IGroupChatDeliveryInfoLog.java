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

package com.gsma.rcs.provider.messaging;

import com.gsma.services.rcs.contacts.ContactId;

import android.net.Uri;

/**
 * Interface for the deliveryinfo table
 * 
 * @author LEMORDANT Philippe
 */
public interface IGroupChatDeliveryInfoLog {

    /**
     * Add a new entry (chat message or file transfer)
     * 
     * @param chatId Chat ID of a chat session
     * @param msgId Message ID of a chat message
     * @param contact Contact phone identifier
     * @param deliverySupported If delivery report is supported
     * @param displaySupported If display report is supported
     */
    public Uri addGroupChatDeliveryInfoEntry(String chatId, String msgId, ContactId contact);

    /**
     * Set delivery status for outgoing group chat messages and files
     * 
     * @param msgID Message ID
     * @param status Delivery info status
     * @param reasonCode Delivery info status reason code
     * @param contact The contact ID for which the entry is to be updated
     */
    public void setGroupChatDeliveryInfoStatusAndReasonCode(String msgId, int status,
            int reasonCode, ContactId contact);

    /**
     * Check if all recipients have received message
     * 
     * @param msgId Message ID
     * @return true If it is last contact to receive message
     */
    public boolean isDeliveredToAllRecipients(String msgId);

    /**
     * Check if all recipients have displayed message
     * 
     * @param msgId Message ID
     * @return true If it is last contact to display message
     */
    public boolean isDisplayedByAllRecipients(String msgId);
}
