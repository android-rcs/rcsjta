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

package com.gsma.services.rcs;

/**
 * Intents related to rcs service activities
 * 
 * @author Jean-Marc AUFFRET
 */
public class Intents {
    /**
     * Intents for RCS service
     */
    public static class Service {
        /**
         * Intent to load the settings activity to enable or disable the service
         */
        public static final String ACTION_VIEW_SETTINGS = "com.gsma.services.rcs.action.VIEW_SETTINGS";

        /**
         * Intent to request the service status. The result is received via an Intent having the
         * following extras:
         * <ul>
         * <li> {@link #EXTRA_PACKAGENAME} containing the service package name.
         * <li> {@link #EXTRA_STATUS} containing the boolean status of the service. True means that
         * the service is activated, else the service is not activated.
         */
        public static final String ACTION_GET_STATUS = ".service.action.GET_STATUS";

        /**
         * Service package name
         */
        public final static String EXTRA_PACKAGENAME = "packageName";

        /**
         * Service status
         */
        public final static String EXTRA_STATUS = "status";

        private Service() {
        }
    }

    /**
     * Intents for chat service
     */
    public static class Chat {
        /**
         * Load the chat application to view a chat conversation. This Intent takes into parameter
         * an URI on the chat conversation (i.e. content://chats/chat_ID). If no parameter found the
         * main entry of the chat application is displayed.
         */
        public static final String ACTION_VIEW_ONE_TO_ONE_CHAT = "com.gsma.services.rcs.action.VIEW_ONE_TO_ONE_CHAT";

        /**
         * Load the chat application to send a new chat message to a given contact. This Intent
         * takes into parameter a contact URI (i.e. content://contacts/people/contact_ID). If no
         * parameter the main entry of the chat application is displayed.
         */
        public static final String ACTION_SEND_ONE_TO_ONE_CHAT_MESSAGE = "com.gsma.services.rcs.action.SEND_ONE_TO_ONE_CHAT_MESSAGE";

        /**
         * Load the group chat application. This Intent takes into parameter an URI on the group
         * chat conversation (i.e. content://chats/chat_ID). If no parameter found the main entry of
         * the group chat application is displayed.
         */
        public static final String ACTION_VIEW_GROUP_CHAT = "com.gsma.services.rcs.action.VIEW_GROUP_CHAT";

        /**
         * Load the group chat application to start a new conversation with a group of contacts.
         * This Intent takes into parameter a list of contact URIs. If no parameter the main entry
         * of the group chat application is displayed.
         */
        public static final String ACTION_INITIATE_GROUP_CHAT = "com.gsma.services.rcs.action.INITIATE_GROUP_CHAT";

        private Chat() {
        }
    }

    /**
     * Intents for file transfer service
     */
    public static class FileTransfer {
        /**
         * Load the file transfer application to view a file transfer. This Intent takes into
         * parameter an URI on the file transfer (i.e. content://filetransfers/ft_ID). If no
         * parameter found the main entry of the file transfer application is displayed.
         */
        public static final String ACTION_VIEW_FILE_TRANSFER = "com.gsma.services.rcs.action.VIEW_FILE_TRANSFER";

        /**
         * Load the file transfer application to start a new file transfer to a given contact. This
         * Intent takes into parameter a contact URI (i.e. content://contacts/people/contact_ID). If
         * no parameter the main entry of the file transfer application is displayed.
         */
        public static final String ACTION_INITIATE_ONE_TO_ONE_FILE_TRANSFER = "com.gsma.services.rcs.action.INITIATE_ONE_TO_ONE_FILE_TRANSFER";

        private FileTransfer() {
        }
    }

    /**
     * Intents for IP call service
     */
    public static class IPCall {
        /**
         * Load the IP call application to view a call. This Intent takes into parameter an URI on
         * the call (i.e. content://ipcalls/ipcall_ID). If no parameter found the main entry of the
         * IP call application is displayed.
         */
        public static final String ACTION_VIEW_IPCALL = "com.gsma.services.rcs.action.VIEW_IPCALL";

        /**
         * Load the IP call application to start a new call to a given contact. This Intent takes
         * into parameter a contact URI (i.e. content://contacts/people/contact_ID). If no parameter
         * the main entry of the IP call application is displayed.
         */
        public static final String ACTION_INITIATE_IPCALL = "com.gsma.services.rcs.action.INITIATE_IPCALL";

        private IPCall() {
        }
    }
}
