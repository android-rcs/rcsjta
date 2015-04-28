/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
 ******************************************************************************/

package com.orangelabs.rcs.ri.messaging.chat;

import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.chat.ChatMessage;
import com.gsma.services.rcs.chat.ChatService;

/**
 * @author YPLO6403
 */
public interface IChatView {

    /**
     * Send text message
     * 
     * @param message Message to send
     * @return Message ID or null if sending failed
     */
    ChatMessage sendMessage(String message);

    /**
     * Send geoloc message
     * 
     * @param geoloc Geoloc
     * @return Message ID or null if sending failed
     */
    ChatMessage sendMessage(Geoloc geoloc);

    /**
     * Process intent
     * 
     * @return True if operation is successful
     */
    boolean processIntent();

    /**
     * Add chat listener
     * 
     * @param chatService
     * @throws RcsServiceException
     */
    void addChatEventListener(ChatService chatService) throws RcsServiceException;

    /**
     * Remove chat listener
     * 
     * @param chatService
     * @throws RcsServiceException
     */
    void removeChatEventListener(ChatService chatService) throws RcsServiceException;

    /**
     * Is a 1-1 chat
     * 
     * @return True if single chat
     */
    boolean isSingleChat();

    /**
     * On is-composing event
     */
    void onComposing();
}
