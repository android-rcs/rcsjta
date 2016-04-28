/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo.ReasonCode;
import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo.Status;

import android.net.Uri;

/**
 * Interface for the deliveryinfo table
 * 
 * @author LEMORDANT Philippe
 */
public interface IGroupDeliveryInfoLog {

    /**
     * Add a new entry (chat message or file transfer)
     * 
     * @param chatId Chat ID of a chat session
     * @param contact Contact phone identifier
     * @param msgId Message ID of a chat message
     * @param status Delivery info status
     * @param reasonCode Delivery info status reason code
     * @param timestampDisplayed Timestamp for display
     * @param timestampDelivered Timestamp for delivery
     */
    Uri addGroupChatDeliveryInfoEntry(String chatId, ContactId contact, String msgId,
            Status status, ReasonCode reasonCode, long timestampDisplayed, long timestampDelivered);

    /**
     * Set delivery status for outgoing group chat messages and files
     * 
     * @param chatId Group chat ID
     * @param contact The contact ID for which the entry is to be updated
     * @param msgId Message ID
     * @param status Delivery info status
     * @param reasonCode Delivery info status reason code
     * @return True if an entry was updated, otherwise false
     */
    boolean setGroupChatDeliveryInfoStatusAndReasonCode(String chatId, ContactId contact,
            String msgId, Status status, ReasonCode reasonCode);

    /**
     * Check if all recipients have received message
     * 
     * @param msgId Message ID
     * @return true If it is last contact to receive message
     */
    boolean isDeliveredToAllRecipients(String msgId);

    /**
     * Check if all recipients have displayed message
     * 
     * @param msgId Message ID
     * @return true If it is last contact to display message
     */
    boolean isDisplayedByAllRecipients(String msgId);

    /**
     * Set delivery info status to delivered for outgoing group chat message or file
     * 
     * @param chatId Group chat ID
     * @param contact The contact ID for which the entry is to be updated
     * @param msgId Message ID
     * @param timestampDelivered Timestamp for message delivery
     * @return True if an entry was updated, otherwise false
     */
    boolean setGroupChatDeliveryInfoDelivered(String chatId, ContactId contact, String msgId,
            long timestampDelivered);

    /**
     * Set delivery info status to displayed for outgoing group chat message or file
     * 
     * @param chatId Group chat ID
     * @param contact The contact ID for which the entry is to be updated
     * @param msgId Message ID
     * @param timestampDisplayed Timestamp for message display
     * @return True if an entry was updated, otherwise false
     */
    boolean setGroupChatDeliveryInfoDisplayed(String chatId, ContactId contact, String msgId,
            long timestampDisplayed);
}
