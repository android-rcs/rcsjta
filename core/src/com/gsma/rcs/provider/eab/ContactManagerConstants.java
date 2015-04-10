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

package com.gsma.rcs.provider.eab;

import static com.gsma.rcs.provider.eab.RichAddressBookData.BLOCKED_VALUE_SET;
import static com.gsma.rcs.provider.eab.RichAddressBookData.KEY_AUTOMATA;
import static com.gsma.rcs.provider.eab.RichAddressBookData.KEY_BLOCKED;
import static com.gsma.rcs.provider.eab.RichAddressBookData.KEY_CAPABILITY_CS_VIDEO;
import static com.gsma.rcs.provider.eab.RichAddressBookData.KEY_CAPABILITY_EXTENSIONS;
import static com.gsma.rcs.provider.eab.RichAddressBookData.KEY_CAPABILITY_FILE_TRANSFER;
import static com.gsma.rcs.provider.eab.RichAddressBookData.KEY_CAPABILITY_FILE_TRANSFER_HTTP;
import static com.gsma.rcs.provider.eab.RichAddressBookData.KEY_CAPABILITY_FILE_TRANSFER_SF;
import static com.gsma.rcs.provider.eab.RichAddressBookData.KEY_CAPABILITY_FILE_TRANSFER_THUMBNAIL;
import static com.gsma.rcs.provider.eab.RichAddressBookData.KEY_CAPABILITY_GEOLOCATION_PUSH;
import static com.gsma.rcs.provider.eab.RichAddressBookData.KEY_CAPABILITY_GROUP_CHAT_SF;
import static com.gsma.rcs.provider.eab.RichAddressBookData.KEY_CAPABILITY_IMAGE_SHARING;
import static com.gsma.rcs.provider.eab.RichAddressBookData.KEY_CAPABILITY_IM_SESSION;
import static com.gsma.rcs.provider.eab.RichAddressBookData.KEY_CAPABILITY_IP_VIDEO_CALL;
import static com.gsma.rcs.provider.eab.RichAddressBookData.KEY_CAPABILITY_IP_VOICE_CALL;
import static com.gsma.rcs.provider.eab.RichAddressBookData.KEY_CAPABILITY_PRESENCE_DISCOVERY;
import static com.gsma.rcs.provider.eab.RichAddressBookData.KEY_CAPABILITY_SOCIAL_PRESENCE;
import static com.gsma.rcs.provider.eab.RichAddressBookData.KEY_CAPABILITY_TIME_LAST_REFRESH;
import static com.gsma.rcs.provider.eab.RichAddressBookData.KEY_CAPABILITY_TIME_LAST_RQST;
import static com.gsma.rcs.provider.eab.RichAddressBookData.KEY_CAPABILITY_VIDEO_SHARING;
import static com.gsma.rcs.provider.eab.RichAddressBookData.KEY_CONTACT;
import static com.gsma.rcs.provider.eab.RichAddressBookData.KEY_DISPLAY_NAME;
import static com.gsma.rcs.provider.eab.RichAddressBookData.KEY_PRESENCE_SHARING_STATUS;
import static com.gsma.rcs.provider.eab.RichAddressBookData.KEY_RCS_STATUS;
import static com.gsma.rcs.provider.eab.RichAddressBookData.KEY_REGISTRATION_STATE;

import com.gsma.rcs.addressbook.AuthenticationService;
import com.gsma.rcs.core.ims.service.ContactInfo.RcsStatus;
import com.gsma.services.rcs.contact.ContactProvider;

import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.Website;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;

/**
 * Contains constants for the Rich Address book provider.
 */
