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
         * Intent to check if stack deactivation/activation is allowed by the client
         */
        public static final String ACTION_GET_ACTIVATION_MODE_CHANGEABLE = "com.gsma.services.rcs.action.GET_ACTIVATION_MODE_CHANGEABLE";

        /**
         * Used as an boolean extra field in ACTION_GET_ACTIVATION_MODE_CHANGEABLE intents to
         * request the activation mode changeable.
         */
        public static final String EXTRA_GET_ACTIVATION_MODE_CHANGEABLE = "get_activation_mode_changeable";

        /**
         * Intent to check if RCS stack is activated
         */
        public static final String ACTION_GET_ACTIVATION_MODE = "com.gsma.services.rcs.action.GET_ACTIVATION_MODE";

        /**
         * Used as an boolean extra field in ACTION_GET_ACTIVATION_MODE intents to request the
         * activation mode.
         */
        public static final String EXTRA_GET_ACTIVATION_MODE = "get_activation_mode";

        /**
         * Intent to set the activation mode of the RCS stack
         */
        public static final String ACTION_SET_ACTIVATION_MODE = "com.gsma.services.rcs.action.SET_ACTIVATION_MODE";

        /**
         * Used as an boolean extra field in ACTION_SET_ACTIVATION_MODE intents to set the
         * activation mode.
         */
        public static final String EXTRA_SET_ACTIVATION_MODE = "set_activation_mode";

        /**
         * Intent to check if RCS stack is compatible with RCS API
         */
        public static final String ACTION_GET_COMPATIBLITY = "com.gsma.services.rcs.action.GET_COMPATIBLITY";

        /**
         * Used as a string extra field in ACTION_GET_COMPATIBLITY intent to convey the codename
         */
        public static final String EXTRA_GET_COMPATIBLITY_CODENAME = "get_compatibility_codename";

        /**
         * Used as an integer extra field in ACTION_GET_COMPATIBLITY intent to convey the version
         */
        public static final String EXTRA_GET_COMPATIBLITY_VERSION = "get_compatibility_version";

        /**
         * Used as an integer extra field in ACTION_GET_COMPATIBLITY intent to convey the increment
         */
        public static final String EXTRA_GET_COMPATIBLITY_INCREMENT = "get_compatibility_increment";

        /**
         * Used as an boolean extra field in ACTION_GET_COMPATIBLITY intent to convey the response
         */
        public static final String EXTRA_GET_COMPATIBLITY_RESPONSE = "get_compatibility_response";

        /**
         * Used as an string extra field in ACTION_GET_COMPATIBLITY intent to convey the service
         * class name
         */
        public static final String EXTRA_GET_COMPATIBLITY_SERVICE = "get_compatibility_service";

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

        /**
         * Load the group chat application to start a new conversation with a group of contacts and
         * send a file to them. This Intent takes into parameter a list of contact URIs (i.e.
         * content://contacts/people/contact_ID). If no parameter, the main entry of the group chat
         * application is displayed.
         */
        public static final String ACTION_INITIATE_GROUP_FILE_TRANSFER = "com.gsma.services.rcs.action.ACTION_INITIATE_GROUP_FILE_TRANSFER";

        private FileTransfer() {
        }
    }

}
