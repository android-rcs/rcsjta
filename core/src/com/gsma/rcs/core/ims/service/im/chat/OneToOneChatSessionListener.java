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

package com.gsma.rcs.core.ims.service.im.chat;

import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk;
import com.gsma.services.rcs.contact.ContactId;

/**
 * One to one chat session listener
 */
public interface OneToOneChatSessionListener extends ChatSessionListener {

    /**
     * A session invitation has been received
     * 
     * @param contact Remote contact
     */
    public void handleSessionInvited(ContactId contact);

    /**
     * Chat is auto-accepted and the session is in the process of being started
     * 
     * @param contact Remote contact
     */
    public void handleSessionAutoAccepted(ContactId contact);

    /**
     * Handle Delivery report send via MSRP Failure
     * 
     * @param msgId
     * @param contact
     * @param TypeMsrpChunk
     */
    public void handleDeliveryReportSendViaMsrpFailure(String msgId, ContactId contact,
            TypeMsrpChunk chunktype);

    /**
     * Handle IM error
     * 
     * @param error Error
     * @param msgId
     * @param mimeType
     */
    public void handleImError(ChatError error, String msgId, String mimeType);
}