/* package private */final class ContactManagerConstants {
    /* package private */static final int INVALID_ID = -1;

    /* package private */static final long INVALID_TIME = -1L;

    // @formatter:off
    /* package private */ enum MimeType {
        NUMBER, 
        RCS_STATUS, 
        REGISTRATION_STATE, 
        CAPABILITY_IMAGE_SHARING, 
        CAPABILITY_VIDEO_SHARING, 
        CAPABILITY_IM_SESSION,
        CAPABILITY_FILE_TRANSFER, 
        CAPABILITY_GEOLOCATION_PUSH, 
        CAPABILITY_EXTENSIONS, 
        CAPABILITY_IP_VOICE_CALL, 
        CAPABILITY_IP_VIDEO_CALL
    };

    // @formatter:on

    /**
     * MIME type for contact number
     */
    /* package private */static final String MIMETYPE_NUMBER = ContactProvider.MIME_TYPE_PHONE_NUMBER;

    /**
     * MIME type for RCS status
     */
    /* package private */static final String MIMETYPE_RCS_STATUS = "vnd.android.cursor.item/com.gsma.rcs.rcs-status";

    /**
     * MIME type for RCS registration state
     */
    /* package private */static final String MIMETYPE_REGISTRATION_STATE = ContactProvider.MIME_TYPE_REGISTRATION_STATE;

    /**
     * MIME type for blocking state
     */
    /* package private */static final String MIMETYPE_BLOCKING_STATE = ContactProvider.MIME_TYPE_BLOCKING_STATE;

    /**
     * MIME type for GSMA_CS_IMAGE (image sharing) capability
     */
    /* package private */static final String MIMETYPE_CAPABILITY_IMAGE_SHARING = ContactProvider.MIME_TYPE_IMAGE_SHARING;

    /**
     * MIME type for 3GPP_CS_VOICE (video sharing) capability
     */
    /* package private */static final String MIMETYPE_CAPABILITY_VIDEO_SHARING = ContactProvider.MIME_TYPE_VIDEO_SHARING;

    /**
     * MIME type for RCS_IM (IM session) capability
     */
    /* package private */static final String MIMETYPE_CAPABILITY_IM_SESSION = ContactProvider.MIME_TYPE_IM_SESSION;

    /**
     * MIME type for RCS_FT (file transfer) capability
     */
    /* package private */static final String MIMETYPE_CAPABILITY_FILE_TRANSFER = ContactProvider.MIME_TYPE_FILE_TRANSFER;

    /**
     * MIME type for geoloc psuh capability
     */
    /* package private */static final String MIMETYPE_CAPABILITY_GEOLOCATION_PUSH = ContactProvider.MIME_TYPE_GEOLOC_PUSH;

    /**
     * MIME type for RCS extensions
     */
    /* package private */static final String MIMETYPE_CAPABILITY_EXTENSIONS = ContactProvider.MIME_TYPE_EXTENSIONS;

    /**
     * MIME type for RCS IP Voice Call capability
     */
    // TODO: Add Ipcall support here in future releases
    // /*package private */static final String MIMETYPE_CAPABILITY_IP_VOICE_CALL =
    // ContactProvider.MIME_TYPE_IP_VOICE_CALL;
    /* package private */static final String MIMETYPE_CAPABILITY_IP_VOICE_CALL = "vnd.android.cursor.item/com.gsma.services.rcs.ip-voice-call";

    /**
     * MIME type for RCS IP Video Call capability
     */
    // TODO: Add Ipcall support here in future releases
    // /*package private */static final String MIMETYPE_CAPABILITY_IP_VIDEO_CALL =
    // ContactProvider.MIME_TYPE_IP_VIDEO_CALL;
    /* package private */static final String MIMETYPE_CAPABILITY_IP_VIDEO_CALL = "vnd.android.cursor.item/com.gsma.services.rcs.ip-video-call";

    /**
     * ONLINE available status
     */
    /* package private */static final int PRESENCE_STATUS_ONLINE = 5; // StatusUpdates.AVAILABLE;

    /**
     * OFFLINE available status
     */
    /* package private */static final int PRESENCE_STATUS_OFFLINE = 0; // StatusUpdates.OFFLINE;

    /**
     * NOT SET available status
     */
    /* package private */static final int PRESENCE_STATUS_NOT_SET = 1; // StatusUpdates.INVISIBLE;

    /**
     * Account name for SIM contacts
     */
    /* package private */static final String SIM_ACCOUNT_NAME = "com.android.contacts.sim";

    /* package private */final static String NOT_SIM_ACCOUNT_SELECTION = new StringBuilder("(")
            .append(RawContacts.ACCOUNT_TYPE).append(" IS NULL OR ")
            .append(RawContacts.ACCOUNT_TYPE).append("!='").append(SIM_ACCOUNT_NAME)
            .append("') AND ").append(RawContacts._ID).append("=?").toString();

    /* package private */final static String SIM_ACCOUNT_SELECTION = new StringBuilder(
            RawContacts.ACCOUNT_TYPE).append("='").append(SIM_ACCOUNT_NAME).append("' AND ")
            .append(RawContacts._ID).append("=?").toString();

    /**
     * Contact for "Me"
     */
    /* package private */static final String MYSELF = "myself";

    /**
     * Where clause to query raw contact
     */
    /* package private */static final String SELECTION_RAW_CONTACT_MIMETYPE_DATA1 = new StringBuilder(
            Data.RAW_CONTACT_ID).append("=? AND ").append(Data.MIMETYPE).append("=? AND ")
            .append(Data.DATA1).append("=?").toString();

    /* package private */static final String WHERE_RCS_RAW_CONTACT_ID = new StringBuilder(
            AggregationData.KEY_RCS_RAW_CONTACT_ID).append("=?").toString();

    /* package private */static final String[] PROJECTION_RCS_RAW_CONTACT_ID = new String[] {
        AggregationData.KEY_RCS_RAW_CONTACT_ID
    };

    /* package private */static final String WHERE_RCS_STATUS_RCS = new StringBuilder(
            KEY_RCS_STATUS).append("!='").append(RcsStatus.NO_INFO.toInt()).append("' AND ")
            .append(KEY_RCS_STATUS).append("!='").append(RcsStatus.NOT_RCS.toInt()).append("'")
            .toString();

    /* package private */static final String WHERE_RCS_STATUS_WITH_SOCIAL_PRESENCE = new StringBuilder(
            KEY_RCS_STATUS).append("!='").append(RcsStatus.NO_INFO.toInt()).append("' AND ")
            .append(KEY_RCS_STATUS).append("!='").append(RcsStatus.NOT_RCS.toInt())
            .append("' AND ").append(KEY_RCS_STATUS).append("!='")
            .append(RcsStatus.RCS_CAPABLE.toInt()).append("'").toString();

    /* package private */static final String WHERE_RCS_RAW_CONTACT_ID_AND_RCS_NUMBER = new StringBuilder(
            AggregationData.KEY_RCS_NUMBER).append("=? AND ")
            .append(AggregationData.KEY_RAW_CONTACT_ID).append("=?").toString();

    /* package private */static final String[] PROJECTION_PRESENCE_SHARING_STATUS = new String[] {
        KEY_PRESENCE_SHARING_STATUS
    };

    /**
     * Projection to get DISPLAY_NAME from RichAddressBookProvider
     */
    /* package private */static final String[] PROJECTION_RABP_DISPLAY_NAME = new String[] {
        KEY_DISPLAY_NAME
    };

    /* package private */static final String[] PROJECTION_RCS_STATUS = new String[] {
        KEY_RCS_STATUS
    };

    /* package private */static final String[] PROJECTION_REGISTRATION_STATE = new String[] {
        KEY_REGISTRATION_STATE
    };

    /**
     * Projection to get capabilities from RichAddressBookProvider
     */
    /* package private */static final String[] PROJECTION_RABP_CAPABILITIES = new String[] {
            KEY_CAPABILITY_CS_VIDEO, KEY_CAPABILITY_FILE_TRANSFER, KEY_CAPABILITY_IMAGE_SHARING,
            KEY_CAPABILITY_IM_SESSION, KEY_CAPABILITY_PRESENCE_DISCOVERY,
            KEY_CAPABILITY_SOCIAL_PRESENCE, KEY_CAPABILITY_GEOLOCATION_PUSH,
            KEY_CAPABILITY_VIDEO_SHARING, KEY_CAPABILITY_FILE_TRANSFER_THUMBNAIL,
            KEY_CAPABILITY_FILE_TRANSFER_HTTP, KEY_CAPABILITY_IP_VOICE_CALL,
            KEY_CAPABILITY_IP_VIDEO_CALL, KEY_CAPABILITY_FILE_TRANSFER_SF,
            KEY_CAPABILITY_GROUP_CHAT_SF, KEY_AUTOMATA, KEY_CAPABILITY_EXTENSIONS,
            KEY_CAPABILITY_TIME_LAST_RQST, KEY_CAPABILITY_TIME_LAST_REFRESH
    };

    /**
     * Projection to get CONTACT from RichAddressBookProvider
     */
    /* package private */static final String[] PROJECTION_RABP = new String[] {
        KEY_CONTACT
    };

    /* package private */static final String SELECTION_RAPB_BLOCKED = new StringBuilder(KEY_BLOCKED)
            .append("=").append(BLOCKED_VALUE_SET).toString();

    /* package private */static final String SELECTION_RAW_CONTACT_FROM_NUMBER = new StringBuilder(
            Data.MIMETYPE).append("=? AND PHONE_NUMBERS_EQUAL(").append(Phone.NUMBER)
            .append(", ?)").toString();

    /* package private */static final String STRICT_SELECTION_RAW_CONTACT_FROM_NUMBER = new StringBuilder(
            Data.MIMETYPE).append("=? AND (NOT PHONE_NUMBERS_EQUAL(").append(Phone.NUMBER)
            .append(", ?) AND PHONE_NUMBERS_EQUAL(").append(Phone.NUMBER).append(", ?, 1))")
            .toString();

    /* package private */static final String[] PROJECTION_RAW_CONTACT_ID = {
        RawContacts._ID
    };

    /* package private */static final String SELECTION_RAW_CONTACT = new StringBuilder(
            RawContacts._ID).append("=?").toString();

    /* package private */static final String SELECTION_RAW_CONTACT_WITH_WEBLINK = new StringBuilder(
            Data.RAW_CONTACT_ID).append("=? AND ").append(Website.TYPE).append("=?").toString();

    /* package private */static final String[] PROJECTION_DATA_ID = new String[] {
        Data._ID
    };

    /* package private */static final String SELECTION_DATA_ID = new StringBuilder(Data._ID)
            .append("=?").toString();

    /* package private */static final String SELECTION_RAW_CONTACT_WITH_MIMETYPE = new StringBuilder(
            Data.RAW_CONTACT_ID).append("=? AND ").append(Data.MIMETYPE).append("=?").toString();

    /* package private */static final String SELECTION_RAW_CONTACT_ME = new StringBuilder(
            RawContacts.ACCOUNT_TYPE).append("='")
            .append(AuthenticationService.ACCOUNT_MANAGER_TYPE).append("' AND ")
            .append(RawContacts.SOURCE_ID).append("='").append(MYSELF).append("'").toString();

    /* package private */static final String SELECTION_DATA_MIMETYPE_CAPABILITY_FILE_TRANSFER = new StringBuilder(
            Data.MIMETYPE).append("='").append(MIMETYPE_CAPABILITY_FILE_TRANSFER).append("'")
            .toString();

    /* package private */static final String SELECTION_DATA_MIMETYPE_CAPABILITY_IM_SESSION = new StringBuilder(
            Data.MIMETYPE).append("='").append(MIMETYPE_CAPABILITY_IM_SESSION).append("'")
            .toString();

    /* package private */static final String SELECTION_DATA_MIMETYPE_CAPABILITY_IMAGE_SHARING = new StringBuilder(
            Data.MIMETYPE).append("='").append(MIMETYPE_CAPABILITY_IMAGE_SHARING).append("'")
            .toString();

    /* package private */static final String SELECTION_DATA_MIMETYPE_CAPABILITY_VIDEO_SHARING = new StringBuilder(
            Data.MIMETYPE).append("='").append(MIMETYPE_CAPABILITY_VIDEO_SHARING).append("'")
            .toString();

    /* package private */static final String SELECTION_DATA_MIMETYPE_CAPABILITY_IP_VOICE_CALL = new StringBuilder(
            Data.MIMETYPE).append("='").append(MIMETYPE_CAPABILITY_IP_VOICE_CALL).append("'")
            .toString();

    /* package private */static final String SELECTION_DATA_MIMETYPE_CAPABILITY_IP_VIDEO_CALL = new StringBuilder(
            Data.MIMETYPE).append("='").append(MIMETYPE_CAPABILITY_IP_VIDEO_CALL).append("'")
            .toString();

    /* package private */static final String SELECTION_DATA_MIMETYPE_NUMBER = new StringBuilder(
            Data.MIMETYPE).append("='").append(MIMETYPE_NUMBER).append("'").toString();

    /* package private */static final String[] PROJECTION_RAW_CONTACT_DATA1 = {
            Data.RAW_CONTACT_ID, Data.DATA1
    };

    /* package private */static final String[] PROJECTION_DATA_RAW_CONTACT = {
        Data.RAW_CONTACT_ID
    };

    /* package private */static final String SELECTION_RAW_CONTACT_ID = new StringBuilder(
            Data.RAW_CONTACT_ID).append("=?").toString();

    /* package private */static final String[] PROJECTION_RAW_CONTACT_DATA_ALL = {
            Data._ID, Data.MIMETYPE, Data.DATA1, Data.DATA2, Website.URL, Photo.PHOTO
    };

    /* package private */static final String SELECTION_RAW_CONTACT_MIME_TYPES = new StringBuilder(
            Data.RAW_CONTACT_ID).append("=? AND ").append(Data.MIMETYPE).append(" IN (")
            .append(MIMETYPE_REGISTRATION_STATE).append(",").append(MIMETYPE_BLOCKING_STATE)
            .append(",").append(MIMETYPE_NUMBER).append(",")
            .append(MIMETYPE_CAPABILITY_IMAGE_SHARING).append(",")
            .append(MIMETYPE_CAPABILITY_VIDEO_SHARING).append(",")
            .append(MIMETYPE_CAPABILITY_IP_VOICE_CALL).append(",")
            .append(MIMETYPE_CAPABILITY_IP_VIDEO_CALL).append(",")
            .append(MIMETYPE_CAPABILITY_IM_SESSION).append(",")
            .append(MIMETYPE_CAPABILITY_FILE_TRANSFER).append(",")
            .append(MIMETYPE_CAPABILITY_GEOLOCATION_PUSH).append(",")
            .append(MIMETYPE_CAPABILITY_EXTENSIONS).append(")").toString();

}
