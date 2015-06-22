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

package com.gsma.rcs.provider.contact;

import static com.gsma.rcs.provider.contact.ContactData.BLOCKED_VALUE_NOT_SET;
import static com.gsma.rcs.provider.contact.ContactData.BLOCKED_VALUE_SET;
import static com.gsma.rcs.provider.contact.ContactData.CONTENT_URI;
import static com.gsma.rcs.provider.contact.ContactData.FALSE_VALUE;
import static com.gsma.rcs.provider.contact.ContactData.KEY_AUTOMATA;
import static com.gsma.rcs.provider.contact.ContactData.KEY_BLOCKED;
import static com.gsma.rcs.provider.contact.ContactData.KEY_BLOCKING_TIMESTAMP;
import static com.gsma.rcs.provider.contact.ContactData.KEY_CAPABILITY_CS_VIDEO;
import static com.gsma.rcs.provider.contact.ContactData.KEY_CAPABILITY_EXTENSIONS;
import static com.gsma.rcs.provider.contact.ContactData.KEY_CAPABILITY_FILE_TRANSFER;
import static com.gsma.rcs.provider.contact.ContactData.KEY_CAPABILITY_FILE_TRANSFER_HTTP;
import static com.gsma.rcs.provider.contact.ContactData.KEY_CAPABILITY_FILE_TRANSFER_SF;
import static com.gsma.rcs.provider.contact.ContactData.KEY_CAPABILITY_FILE_TRANSFER_THUMBNAIL;
import static com.gsma.rcs.provider.contact.ContactData.KEY_CAPABILITY_GEOLOC_PUSH;
import static com.gsma.rcs.provider.contact.ContactData.KEY_CAPABILITY_GROUP_CHAT_SF;
import static com.gsma.rcs.provider.contact.ContactData.KEY_CAPABILITY_IMAGE_SHARE;
import static com.gsma.rcs.provider.contact.ContactData.KEY_CAPABILITY_IM_SESSION;
import static com.gsma.rcs.provider.contact.ContactData.KEY_CAPABILITY_IP_VIDEO_CALL;
import static com.gsma.rcs.provider.contact.ContactData.KEY_CAPABILITY_IP_VOICE_CALL;
import static com.gsma.rcs.provider.contact.ContactData.KEY_CAPABILITY_PRESENCE_DISCOVERY;
import static com.gsma.rcs.provider.contact.ContactData.KEY_CAPABILITY_SOCIAL_PRESENCE;
import static com.gsma.rcs.provider.contact.ContactData.KEY_CAPABILITY_TIMESTAMP_LAST_REQUEST;
import static com.gsma.rcs.provider.contact.ContactData.KEY_CAPABILITY_TIMESTAMP_LAST_RESPONSE;
import static com.gsma.rcs.provider.contact.ContactData.KEY_CAPABILITY_VIDEO_SHARE;
import static com.gsma.rcs.provider.contact.ContactData.KEY_CONTACT;
import static com.gsma.rcs.provider.contact.ContactData.KEY_DISPLAY_NAME;
import static com.gsma.rcs.provider.contact.ContactData.KEY_PRESENCE_FREE_TEXT;
import static com.gsma.rcs.provider.contact.ContactData.KEY_PRESENCE_GEOLOC_ALTITUDE;
import static com.gsma.rcs.provider.contact.ContactData.KEY_PRESENCE_GEOLOC_EXIST_FLAG;
import static com.gsma.rcs.provider.contact.ContactData.KEY_PRESENCE_GEOLOC_LATITUDE;
import static com.gsma.rcs.provider.contact.ContactData.KEY_PRESENCE_GEOLOC_LONGITUDE;
import static com.gsma.rcs.provider.contact.ContactData.KEY_PRESENCE_PHOTO_ETAG;
import static com.gsma.rcs.provider.contact.ContactData.KEY_PRESENCE_PHOTO_EXIST_FLAG;
import static com.gsma.rcs.provider.contact.ContactData.KEY_PRESENCE_SHARING_STATUS;
import static com.gsma.rcs.provider.contact.ContactData.KEY_PRESENCE_TIMESTAMP;
import static com.gsma.rcs.provider.contact.ContactData.KEY_PRESENCE_WEBLINK_NAME;
import static com.gsma.rcs.provider.contact.ContactData.KEY_PRESENCE_WEBLINK_URL;
import static com.gsma.rcs.provider.contact.ContactData.KEY_RCS_STATUS;
import static com.gsma.rcs.provider.contact.ContactData.KEY_RCS_STATUS_TIMESTAMP;
import static com.gsma.rcs.provider.contact.ContactData.KEY_REGISTRATION_STATE;
import static com.gsma.rcs.provider.contact.ContactData.KEY_TIMESTAMP_CONTACT_UPDATED;
import static com.gsma.rcs.provider.contact.ContactData.TRUE_VALUE;

import com.gsma.rcs.R;
import com.gsma.rcs.addressbook.RcsAccountManager;
import com.gsma.rcs.core.ims.service.ContactInfo;
import com.gsma.rcs.core.ims.service.ContactInfo.BlockingState;
import com.gsma.rcs.core.ims.service.ContactInfo.RcsStatus;
import com.gsma.rcs.core.ims.service.ContactInfo.RegistrationState;
import com.gsma.rcs.core.ims.service.capability.Capabilities;
import com.gsma.rcs.core.ims.service.capability.Capabilities.CapabilitiesBuilder;
import com.gsma.rcs.core.ims.service.extension.ServiceExtensionManager;
import com.gsma.rcs.core.ims.service.presence.FavoriteLink;
import com.gsma.rcs.core.ims.service.presence.Geoloc;
import com.gsma.rcs.core.ims.service.presence.PhotoIcon;
import com.gsma.rcs.core.ims.service.presence.PresenceInfo;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.contact.ContactData.AggregationData;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.StringUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.capability.CapabilitiesLog;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactProvider;

import android.accounts.AccountManager;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.AggregationExceptions;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.Website;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.StatusUpdates;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Contains utility methods for interfacing with the Android SDK ContactProvider and the RCS contact
 * provider.
 * 
 * @author Jean-Marc AUFFRET
 * @author Deutsche Telekom AG
 * @author yplo6403
 */
public final class ContactManager {

    /**
     * Invalid contact raw ID
     */
    public static final long INVALID_ID = -1;

    private static final long INVALID_TIME = -1L;

    /**
     * MIME type for contact number
     */
    private static final String MIMETYPE_NUMBER = ContactProvider.MIME_TYPE_PHONE_NUMBER;

    /**
     * MIME type for RCS registration state
     */
    private static final String MIMETYPE_REGISTRATION_STATE = ContactProvider.MIME_TYPE_REGISTRATION_STATE;

    /**
     * MIME type for blocking state
     */
    private static final String MIMETYPE_BLOCKING_STATE = ContactProvider.MIME_TYPE_BLOCKING_STATE;

    /**
     * MIME type for blocking timestamp
     */
    private static final String MIMETYPE_BLOCKING_TIMESTAMP = ContactProvider.MIME_TYPE_BLOCKING_TIMESTAMP;

    /**
     * MIME type for GSMA_CS_IMAGE (image sharing) capability
     */
    private static final String MIMETYPE_CAPABILITY_IMAGE_SHARE = ContactProvider.MIME_TYPE_IMAGE_SHARE;

    /**
     * MIME type for 3GPP_CS_VOICE (video sharing) capability
     */
    private static final String MIMETYPE_CAPABILITY_VIDEO_SHARE = ContactProvider.MIME_TYPE_VIDEO_SHARE;

    /**
     * MIME type for RCS_IM (IM session) capability
     */
    private static final String MIMETYPE_CAPABILITY_IM_SESSION = ContactProvider.MIME_TYPE_IM_SESSION;

    /**
     * MIME type for RCS_FT (file transfer) capability
     */
    private static final String MIMETYPE_CAPABILITY_FILE_TRANSFER = ContactProvider.MIME_TYPE_FILE_TRANSFER;

    /**
     * MIME type for geoloc psuh capability
     */
    private static final String MIMETYPE_CAPABILITY_GEOLOCATION_PUSH = ContactProvider.MIME_TYPE_GEOLOC_PUSH;

    /**
     * MIME type for RCS extensions
     */
    private static final String MIMETYPE_CAPABILITY_EXTENSIONS = ContactProvider.MIME_TYPE_EXTENSIONS;

    /**
     * MIME type for RCS IP Voice Call capability
     */
    // TODO: Add Ipcall support here in future releases
    // /*package private */static final String MIMETYPE_CAPABILITY_IP_VOICE_CALL =
    // ContactProvider.MIME_TYPE_IP_VOICE_CALL;
    private static final String MIMETYPE_CAPABILITY_IP_VOICE_CALL = "vnd.android.cursor.item/com.gsma.services.rcs.ip-voice-call";

    /**
     * MIME type for RCS IP Video Call capability
     */
    // TODO: Add Ipcall support here in future releases
    // /*package private */static final String MIMETYPE_CAPABILITY_IP_VIDEO_CALL =
    // ContactProvider.MIME_TYPE_IP_VIDEO_CALL;
    private static final String MIMETYPE_CAPABILITY_IP_VIDEO_CALL = "vnd.android.cursor.item/com.gsma.services.rcs.ip-video-call";

    private enum MimeType {

        // @formatter:off
        NUMBER(MIMETYPE_NUMBER),
        REGISTRATION_STATE(MIMETYPE_REGISTRATION_STATE),
        CAPABILITY_IMAGE_SHARE(MIMETYPE_CAPABILITY_IMAGE_SHARE),
        CAPABILITY_VIDEO_SHARE(MIMETYPE_CAPABILITY_VIDEO_SHARE),
        CAPABILITY_IM_SESSION(MIMETYPE_CAPABILITY_IM_SESSION),
        CAPABILITY_FILE_TRANSFER(MIMETYPE_CAPABILITY_FILE_TRANSFER),
        CAPABILITY_GEOLOCATION_PUSH(MIMETYPE_CAPABILITY_GEOLOCATION_PUSH),
        CAPABILITY_EXTENSIONS(MIMETYPE_CAPABILITY_EXTENSIONS),
        CAPABILITY_IP_VOICE_CALL(MIMETYPE_CAPABILITY_IP_VOICE_CALL),
        CAPABILITY_IP_VIDEO_CALL(MIMETYPE_CAPABILITY_IP_VIDEO_CALL),
        BLOCKING_STATE(MIMETYPE_BLOCKING_STATE),
        BLOCKING_TIMESTAMP(MIMETYPE_BLOCKING_TIMESTAMP);
        // @formatter:on

        private String mValue;

        private MimeType(String value) {
            mValue = value;
        }

        private static Map<String, MimeType> mValueToEnum = new HashMap<String, MimeType>();
        static {
            for (MimeType entry : MimeType.values()) {
                mValueToEnum.put(entry.getMimeType(), entry);
            }
        }

        /**
         * Gets string representation
         * 
         * @return String
         */
        public final String getMimeType() {
            return mValue;
        }

        /**
         * Gets the MimeType from its string representation
         * 
         * @param value the string representation
         * @return MimeType
         */
        public final static MimeType getMimeType(String value) {
            MimeType entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException(new StringBuilder("No enum const class ")
                    .append(MimeType.class.getName()).append(".").append(value).toString());
        }
    };

    /**
     * ONLINE available status
     */
    private static final int PRESENCE_STATUS_ONLINE = 5; // StatusUpdates.AVAILABLE;

    /**
     * OFFLINE available status
     */
    private static final int PRESENCE_STATUS_OFFLINE = 0; // StatusUpdates.OFFLINE;

    /**
     * NOT SET available status
     */
    private static final int PRESENCE_STATUS_NOT_SET = 1; // StatusUpdates.INVISIBLE;

    /*
     * For account persisted on SIM card, type may be "vnd.sec.contact.sim" (Samsung) or
     * "com.android.contacts.sim" (normal case)
     */
    private final static String SIM_ACCOUNT_SELECTION = new StringBuilder(RawContacts.ACCOUNT_TYPE)
            .append(" LIKE '%.sim' AND ").append(RawContacts._ID).append("=?").toString();

    /**
     * Contact for "Me"
     */
    private static final String MYSELF = "myself";

    private static final String SEL_RAW_CONTACT_MIMETYPE_DATA1 = new StringBuilder(
            Data.RAW_CONTACT_ID).append("=? AND ").append(Data.MIMETYPE).append("=? AND ")
            .append(Data.DATA1).append("=?").toString();

    private static final String WHERE_RCS_RAW_CONTACT_ID = new StringBuilder(
            AggregationData.KEY_RCS_RAW_CONTACT_ID).append("=?").toString();

    private static final String[] PROJ_RCS_RAW_CONTACT_ID = new String[] {
        AggregationData.KEY_RCS_RAW_CONTACT_ID
    };

    private static final String WHERE_RCS_STATUS_RCS = new StringBuilder(KEY_RCS_STATUS)
            .append("<>'").append(RcsStatus.NO_INFO.toInt()).append("' AND ")
            .append(KEY_RCS_STATUS).append("<>'").append(RcsStatus.NOT_RCS.toInt()).append("'")
            .toString();

    private static final String WHERE_RCS_STATUS_WITH_SOCIAL_PRESENCE = new StringBuilder(
            KEY_RCS_STATUS).append("<>'").append(RcsStatus.NO_INFO.toInt()).append("' AND ")
            .append(KEY_RCS_STATUS).append("<>'").append(RcsStatus.NOT_RCS.toInt())
            .append("' AND ").append(KEY_RCS_STATUS).append("<>'")
            .append(RcsStatus.RCS_CAPABLE.toInt()).append("'").toString();

    private static final String WHERE_RCS_RAW_CONTACT_ID_AND_RCS_NUMBER = new StringBuilder(
            AggregationData.KEY_RCS_NUMBER).append("=? AND ")
            .append(AggregationData.KEY_RAW_CONTACT_ID).append("=?").toString();

    /**
     * Projection to get CONTACT from RCS contact Provider
     */
    private static final String[] PROJ_RCSCONTACT_CONTACT = new String[] {
        KEY_CONTACT
    };

    private static final String SEL_RAW_CONTACT_FROM_NUMBER = new StringBuilder(Data.MIMETYPE)
            .append("=? AND PHONE_NUMBERS_EQUAL(").append(Phone.NUMBER).append(", ?)").toString();

    private static final String STRICT_SELECTION_RAW_CONTACT_FROM_NUMBER = new StringBuilder(
            Data.MIMETYPE).append("=? AND (NOT PHONE_NUMBERS_EQUAL(").append(Phone.NUMBER)
            .append(", ?) AND PHONE_NUMBERS_EQUAL(").append(Phone.NUMBER).append(", ?, 1))")
            .toString();

    private static final String[] PROJ_RAW_CONTACT_ID = {
        RawContacts._ID
    };

    private static final String SEL_RAW_CONTACT = new StringBuilder(RawContacts._ID).append("=?")
            .toString();

    private static final String SEL_RAW_CONTACT_WITH_WEBLINK = new StringBuilder(
            Data.RAW_CONTACT_ID).append("=? AND ").append(Website.TYPE).append("=?").toString();

    private static final String[] PROJ_DATA_ID = new String[] {
        Data._ID
    };

    private static final String SEL_DATA_ID = new StringBuilder(Data._ID).append("=?").toString();

    private static final String SEL_RAW_CONTACT_WITH_MIMETYPE = new StringBuilder(
            Data.RAW_CONTACT_ID).append("=? AND ").append(Data.MIMETYPE).append("=?").toString();

    private static final String SEL_RAW_CONTACT_ME = new StringBuilder(RawContacts.ACCOUNT_TYPE)
            .append("='").append(RcsAccountManager.ACCOUNT_MANAGER_TYPE).append("' AND ")
            .append(RawContacts.SOURCE_ID).append("='").append(MYSELF).append("'").toString();

    private static final String SEL_DATA_MIMETYPE_CAPABILITY_FILE_TRANSFER = new StringBuilder(
            Data.MIMETYPE).append("='").append(MIMETYPE_CAPABILITY_FILE_TRANSFER).append("'")
            .toString();

    private static final String SEL_DATA_MIMETYPE_CAPABILITY_IM_SESSION = new StringBuilder(
            Data.MIMETYPE).append("='").append(MIMETYPE_CAPABILITY_IM_SESSION).append("'")
            .toString();

    private static final String SEL_DATA_MIMETYPE_CAPABILITY_IMAGE_SHARING = new StringBuilder(
            Data.MIMETYPE).append("='").append(MIMETYPE_CAPABILITY_IMAGE_SHARE).append("'")
            .toString();

    private static final String SEL_DATA_MIMETYPE_CAPABILITY_VIDEO_SHARING = new StringBuilder(
            Data.MIMETYPE).append("='").append(MIMETYPE_CAPABILITY_VIDEO_SHARE).append("'")
            .toString();

    private static final String SEL_DATA_MIMETYPE_CAPABILITY_IP_VOICE_CALL = new StringBuilder(
            Data.MIMETYPE).append("='").append(MIMETYPE_CAPABILITY_IP_VOICE_CALL).append("'")
            .toString();

    private static final String SEL_DATA_MIMETYPE_CAPABILITY_IP_VIDEO_CALL = new StringBuilder(
            Data.MIMETYPE).append("='").append(MIMETYPE_CAPABILITY_IP_VIDEO_CALL).append("'")
            .toString();

    private static final String SEL_DATA_MIMETYPE_CAPABILITY_EXTENSIONS = new StringBuilder(
            Data.MIMETYPE).append("='").append(MIMETYPE_CAPABILITY_EXTENSIONS).append("'")
            .toString();

    private static final String SEL_DATA_MIMETYPE_NUMBER = new StringBuilder(Data.MIMETYPE)
            .append("='").append(MIMETYPE_NUMBER).append("'").toString();

    private static final String[] PROJ_RAW_CONTACT_DATA1 = {
            Data.RAW_CONTACT_ID, Data.DATA1
    };

    private static final String[] PROJ_DATA_RAW_CONTACT = {
        Data.RAW_CONTACT_ID
    };

    private final String[] PHONE_PROJ_NUMBER_RAW_CONTACT_ID = {
            Phone.NUMBER, Phone.RAW_CONTACT_ID
    };

    private static final String SEL_RAW_CONTACT_ID = new StringBuilder(Data.RAW_CONTACT_ID).append(
            "=?").toString();

    private static final String[] PROJ_RAW_CONTACT_DATA_ALL = {
            Data._ID, Data.MIMETYPE, Data.DATA1, Data.DATA2, Website.URL, Photo.PHOTO
    };

    private static final String SEL_RAW_CONTACT_MIME_TYPES = new StringBuilder(Data.RAW_CONTACT_ID)
            .append("=? AND ").append(Data.MIMETYPE).append(" IN ('")
            .append(MIMETYPE_REGISTRATION_STATE).append("','").append(MIMETYPE_BLOCKING_STATE)
            .append("','").append(MIMETYPE_BLOCKING_TIMESTAMP).append("','")
            .append(MIMETYPE_NUMBER).append("','").append(MIMETYPE_CAPABILITY_IMAGE_SHARE)
            .append("','").append(MIMETYPE_CAPABILITY_VIDEO_SHARE).append("','")
            .append(MIMETYPE_CAPABILITY_IP_VOICE_CALL).append("','")
            .append(MIMETYPE_CAPABILITY_IP_VIDEO_CALL).append("','")
            .append(MIMETYPE_CAPABILITY_IM_SESSION).append("','")
            .append(MIMETYPE_CAPABILITY_FILE_TRANSFER).append("','")
            .append(MIMETYPE_CAPABILITY_GEOLOCATION_PUSH).append("','")
            .append(MIMETYPE_CAPABILITY_EXTENSIONS).append("')").toString();

    private static final String RCS_CONTACT_GROUP_NAME = RcsAccountManager.ACCOUNT_MANAGER_TYPE;

    private static final String[] PROJ_CONTACT_GROUP_ID = new String[] {
        Groups._ID
    };

    private static final String SEL_RCS_CONTACT_GROUP_ACCOUNT_TYPE = new StringBuilder(
            Groups.ACCOUNT_TYPE).append("='").append(RCS_CONTACT_GROUP_NAME).append("'").toString();

    /**
     * Current instance
     */
    private static volatile ContactManager sInstance;

    private final ContentResolver mContentResolver;

    private final LocalContentResolver mLocalContentResolver;

    private final Context mContext;

    private final RcsSettings mRcsSettings;

    private final Map<ContactId, ContactInfo> mContactInfoCache;

    private static final Logger sLogger = Logger.getLogger(ContactManager.class.getSimpleName());

    /**
     * Create a singleton instance of ContactManager
     * 
     * @param ctx Application context
     * @param contentResolver Content resolver
     * @param localContentResolver Local content resolver
     * @param rcsSettings The accessor to RCS settings
     * @return singleton instance of ContactManager
     */
    public static ContactManager createInstance(Context ctx, ContentResolver contentResolver,
            LocalContentResolver localContentResolver, RcsSettings rcsSettings) {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (ContactManager.class) {
            if (sInstance == null) {
                sInstance = new ContactManager(ctx, contentResolver, localContentResolver,
                        rcsSettings);
            }
            return sInstance;
        }
    }

    /**
     * Constructor
     * 
     * @param context Application context
     * @param contentResolver Content resolver
     * @param localContentResolver Local content resolver
     * @param rcsSettings
     */
    private ContactManager(Context context, ContentResolver contentResolver,
            LocalContentResolver localContentResolver, RcsSettings rcsSettings) {
        mContext = context;
        mContentResolver = contentResolver;
        mLocalContentResolver = localContentResolver;
        mContactInfoCache = new HashMap<ContactId, ContactInfo>();
        mRcsSettings = rcsSettings;
    }

    /**
     * Returns my presence info from the RCS contact provider
     * 
     * @return Presence info or null in case of error
     */
    public PresenceInfo getMyPresenceInfo() {
        if (!mRcsSettings.isSocialPresenceSupported()) {
            return new PresenceInfo();
        }
        Cursor cursor = getRawContactDataCursor(getRawContactIdForMe());
        CursorUtil.assertCursorIsNotNull(cursor, Data.CONTENT_URI);
        return getContactInfoFromCursor(cursor).getPresenceInfo();
    }

    /**
     * Set the info of a contact
     * 
     * @param newInfo New contact info
     * @param oldInfo Old contact info
     * @throws ContactManagerException
     */
    private void setContactInfo(ContactInfo newInfo, ContactInfo oldInfo)
            throws ContactManagerException {
        ContactId contact = newInfo.getContact();
        String contactNumber = contact.toString();
        boolean logActivated = sLogger.isActivated();
        /* Update contactInfo cache with new contact information */
        mContactInfoCache.put(contact, newInfo);

        /* Check if we have an entry for the contact */
        boolean hasEntryInRcsContactAddressBook = isContactIdAssociatedWithRcsContactProvider(contact);

        ContentValues values = new ContentValues();
        values.put(KEY_CONTACT, contactNumber);

        /* Save RCS status */
        values.put(KEY_RCS_STATUS, newInfo.getRcsStatus().toInt());
        values.put(KEY_RCS_STATUS_TIMESTAMP, newInfo.getRcsStatusTimestamp());

        /* Save capabilities */
        Capabilities newCapa = newInfo.getCapabilities();
        values.put(KEY_CAPABILITY_CS_VIDEO, newCapa.isCsVideoSupported());
        values.put(KEY_CAPABILITY_FILE_TRANSFER, newCapa.isFileTransferSupported());
        values.put(KEY_CAPABILITY_IMAGE_SHARE, newCapa.isImageSharingSupported());
        values.put(KEY_CAPABILITY_IM_SESSION, newCapa.isImSessionSupported());
        values.put(KEY_CAPABILITY_VIDEO_SHARE, newCapa.isVideoSharingSupported());
        values.put(KEY_CAPABILITY_GEOLOC_PUSH, newCapa.isGeolocationPushSupported());
        values.put(KEY_CAPABILITY_FILE_TRANSFER_HTTP, newCapa.isFileTransferHttpSupported());
        values.put(KEY_CAPABILITY_FILE_TRANSFER_THUMBNAIL,
                newCapa.isFileTransferThumbnailSupported());
        values.put(KEY_CAPABILITY_IP_VOICE_CALL, newCapa.isIPVoiceCallSupported());
        values.put(KEY_CAPABILITY_IP_VIDEO_CALL, newCapa.isIPVideoCallSupported());
        values.put(KEY_CAPABILITY_FILE_TRANSFER_SF, newCapa.isFileTransferStoreForwardSupported());
        values.put(KEY_AUTOMATA, newCapa.isSipAutomata());
        values.put(KEY_CAPABILITY_GROUP_CHAT_SF, newCapa.isGroupChatStoreForwardSupported());
        values.put(KEY_CAPABILITY_EXTENSIONS,
                ServiceExtensionManager.getExtensions(newCapa.getSupportedExtensions()));
        values.put(KEY_CAPABILITY_TIMESTAMP_LAST_REQUEST, newCapa.getTimestampOfLastRequest());
        values.put(KEY_CAPABILITY_TIMESTAMP_LAST_RESPONSE, newCapa.getTimestampOfLastResponse());
        values.put(KEY_CAPABILITY_PRESENCE_DISCOVERY, newCapa.isPresenceDiscoverySupported());
        values.put(KEY_CAPABILITY_SOCIAL_PRESENCE, newCapa.isSocialPresenceSupported());

        PhotoIcon photoIcon = null;

        /* Save presence information */
        PresenceInfo newPresenceInfo = newInfo.getPresenceInfo();
        if (newPresenceInfo != null) {
            values.put(KEY_PRESENCE_SHARING_STATUS, newPresenceInfo.getPresenceStatus());
            values.put(KEY_PRESENCE_FREE_TEXT, newPresenceInfo.getFreetext());
            FavoriteLink favLink = newPresenceInfo.getFavoriteLink();
            if (favLink == null) {
                values.put(KEY_PRESENCE_WEBLINK_NAME, "");
                values.put(KEY_PRESENCE_WEBLINK_URL, "");
            } else {
                values.put(KEY_PRESENCE_WEBLINK_NAME, favLink.getName());
                values.put(KEY_PRESENCE_WEBLINK_URL, favLink.getLink());
            }

            Geoloc geoloc = newPresenceInfo.getGeoloc();
            if (geoloc == null) {
                values.put(KEY_PRESENCE_GEOLOC_EXIST_FLAG, FALSE_VALUE);
                values.put(KEY_PRESENCE_GEOLOC_LATITUDE, 0);
                values.put(KEY_PRESENCE_GEOLOC_LONGITUDE, 0);
                values.put(KEY_PRESENCE_GEOLOC_ALTITUDE, 0);
            } else {
                values.put(KEY_PRESENCE_GEOLOC_EXIST_FLAG, TRUE_VALUE);
                values.put(KEY_PRESENCE_GEOLOC_LATITUDE, geoloc.getLatitude());
                values.put(KEY_PRESENCE_GEOLOC_LONGITUDE, geoloc.getLongitude());
                values.put(KEY_PRESENCE_GEOLOC_ALTITUDE, geoloc.getAltitude());
            }
            values.put(KEY_PRESENCE_TIMESTAMP, newPresenceInfo.getTimestamp());

            photoIcon = newPresenceInfo.getPhotoIcon();
            if (photoIcon == null) {
                values.put(KEY_PRESENCE_PHOTO_ETAG, "");
                values.put(KEY_PRESENCE_PHOTO_EXIST_FLAG, FALSE_VALUE);
            } else {
                if (photoIcon.getContent() != null) {
                    values.put(KEY_PRESENCE_PHOTO_EXIST_FLAG, TRUE_VALUE);
                } else {
                    values.put(KEY_PRESENCE_PHOTO_EXIST_FLAG, FALSE_VALUE);
                }
                values.put(KEY_PRESENCE_PHOTO_ETAG, photoIcon.getEtag());
            }
        } else {
            values.put(KEY_PRESENCE_TIMESTAMP, INVALID_TIME);
        }

        /* Save blocking state */
        if (BlockingState.BLOCKED == newInfo.getBlockingState()) {
            values.put(KEY_BLOCKED, BLOCKED_VALUE_SET);
            values.put(KEY_BLOCKING_TIMESTAMP, newInfo.getBlockingTimestamp());
        } else {
            values.put(KEY_BLOCKED, BLOCKED_VALUE_NOT_SET);
            values.put(KEY_BLOCKING_TIMESTAMP, INVALID_TIME);
        }

        values.put(KEY_TIMESTAMP_CONTACT_UPDATED, System.currentTimeMillis());

        /* Save the registration state */
        values.put(KEY_REGISTRATION_STATE, newInfo.getRegistrationState().toInt());

        if (hasEntryInRcsContactAddressBook) {
            if (logActivated) {
                sLogger.info("Update RCS contact ".concat(contactNumber));
            }
            /* RCS contact already exists in provider: update RABP entry */
            Uri uri = Uri.withAppendedPath(CONTENT_URI, contactNumber);
            mLocalContentResolver.update(uri, values, null, null);
        } else {
            if (logActivated) {
                sLogger.info("Insert new contact ".concat(contactNumber));
            }
            /* RCS contact does not exists in provider: insert entry in RABP */
            mLocalContentResolver.insert(CONTENT_URI, values);
        }

        /* Save presence photo content */
        if (photoIcon != null) {
            savePhotoIcon(photoIcon, contact);
        }

        /* Get all the IDs from raw contacts that have this phone number */
        Set<Long> rawContactIds = getRawContactIdsFromPhoneNumber(contact);
        if (rawContactIds.isEmpty()) {
            /*
             * If the number is not in the native address book, work is done.
             */
            return;
        }
        boolean isRcsContact = newInfo.isRcsContact();
        /* For each entries in the native address book, prepare the modifications */
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        for (Long rawContactId : rawContactIds) {
            /* Get the associated RCS raw contact ID */
            long rcsRawContactId = getAssociatedRcsRawContact(rawContactId, contact);

            if (!isRcsContact) {
                /*
                 * If the contact is not a RCS contact anymore, we delete the corresponding native
                 * raw contacts.
                 */
                ops.add(ContentProviderOperation.newDelete(RawContacts.CONTENT_URI)
                        .withSelection(SEL_RAW_CONTACT, new String[] {
                            Long.toString(rcsRawContactId)
                        }).build());
                /* Also delete the corresponding entries in the RCS aggregation provider. */
                mLocalContentResolver.delete(AggregationData.CONTENT_URI, WHERE_RCS_RAW_CONTACT_ID,
                        new String[] {
                            Long.toString(rcsRawContactId)
                        });
            } else {
                /* If the contact is still a RCS contact, we update the native raw contacts. */
                if (INVALID_ID == rcsRawContactId) {
                    /*
                     * If no RCS raw contact ID is associated to the raw contact, create one with
                     * the right information.
                     */
                    rcsRawContactId = createRcsRawContact(newInfo, rawContactId);
                    /* Nothing to modify, as the new contact will have taken the new information. */
                    continue;
                }

                /* Modify the contact type */
                if (newInfo.getRcsStatus() != oldInfo.getRcsStatus()) {
                    /* Update status in RCS contact provider if it has changed */
                    updateRcsStatusOrCreateNewContact(contact, newInfo.getRcsStatus());
                }

                /* Modify the capabilities */
                Capabilities oldCapa = oldInfo.getCapabilities();
                ContentProviderOperation op = modifyMimeTypeForContact(rcsRawContactId, contact,
                        MIMETYPE_CAPABILITY_FILE_TRANSFER, newCapa.isFileTransferSupported(),
                        oldCapa.isFileTransferSupported());
                if (op != null) {
                    ops.add(op);
                }
                op = modifyMimeTypeForContact(rcsRawContactId, contact,
                        MIMETYPE_CAPABILITY_IMAGE_SHARE, newCapa.isImageSharingSupported(),
                        oldCapa.isImageSharingSupported());
                if (op != null) {
                    ops.add(op);
                }
                op = modifyMimeTypeForContact(rcsRawContactId, contact,
                        MIMETYPE_CAPABILITY_IM_SESSION, newCapa.isImSessionSupported(),
                        oldCapa.isImSessionSupported());
                if (op != null) {
                    ops.add(op);
                }
                op = modifyMimeTypeForContact(rcsRawContactId, contact,
                        MIMETYPE_CAPABILITY_VIDEO_SHARE, newCapa.isVideoSharingSupported(),
                        oldCapa.isVideoSharingSupported());
                if (op != null) {
                    ops.add(op);
                }
                op = modifyMimeTypeForContact(rcsRawContactId, contact,
                        MIMETYPE_CAPABILITY_IP_VOICE_CALL, newCapa.isIPVoiceCallSupported(),
                        oldCapa.isIPVoiceCallSupported());
                if (op != null) {
                    ops.add(op);
                }
                op = modifyMimeTypeForContact(rcsRawContactId, contact,
                        MIMETYPE_CAPABILITY_IP_VIDEO_CALL, newCapa.isIPVideoCallSupported(),
                        oldCapa.isIPVideoCallSupported());
                if (op != null) {
                    ops.add(op);
                }
                op = modifyMimeTypeForContact(rcsRawContactId, contact,
                        MIMETYPE_CAPABILITY_GEOLOCATION_PUSH, newCapa.isGeolocationPushSupported(),
                        oldCapa.isGeolocationPushSupported());
                if (op != null) {
                    ops.add(op);
                }
                /* Modify the RCS extensions */
                op = modifyExtensionsCapabilityForContact(rcsRawContactId, contact,
                        newCapa.getSupportedExtensions(), oldCapa.getSupportedExtensions());
                if (op != null) {
                    ops.add(op);
                }
                /* Modify the Blocking state */
                op = modifyBlockingStateForContact(rcsRawContactId, contact,
                        newInfo.getBlockingState(), oldInfo.getBlockingState());
                if (op != null) {
                    ops.add(op);
                }

                /* Modify the Blocking timestamp */
                op = modifyBlockingTimestampForContact(rcsRawContactId, contact,
                        newInfo.getBlockingTimestamp(), oldInfo.getBlockingTimestamp());
                if (op != null) {
                    ops.add(op);
                }

                /* New contact registration state */
                String newFreeText = "";
                if (newInfo.getPresenceInfo() != null) {
                    newFreeText = newInfo.getPresenceInfo().getFreetext();
                }
                /* Old contact registration state */
                String oldFreeText = "";
                if (oldInfo.getPresenceInfo() != null) {
                    oldFreeText = oldInfo.getPresenceInfo().getFreetext();
                }
                List<ContentProviderOperation> registrationOps = modifyContactRegistrationState(
                        rcsRawContactId, contact, newInfo.getRegistrationState(),
                        oldInfo.getRegistrationState(), newFreeText, oldFreeText);
                for (ContentProviderOperation registrationOperation : registrationOps) {
                    if (registrationOperation != null) {
                        ops.add(registrationOperation);
                    }
                }

                /* Presence fields */
                List<ContentProviderOperation> presenceOps = modifyPresenceForContact(
                        rcsRawContactId, contact, newInfo.getPresenceInfo(),
                        oldInfo.getPresenceInfo());
                for (ContentProviderOperation presenceOperation : presenceOps) {
                    if (presenceOperation != null) {
                        ops.add(presenceOperation);
                    }
                }
            }
        }

        if (!ops.isEmpty()) {
            if (logActivated) {
                sLogger.info("Execute " + ops.size()
                        + " operations to update native address book for contact " + contactNumber
                        + " (for raw contact IDs: " + Arrays.toString(rawContactIds.toArray())
                        + ")");
            }
            /* Do the actual database modifications */
            try {
                mContentResolver.applyBatch(ContactsContract.AUTHORITY, ops);
            } catch (RemoteException e) {
                throw new ContactManagerException(e);
            } catch (OperationApplicationException e) {
                throw new ContactManagerException(e);
            }
        }
    }

    /**
     * Get the contact info from the RCS contact provider
     * 
     * @param contact Contact
     * @return Contact info
     */
    private ContactInfo getContactInfoFromProvider(ContactId contact) {
        ContactInfo infos = new ContactInfo();
        infos.setRcsStatus(RcsStatus.NO_INFO);
        infos.setRcsStatusTimestamp(System.currentTimeMillis());
        infos.setContact(contact);

        CapabilitiesBuilder capaBuilder = new CapabilitiesBuilder();

        PresenceInfo presenceInfo = new PresenceInfo();

        infos.setRegistrationState(RegistrationState.UNKNOWN);

        infos.setBlockingState(BlockingState.NOT_BLOCKED);
        infos.setBlockingTimestamp(INVALID_TIME);

        Cursor cursor = null;
        String contactNumber = contact.toString();
        Uri uri = Uri.withAppendedPath(CONTENT_URI, contactNumber);
        try {
            cursor = mLocalContentResolver.query(uri, null, null, null, null);
            CursorUtil.assertCursorIsNotNull(cursor, uri);
            if (cursor.moveToFirst()) {
                // Get RCS display name
                infos.setDisplayName(cursor.getString(cursor
                        .getColumnIndexOrThrow(KEY_DISPLAY_NAME)));

                // Get RCS Status
                int rcsStatus = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_RCS_STATUS));
                infos.setRcsStatus(RcsStatus.valueOf(rcsStatus));

                infos.setRcsStatusTimestamp(cursor.getLong(cursor
                        .getColumnIndexOrThrow(KEY_RCS_STATUS_TIMESTAMP)));

                int registrationState = cursor.getInt(cursor
                        .getColumnIndexOrThrow(KEY_REGISTRATION_STATE));
                infos.setRegistrationState(RegistrationState.valueOf(registrationState));

                int blockingState = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_BLOCKED));
                infos.setBlockingState(BlockingState.valueOf(blockingState));

                long blockingTimestamp = cursor.getLong(cursor
                        .getColumnIndexOrThrow(KEY_BLOCKING_TIMESTAMP));
                infos.setBlockingTimestamp(blockingTimestamp);

                // Get Presence info
                presenceInfo.setPresenceStatus(cursor.getString(cursor
                        .getColumnIndexOrThrow(KEY_PRESENCE_SHARING_STATUS)));

                FavoriteLink favLink = new FavoriteLink(cursor.getString(cursor
                        .getColumnIndexOrThrow(KEY_PRESENCE_WEBLINK_NAME)), cursor.getString(cursor
                        .getColumnIndexOrThrow(KEY_PRESENCE_WEBLINK_URL)));
                presenceInfo.setFavoriteLink(favLink);
                presenceInfo.setFavoriteLinkUrl(favLink.getLink());

                presenceInfo.setFreetext(cursor.getString(cursor
                        .getColumnIndexOrThrow(KEY_PRESENCE_FREE_TEXT)));

                Geoloc geoloc = null;
                if (Boolean.parseBoolean(cursor.getString(cursor
                        .getColumnIndexOrThrow(KEY_PRESENCE_GEOLOC_EXIST_FLAG)))) {
                    geoloc = new Geoloc(cursor.getDouble(cursor
                            .getColumnIndexOrThrow(KEY_PRESENCE_GEOLOC_LATITUDE)),
                            cursor.getDouble(cursor
                                    .getColumnIndexOrThrow(KEY_PRESENCE_GEOLOC_LONGITUDE)),
                            cursor.getDouble(cursor
                                    .getColumnIndexOrThrow(KEY_PRESENCE_GEOLOC_ALTITUDE)));
                }
                presenceInfo.setGeoloc(geoloc);

                presenceInfo.setTimestamp(cursor.getLong(cursor
                        .getColumnIndexOrThrow(KEY_PRESENCE_TIMESTAMP)));

                if (Boolean.parseBoolean(cursor.getString(cursor
                        .getColumnIndexOrThrow(KEY_PRESENCE_PHOTO_EXIST_FLAG)))) {
                    presenceInfo.setPhotoIcon(getPhotoIcon(cursor, contact));
                }

                // Get the capabilities infos
                capaBuilder.setCsVideo(isCapabilitySupported(cursor, KEY_CAPABILITY_CS_VIDEO));
                capaBuilder.setFileTransfer(isCapabilitySupported(cursor,
                        KEY_CAPABILITY_FILE_TRANSFER));
                capaBuilder.setImageSharing(isCapabilitySupported(cursor,
                        KEY_CAPABILITY_IMAGE_SHARE));
                capaBuilder.setImSession(isCapabilitySupported(cursor, KEY_CAPABILITY_IM_SESSION));
                capaBuilder.setPresenceDiscovery(isCapabilitySupported(cursor,
                        KEY_CAPABILITY_PRESENCE_DISCOVERY));
                capaBuilder.setSocialPresence(isCapabilitySupported(cursor,
                        KEY_CAPABILITY_SOCIAL_PRESENCE));
                capaBuilder.setGeolocationPush(isCapabilitySupported(cursor,
                        KEY_CAPABILITY_GEOLOC_PUSH));
                capaBuilder.setVideoSharing(isCapabilitySupported(cursor,
                        KEY_CAPABILITY_VIDEO_SHARE));
                capaBuilder.setFileTransferThumbnail(isCapabilitySupported(cursor,
                        KEY_CAPABILITY_FILE_TRANSFER_THUMBNAIL));
                capaBuilder.setFileTransferHttp(isCapabilitySupported(cursor,
                        KEY_CAPABILITY_FILE_TRANSFER_HTTP));
                capaBuilder.setIpVoiceCall(isCapabilitySupported(cursor,
                        KEY_CAPABILITY_IP_VOICE_CALL));
                capaBuilder.setIpVideoCall(isCapabilitySupported(cursor,
                        KEY_CAPABILITY_IP_VIDEO_CALL));
                capaBuilder.setFileTransferStoreForward(isCapabilitySupported(cursor,
                        KEY_CAPABILITY_FILE_TRANSFER_SF));
                capaBuilder.setGroupChatStoreForward(isCapabilitySupported(cursor,
                        KEY_CAPABILITY_GROUP_CHAT_SF));
                capaBuilder.setSipAutomata(isCapabilitySupported(cursor, KEY_AUTOMATA));
                capaBuilder.setExtensions(ServiceExtensionManager.getExtensions(cursor
                        .getString(cursor.getColumnIndexOrThrow(KEY_CAPABILITY_EXTENSIONS))));
                capaBuilder.setTimestampOfLastRequest(cursor.getLong(cursor
                        .getColumnIndexOrThrow(KEY_CAPABILITY_TIMESTAMP_LAST_REQUEST)));
                capaBuilder.setTimestampOfLastResponse(cursor.getLong(cursor
                        .getColumnIndexOrThrow(KEY_CAPABILITY_TIMESTAMP_LAST_RESPONSE)));
            }
            infos.setPresenceInfo(presenceInfo);
            infos.setCapabilities(capaBuilder.build());
            return infos;

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Get the infos of a contact in the RCS contact provider
     * 
     * @param contact Contact
     * @return Contact info
     */
    public ContactInfo getContactInfo(ContactId contact) {
        ContactInfo contactInfo = mContactInfoCache.get(contact);
        if (contactInfo == null) {
            contactInfo = getContactInfoFromProvider(contact);
            mContactInfoCache.put(contact, contactInfo);
        }
        return contactInfo;
    }

    /**
     * Save photo icon
     * 
     * @param photoIcon
     * @param contact
     */
    private void savePhotoIcon(PhotoIcon photoIcon, ContactId contact) {
        byte photoContent[] = photoIcon.getContent();
        if (photoContent == null) {
            return;
        }
        Uri photoUri = Uri.withAppendedPath(CONTENT_URI, contact.toString());
        OutputStream outstream = null;
        try {
            outstream = mLocalContentResolver.openContentOutputStream(photoUri);
            outstream.write(photoContent);
            outstream.flush();
        } catch (IOException e) {
            if (sLogger.isActivated()) {
                sLogger.error("Photo can't be saved", e);
            }
        } finally {
            if (outstream != null) {
                try {
                    outstream.close();
                } catch (Exception e2) {
                }
            }
        }
    }

    /**
     * Get photo icon from RCS contact provider
     * 
     * @param cursor
     * @param contact
     * @return PhotoIcon or null
     */
    private PhotoIcon getPhotoIcon(Cursor cursor, ContactId contact) {
        Uri photoUri = Uri.withAppendedPath(CONTENT_URI, contact.toString());
        try {
            String etag = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PRESENCE_PHOTO_ETAG));
            InputStream stream = mLocalContentResolver.openContentInputStream(photoUri);
            byte[] content = new byte[stream.available()];
            stream.read(content, 0, content.length);
            Bitmap bmp = BitmapFactory.decodeByteArray(content, 0, content.length);
            if (bmp != null) {
                return new PhotoIcon(content, bmp.getWidth(), bmp.getHeight(), etag);
            }
        } catch (IOException e) {
            if (sLogger.isActivated()) {
                sLogger.error("Can't get the photo", e);
            }
        }
        return null;
    }

    /**
     * Get sharing status of a contact
     * 
     * @param contact Contact
     * @return sharing status or NO_INFO if cannot be retrieved
     */
    private RcsStatus getContactSharingStatus(ContactId contact) {
        return getContactInfo(contact).getRcsStatus();
    }

    /**
     * Block a contact
     * 
     * @param contact Contact ID
     * @throws ContactManagerException exception thrown if update operation has failed
     */
    public void blockContact(ContactId contact) throws ContactManagerException {
        if (sLogger.isActivated()) {
            sLogger.info("Block contact ".concat(contact.toString()));
        }
        ContactInfo oldInfo = getContactInfo(contact);
        ContactInfo newInfo = new ContactInfo(oldInfo);
        newInfo.setRcsStatus(RcsStatus.BLOCKED);
        setContactInfo(newInfo, oldInfo);
    }

    /**
     * Flush the RCS contact provider
     */
    public void flushRcsContactProvider() {
        if (sLogger.isActivated()) {
            sLogger.debug("clear ContactInfo cache");
        }
        mContactInfoCache.clear();
        mLocalContentResolver.delete(CONTENT_URI, null, null);
    }

    /**
     * Update status if contact exists in RCS provider. If it does not already exists, create a new
     * contact with default attributes.
     * 
     * @param contact Contact ID
     * @param rcsStatus RCS status
     */
    public void updateRcsStatusOrCreateNewContact(ContactId contact, RcsStatus rcsStatus) {
        long currentTime = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put(KEY_PRESENCE_SHARING_STATUS, rcsStatus.toInt());
        values.put(KEY_TIMESTAMP_CONTACT_UPDATED, currentTime);
        if (isContactIdAssociatedWithRcsContactProvider(contact)) {
            /* Contact already present, update. */
            Uri uri = Uri.withAppendedPath(CONTENT_URI, contact.toString());
            mLocalContentResolver.update(uri, values, null, null);
        } else {
            /* Contact not present in provider, insert. */
            values.put(KEY_CONTACT, contact.toString());
            values.put(KEY_RCS_STATUS, rcsStatus.toInt());
            values.put(KEY_RCS_STATUS_TIMESTAMP, currentTime);
            values.put(KEY_REGISTRATION_STATE, RegistrationState.UNKNOWN.toInt());
            values.put(KEY_PRESENCE_TIMESTAMP, -1);
            values.put(KEY_CAPABILITY_TIMESTAMP_LAST_REQUEST, Capabilities.INVALID_TIMESTAMP);
            values.put(KEY_CAPABILITY_CS_VIDEO, 0);
            values.put(KEY_CAPABILITY_IMAGE_SHARE, 0);
            values.put(KEY_CAPABILITY_VIDEO_SHARE, 0);
            values.put(KEY_CAPABILITY_IM_SESSION, 0);
            values.put(KEY_CAPABILITY_FILE_TRANSFER, 0);
            values.put(KEY_CAPABILITY_PRESENCE_DISCOVERY, 0);
            values.put(KEY_CAPABILITY_SOCIAL_PRESENCE, 0);
            values.put(KEY_CAPABILITY_GEOLOC_PUSH, 0);
            values.put(KEY_CAPABILITY_FILE_TRANSFER_HTTP, 0);
            values.put(KEY_CAPABILITY_FILE_TRANSFER_THUMBNAIL, 0);
            values.put(KEY_CAPABILITY_IP_VOICE_CALL, 0);
            values.put(KEY_CAPABILITY_IP_VIDEO_CALL, 0);
            values.put(KEY_CAPABILITY_FILE_TRANSFER_SF, 0);
            values.put(KEY_CAPABILITY_GROUP_CHAT_SF, 0);
            values.put(KEY_BLOCKED, BLOCKED_VALUE_NOT_SET);
            values.put(KEY_BLOCKING_TIMESTAMP, INVALID_TIME);
            values.put(KEY_AUTOMATA, 0);
            values.put(KEY_CAPABILITY_TIMESTAMP_LAST_RESPONSE, Capabilities.INVALID_TIMESTAMP);
            mLocalContentResolver.insert(CONTENT_URI, values);
        }
        getContactInfo(contact).setRcsStatus(rcsStatus);
    }

    /**
     * Get the RCS contacts in the RCS contact provider which have a presence relationship with the
     * user
     * 
     * @return set containing all RCS contacts, "Me" item excluded
     */
    public Set<ContactId> getRcsContactsWithSocialPresence() {
        Set<ContactId> rcsNumbers = new HashSet<ContactId>();
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(CONTENT_URI, PROJ_RCSCONTACT_CONTACT,
                    WHERE_RCS_STATUS_WITH_SOCIAL_PRESENCE, null, null);
            CursorUtil.assertCursorIsNotNull(cursor, CONTENT_URI);
            if (!cursor.moveToFirst()) {
                return rcsNumbers;
            }
            int contactColumnIdx = cursor.getColumnIndexOrThrow(KEY_CONTACT);
            do {
                String contact = cursor.getString(contactColumnIdx);
                /* Do not check validity of trusted data */
                rcsNumbers.add(ContactUtil.createContactIdFromTrustedData(contact));
            } while (cursor.moveToNext());
            return rcsNumbers;

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Gets the RCS contacts from RCS contact provider
     * 
     * @return set containing all RCS contacts
     */
    public Set<ContactId> getRcsContactsFromRcsContactProvider() {
        Set<ContactId> rcsNumbers = new HashSet<ContactId>();
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(CONTENT_URI, PROJ_RCSCONTACT_CONTACT,
                    WHERE_RCS_STATUS_RCS, null, null);
            CursorUtil.assertCursorIsNotNull(cursor, CONTENT_URI);
            if (!cursor.moveToFirst()) {
                return rcsNumbers;
            }
            int contactColumnIdx = cursor.getColumnIndexOrThrow(KEY_CONTACT);
            do {
                String contact = cursor.getString(contactColumnIdx);
                /* Do no check validity of trusted data */
                rcsNumbers.add(ContactUtil.createContactIdFromTrustedData(contact));
            } while (cursor.moveToNext());
            return rcsNumbers;

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Get all contacts from RCS contact provider
     * 
     * @return set containing all contacts that have been at least queried once for capabilities
     */
    public Set<ContactId> getAllContactsFromRcsContactProvider() {
        Set<ContactId> numbers = new HashSet<ContactId>();
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(CONTENT_URI, PROJ_RCSCONTACT_CONTACT, null, null,
                    null);
            CursorUtil.assertCursorIsNotNull(cursor, CONTENT_URI);
            if (!cursor.moveToFirst()) {
                return numbers;
            }
            int contactColumnIdx = cursor.getColumnIndexOrThrow(KEY_CONTACT);
            do {
                String contact = cursor.getString(contactColumnIdx);
                /* Do not check validity of trusted data */
                numbers.add(ContactUtil.createContactIdFromTrustedData(contact));
            } while (cursor.moveToNext());
            return numbers;

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Is the number in the RCS blocked list
     * 
     * @param contact contact to check
     * @return boolean
     */
    public boolean isNumberBlocked(ContactId contact) {
        return RcsStatus.BLOCKED.equals(getContactSharingStatus(contact));
    }

    /**
     * Is the number in the RCS buddy list
     * 
     * @param contact contact to check
     * @return boolean
     */
    public boolean isNumberShared(ContactId contact) {
        return RcsStatus.ACTIVE.equals(getContactSharingStatus(contact));
    }

    /**
     * Has the number been invited to RCS
     * 
     * @param contact contact to check
     * @return boolean
     */
    public boolean isNumberInvited(ContactId contact) {
        return RcsStatus.PENDING.equals(getContactSharingStatus(contact));
    }

    /**
     * Has the number invited us to RCS
     * 
     * @param contact contact to check
     * @return boolean
     */
    public boolean isNumberWilling(ContactId contact) {
        return RcsStatus.PENDING_OUT.equals(getContactSharingStatus(contact));
    }

    /**
     * Has the number invited us to RCS then be cancelled
     * 
     * @param contact contact to check
     * @return boolean
     */
    public boolean isNumberCancelled(ContactId contact) {
        return RcsStatus.CANCELLED.equals(getContactSharingStatus(contact));
    }

    /**
     * Modify the corresponding mimetype row for the contact
     * 
     * @param rawContactId Raw contact id of the RCS contact
     * @param number RCS number of the contact
     * @param mimeType Mime type associated to the capability
     * @param newState True if the capability must be enabled, else false
     * @param oldState True if the capability was enabled, else false
     * @return ContentProviderOperation to be done or null if state is unchanged
     */
    private ContentProviderOperation modifyMimeTypeForContact(long rawContactId,
            ContactId rcsNumber, String mimeType, boolean newState, boolean oldState) {
        if (newState == oldState) {
            /* Nothing to do */
            return null;
        }
        if (newState == true) {
            /* We have to insert a new data in the raw contact */
            return insertMimeTypeForContact(rawContactId, rcsNumber, mimeType);
        }
        /* We have to remove the data from the raw contact */
        return deleteMimeTypeForContact(rawContactId, rcsNumber, mimeType);
    }

    /**
     * Create (first time) the corresponding mimetype row for the contact
     * 
     * @param rawContactId
     * @param rcsNumber
     * @param mimeType
     * @return ContentProviderOperation to be done
     */
    private ContentProviderOperation createMimeTypeForContact(int rawContactId,
            ContactId rcsNumber, String mimeType) {
        String mimeTypeDescriptionSummary = getMimeTypeDescriptionSummary(mimeType);
        /* Check if there is a mimetype description to be added */
        if (mimeTypeDescriptionSummary != null) {
            String mimeTypeDescriptionDetails = getMimeTypeDescriptionDetails(mimeType);
            return ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, rawContactId)
                    .withValue(Data.MIMETYPE, mimeType).withValue(Data.DATA1, rcsNumber.toString())
                    .withValue(Data.DATA2, mimeTypeDescriptionSummary)
                    .withValue(Data.DATA3, mimeTypeDescriptionDetails).build();
        }
        return ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, rawContactId)
                .withValue(Data.MIMETYPE, mimeType).withValue(Data.DATA1, rcsNumber.toString())
                .build();
    }

    /**
     * Insert the corresponding mimetype row for the contact
     * 
     * @param rawContactId
     * @param rcsNumber
     * @param mimeType
     * @return ContentProviderOperation to be done
     */
    private ContentProviderOperation insertMimeTypeForContact(long rawContactId,
            ContactId rcsNumber, String mimeType) {
        /* Check if there is a mime-type description to be added */
        String mimeTypeDescriptionSummary = getMimeTypeDescriptionSummary(mimeType);
        if (mimeTypeDescriptionSummary != null) {
            String mimeTypeDescriptionDetails = getMimeTypeDescriptionDetails(mimeType);
            return ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValue(Data.RAW_CONTACT_ID, rawContactId)
                    .withValue(Data.MIMETYPE, mimeType).withValue(Data.DATA1, rcsNumber.toString())
                    .withValue(Data.DATA2, mimeTypeDescriptionSummary)
                    .withValue(Data.DATA3, mimeTypeDescriptionDetails).build();
        }
        return ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValue(Data.RAW_CONTACT_ID, rawContactId).withValue(Data.MIMETYPE, mimeType)
                .withValue(Data.DATA1, rcsNumber.toString()).build();
    }

    /**
     * Remove the corresponding mimetype row for the contact
     * 
     * @param rawContactId
     * @param rcsNumber
     * @param mimeType
     * @return ContentProviderOperation to be done
     */
    private ContentProviderOperation deleteMimeTypeForContact(long rawContactId,
            ContactId rcsNumber, String mimeType) {
        // We have to remove a data from the raw contact
        return ContentProviderOperation.newDelete(Data.CONTENT_URI)
                .withSelection(SEL_RAW_CONTACT_MIMETYPE_DATA1, new String[] {
                        String.valueOf(rawContactId), mimeType, rcsNumber.toString()
                }).build();
    }

    /**
     * Modify the registration state for the contact
     * 
     * @param rawContactId Raw contact id of the RCS contact
     * @param number RCS number of the contact
     * @param newRegistrationState
     * @param oldRegistrationState
     * @param newFreeText
     * @param oldFreeText
     * @return list of ContentProviderOperations to be done
     */
    private List<ContentProviderOperation> modifyContactRegistrationState(long rawContactId,
            ContactId rcsNumber, RegistrationState newRegistrationState,
            RegistrationState oldRegistrationState, String newFreeText, String oldFreeText) {

        List<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        boolean registrationChanged = true;
        if ((newRegistrationState == oldRegistrationState || RegistrationState.UNKNOWN == newRegistrationState)) {
            registrationChanged = false;
        }

        if (registrationChanged) {
            // Modify registration status
            ops.add(ContentProviderOperation
                    .newUpdate(Data.CONTENT_URI)
                    .withSelection(
                            SEL_RAW_CONTACT_MIMETYPE_DATA1,
                            new String[] {
                                    Long.toString(rawContactId), MIMETYPE_REGISTRATION_STATE,
                                    rcsNumber.toString()
                            }).withValue(Data.DATA2, newRegistrationState.toInt()).build());
        }

        if (StringUtils.equals(newFreeText, oldFreeText) && !registrationChanged) {
            return ops;
        }

        int availability = PRESENCE_STATUS_NOT_SET;
        if (RegistrationState.ONLINE == newRegistrationState) {
            availability = PRESENCE_STATUS_ONLINE;
        } else if (RegistrationState.OFFLINE == newRegistrationState) {
            availability = PRESENCE_STATUS_OFFLINE;
        }

        // Get the id of the status update data linked to this raw contact id
        long dataId = INVALID_ID;
        String[] selectionArgs = {
            Long.toString(rawContactId)
        };
        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(Data.CONTENT_URI, PROJ_DATA_ID, SEL_RAW_CONTACT_ID,
                    selectionArgs, null);
            CursorUtil.assertCursorIsNotNull(cursor, Data.CONTENT_URI);
            if (cursor.moveToNext()) {
                dataId = cursor.getLong(cursor.getColumnIndexOrThrow(Data._ID));
            }
            ops.add(ContentProviderOperation
                    .newInsert(StatusUpdates.CONTENT_URI)
                    .withValue(StatusUpdates.DATA_ID, dataId)
                    .withValue(StatusUpdates.STATUS, newFreeText)
                    .withValue(StatusUpdates.STATUS_RES_PACKAGE, mContext.getPackageName())
                    .withValue(StatusUpdates.STATUS_LABEL, R.string.rcs_core_account_id)
                    .withValue(StatusUpdates.STATUS_ICON, R.drawable.rcs_icon)
                    .withValue(StatusUpdates.PRESENCE, availability)
                    // Needed for inserting PRESENCE
                    .withValue(StatusUpdates.PROTOCOL, Im.PROTOCOL_CUSTOM)
                    .withValue(StatusUpdates.CUSTOM_PROTOCOL, " " /* Intentional left blank */)
                    .withValue(StatusUpdates.STATUS_TIMESTAMP, System.currentTimeMillis()).build());
            return ops;

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Modify the registration state for the contact
     * 
     * @param rawContactId Raw contact id of the RCS contact
     * @param newRegistrationState
     * @param oldRegistrationState
     * @param newFreeText
     * @param oldFreeText
     * @return list of ContentProviderOperations to be done
     */
    private List<ContentProviderOperation> modifyContactRegistrationStateForMyself(
            long rawContactId, RegistrationState newRegistrationState,
            RegistrationState oldRegistrationState, String newFreeText, String oldFreeText) {
        List<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        boolean registrationChanged = true;
        if (newRegistrationState == oldRegistrationState
                || RegistrationState.UNKNOWN.equals(newRegistrationState)) {
            registrationChanged = false;
        }
        if (registrationChanged) {
            // Modify registration state
            ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                    .withSelection(SEL_RAW_CONTACT_MIMETYPE_DATA1, new String[] {
                            Long.toString(rawContactId), MIMETYPE_REGISTRATION_STATE, MYSELF
                    }).withValue(Data.DATA2, newRegistrationState.toInt()).build());
        }
        if (StringUtils.equals(newFreeText, oldFreeText) && !registrationChanged) {
            return ops;
        }
        int availability = PRESENCE_STATUS_NOT_SET;
        if (RegistrationState.ONLINE.equals(newRegistrationState)) {
            availability = PRESENCE_STATUS_ONLINE;
        } else if (RegistrationState.OFFLINE.equals(newRegistrationState)) {
            availability = PRESENCE_STATUS_OFFLINE;
        }
        // Get the id of the status update data linked to this raw contact id
        long dataId = INVALID_ID;
        Cursor cur = getRawContactDataCursor(rawContactId);
        if (cur != null && cur.moveToNext()) {
            dataId = cur.getLong(cur.getColumnIndex(Data._ID));
        }
        ops.add(ContentProviderOperation
                .newInsert(StatusUpdates.CONTENT_URI)
                .withValue(StatusUpdates.DATA_ID, dataId)
                .withValue(StatusUpdates.STATUS, newFreeText)
                .withValue(StatusUpdates.STATUS_RES_PACKAGE, mContext.getPackageName())
                .withValue(StatusUpdates.STATUS_LABEL, R.string.rcs_core_account_id)
                .withValue(StatusUpdates.STATUS_ICON, R.drawable.rcs_icon)
                .withValue(StatusUpdates.PRESENCE, availability)
                // Needed for inserting PRESENCE
                .withValue(StatusUpdates.PROTOCOL, Im.PROTOCOL_CUSTOM)
                .withValue(StatusUpdates.CUSTOM_PROTOCOL,
                // Intentional left blank
                        " ").withValue(StatusUpdates.STATUS_TIMESTAMP, System.currentTimeMillis())
                .build());
        return ops;
    }

    /**
     * Modify the RCS extensions capability for the contact
     * 
     * @param rawContactId Raw contact id of the RCS contact
     * @param contact RCS number of the contact
     * @param newExtensions New extensions capabilities
     * @param oldExtensions Old extensions capabilities
     * @return contentProviderOperation to be done of null if no update is required
     */
    private ContentProviderOperation modifyExtensionsCapabilityForContact(long rawContactId,
            ContactId contact, Set<String> newExtensions, Set<String> oldExtensions) {
        /* Compare the two lists of extensions */
        if (newExtensions.containsAll(oldExtensions) && oldExtensions.containsAll(newExtensions)) {
            /* Both lists have the same tags, no need to update */
            return null;
        }
        return ContentProviderOperation
                .newUpdate(Data.CONTENT_URI)
                .withSelection(
                        SEL_RAW_CONTACT_MIMETYPE_DATA1,
                        new String[] {
                                Long.toString(rawContactId), MIMETYPE_CAPABILITY_EXTENSIONS,
                                contact.toString()
                        })
                .withValue(Data.DATA2, ServiceExtensionManager.getExtensions(newExtensions))
                .build();
    }

    /**
     * Modify blocking state for contact
     * 
     * @param rawContactIdRaw contact id of the RCS contact
     * @param contact RCS number of the contact
     * @param newBlockingState new blocking state
     * @param oldBlockingState old blocking state
     * @return contentProviderOperation to be done of null if no update is required
     */
    private ContentProviderOperation modifyBlockingStateForContact(long rawContactId,
            ContactId contact, BlockingState newBlockingState, BlockingState oldBlockingState) {
        if (newBlockingState == oldBlockingState) {
            return null;
        }
        return ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                .withSelection(SEL_RAW_CONTACT_MIMETYPE_DATA1, new String[] {
                        Long.toString(rawContactId), MIMETYPE_BLOCKING_STATE, contact.toString()
                }).withValue(Data.DATA2, newBlockingState.toInt()).build();
    }

    /**
     * Modify blocking timestamp for contact
     * 
     * @param rawContactIdRaw contact id of the RCS contact
     * @param contact RCS number of the contact
     * @param newBlockingState new blocking timestamp
     * @param oldBlockingState old blocking timestamp
     * @return contentProviderOperation to be done of null if no update is required
     */
    private ContentProviderOperation modifyBlockingTimestampForContact(long rawContactId,
            ContactId contact, long newBlockingTimestamp, long oldBlockingTimestamp) {
        if (newBlockingTimestamp == oldBlockingTimestamp) {
            return null;
        }
        return ContentProviderOperation
                .newUpdate(Data.CONTENT_URI)
                .withSelection(
                        SEL_RAW_CONTACT_MIMETYPE_DATA1,
                        new String[] {
                                Long.toString(rawContactId), MIMETYPE_BLOCKING_TIMESTAMP,
                                contact.toString()
                        }).withValue(Data.DATA2, newBlockingTimestamp).build();
    }

    /**
     * Modify the presence info for a contact
     * 
     * @param rawContactId Raw contact id of the RCS contact
     * @param contact RCS number of the contact
     * @param newPresenceInfo
     * @param oldPresenceInfo
     * @return list of ContentProviderOperation to be done
     */
    private List<ContentProviderOperation> modifyPresenceForContact(long rawContactId,
            ContactId contact, PresenceInfo newPresenceInfo, PresenceInfo oldPresenceInfo) {
        List<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        if (newPresenceInfo != null && oldPresenceInfo != null) {
            if (!StringUtils.equals(newPresenceInfo.getFavoriteLinkUrl(),
                    oldPresenceInfo.getFavoriteLinkUrl())) {

                // Add the weblink to the native @book
                ContentValues values = new ContentValues();
                values.put(Data.RAW_CONTACT_ID, rawContactId);
                values.put(Website.URL, newPresenceInfo.getFavoriteLinkUrl());
                values.put(Website.TYPE, Website.TYPE_HOMEPAGE);
                values.put(Data.IS_PRIMARY, 1);
                values.put(Data.IS_SUPER_PRIMARY, 1);

                // Get the id of the current weblink mimetype
                long currentNativeWebLinkDataId = getCurrentNativeWebLinkDataId(rawContactId);
                if (oldPresenceInfo.getFavoriteLinkUrl() == null) {
                    // There was no weblink, insert
                    ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValues(values)
                            .build());
                } else if (newPresenceInfo.getFavoriteLinkUrl() != null) {
                    // Update the existing weblink
                    ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                            .withSelection(SEL_DATA_ID, new String[] {
                                String.valueOf(currentNativeWebLinkDataId)
                            }).withValues(values).build());
                } else {
                    // Remove the existing weblink
                    ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI)
                            .withSelection(SEL_DATA_ID, new String[] {
                                String.valueOf(currentNativeWebLinkDataId)
                            }).build());
                }
            }
            // Set the photo-icon
            PhotoIcon oldPhotoIcon = oldPresenceInfo.getPhotoIcon();
            PhotoIcon newPhotoIcon = newPresenceInfo.getPhotoIcon();
            // Check if photo etags are the same between the two presenceInfo
            boolean haveSameEtags = false;
            String oldPhotoIconEtag = null;
            String newPhotoIconEtag = null;
            if (oldPhotoIcon != null) {
                oldPhotoIconEtag = oldPhotoIcon.getEtag();
            }
            if (newPhotoIcon != null) {
                newPhotoIconEtag = newPhotoIcon.getEtag();
            }
            if (oldPhotoIconEtag == null && newPhotoIconEtag == null) {
                haveSameEtags = true;
            } else if (oldPhotoIconEtag != null && newPhotoIconEtag != null) {
                haveSameEtags = (oldPhotoIconEtag.equalsIgnoreCase(newPhotoIconEtag));
            }
            if (!haveSameEtags) {
                // Not the same etag, so photo changed
                // Replace photo and etag
                List<ContentProviderOperation> photoOps = setContactPhoto(rawContactId,
                        newPhotoIcon, true);
                for (ContentProviderOperation photoOp : photoOps) {
                    if (photoOp != null) {
                        ops.add(photoOp);
                    }
                }
            }
        } else if (newPresenceInfo != null) {
            // The new presence info is not null but the old one was, add new fields
            RegistrationState availability = RegistrationState.UNKNOWN;
            if (newPresenceInfo.isOnline()) {
                availability = RegistrationState.ONLINE;
            } else if (newPresenceInfo.isOffline()) {
                availability = RegistrationState.OFFLINE;
            }

            // Add the presence status to native address book
            List<ContentProviderOperation> registrationStateOps = modifyContactRegistrationState(
                    rawContactId, contact, availability, RegistrationState.UNKNOWN,
                    newPresenceInfo.getFreetext(), "");
            for (ContentProviderOperation registrationStateOp : registrationStateOps) {
                if (registrationStateOp != null) {
                    ops.add(registrationStateOp);
                }
            }

            // Add the weblink to the native @book
            ContentValues values = new ContentValues();
            values.put(Data.RAW_CONTACT_ID, rawContactId);
            values.put(Website.URL, newPresenceInfo.getFavoriteLinkUrl());
            values.put(Website.TYPE, Website.TYPE_HOMEPAGE);
            values.put(Data.IS_PRIMARY, 1);
            values.put(Data.IS_SUPER_PRIMARY, 1);

            // Get the id of the current weblink mimetype
            long currentNativeWebLinkDataId = getCurrentNativeWebLinkDataId(rawContactId);

            if (oldPresenceInfo.getFavoriteLinkUrl() == null) {
                // There was no weblink, insert
                ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValues(values)
                        .build());
            } else if (newPresenceInfo.getFavoriteLinkUrl() != null) {
                // Update the existing weblink
                ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                        .withSelection(SEL_DATA_ID, new String[] {
                            String.valueOf(currentNativeWebLinkDataId)
                        }).withValues(values).build());
            } else {
                // Remove the existing weblink
                ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI)
                        .withSelection(SEL_DATA_ID, new String[] {
                            String.valueOf(currentNativeWebLinkDataId)
                        }).build());
            }

            // Set the photo
            List<ContentProviderOperation> photoOps = setContactPhoto(rawContactId,
                    newPresenceInfo.getPhotoIcon(), true);
            for (ContentProviderOperation photoOp : photoOps) {
                if (photoOp != null) {
                    ops.add(photoOp);
                }
            }

        } else if (oldPresenceInfo != null) {
            // The new presence info is null but the old one was not, remove fields

            // Remove the presence status to native address book
            // Force presence status to offline and free text to null
            List<ContentProviderOperation> registrationStateOps = modifyContactRegistrationState(
                    rawContactId, contact, RegistrationState.OFFLINE, RegistrationState.UNKNOWN,
                    "", oldPresenceInfo.getFreetext());
            for (ContentProviderOperation registrationStateOp : registrationStateOps) {
                if (registrationStateOp != null) {
                    ops.add(registrationStateOp);
                }
            }

            // Remove presence web link in native address book
            // Add the weblink to the native @book
            // Get the id of the current weblink mimetype
            long currentNativeWebLinkDataId = getCurrentNativeWebLinkDataId(rawContactId);

            ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI)
                    .withSelection(SEL_DATA_ID, new String[] {
                        String.valueOf(currentNativeWebLinkDataId)
                    }).build());

            // Set the photo
            List<ContentProviderOperation> photoOps = setContactPhoto(rawContactId, null, true);
            for (ContentProviderOperation photoOp : photoOps) {
                if (photoOp != null) {
                    ops.add(photoOp);
                }
            }
        }
        return ops;
    }

    /**
     * Get the id of the current weblink mimetype
     * 
     * @param rawContactId
     * @return id
     */
    private long getCurrentNativeWebLinkDataId(long rawContactId) {
        Cursor cur = null;
        try {
            cur = mContentResolver.query(Data.CONTENT_URI, PROJ_DATA_ID,
                    SEL_RAW_CONTACT_WITH_WEBLINK, new String[] {
                            Long.toString(rawContactId), String.valueOf(Website.TYPE_HOMEPAGE)
                    }, null);
            CursorUtil.assertCursorIsNotNull(cur, Data.CONTENT_URI);

            if (cur.moveToNext()) {
                return cur.getLong(cur.getColumnIndexOrThrow(Data._ID));
            }
            return INVALID_ID;
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
    }

    /**
     * Get description associated to a MIME type. This string will be visible in the contact card
     * 
     * @param mimeType MIME type
     * @return String
     */
    private String getMimeTypeDescriptionSummary(String mimeType) {
        if (mimeType.equalsIgnoreCase(MIMETYPE_CAPABILITY_FILE_TRANSFER)) {
            return mContext.getString(R.string.rcs_core_contact_file_transfer_summary);

        } else if (mimeType.equalsIgnoreCase(MIMETYPE_CAPABILITY_IM_SESSION)) {
            return mContext.getString(R.string.rcs_core_contact_im_session_summary);

        } else if (mimeType.equalsIgnoreCase(MIMETYPE_CAPABILITY_IP_VOICE_CALL)) {
            return mContext.getString(R.string.rcs_core_contact_ip_voice_call_summary);

        } else if (mimeType.equalsIgnoreCase(MIMETYPE_CAPABILITY_IP_VIDEO_CALL)) {
            return mContext.getString(R.string.rcs_core_contact_ip_video_call_summary);

        } else if (mimeType.equalsIgnoreCase(MIMETYPE_CAPABILITY_IMAGE_SHARE)) {
            return mContext.getString(R.string.rcs_core_contact_image_share_summary);

        } else if (mimeType.equalsIgnoreCase(MIMETYPE_CAPABILITY_VIDEO_SHARE)) {
            return mContext.getString(R.string.rcs_core_contact_video_share_summary);
        } else
            return null;
    }

    /**
     * Get description associated to a MIME type. This string will be visible in the contact card
     * 
     * @param mimeType MIME type
     * @return String
     */
    private String getMimeTypeDescriptionDetails(String mimeType) {
        if (mimeType.equalsIgnoreCase(MIMETYPE_CAPABILITY_FILE_TRANSFER)) {
            return mContext.getString(R.string.rcs_core_contact_file_transfer_details);

        } else if (mimeType.equalsIgnoreCase(MIMETYPE_CAPABILITY_IM_SESSION)) {
            return mContext.getString(R.string.rcs_core_contact_im_session_details);

        } else if (mimeType.equalsIgnoreCase(MIMETYPE_CAPABILITY_IP_VOICE_CALL)) {
            return mContext.getString(R.string.rcs_core_contact_ip_voice_call_details);

        } else if (mimeType.equalsIgnoreCase(MIMETYPE_CAPABILITY_IP_VIDEO_CALL)) {
            return mContext.getString(R.string.rcs_core_contact_ip_video_call_details);

        } else if (mimeType.equalsIgnoreCase(MIMETYPE_CAPABILITY_EXTENSIONS)) {
            return mContext.getString(R.string.rcs_core_contact_extensions_details);

        } else if (mimeType.equalsIgnoreCase(MIMETYPE_CAPABILITY_IMAGE_SHARE)) {
            return mContext.getString(R.string.rcs_core_contact_image_share_details);

        } else if (mimeType.equalsIgnoreCase(MIMETYPE_CAPABILITY_VIDEO_SHARE)) {
            return mContext.getString(R.string.rcs_core_contact_video_share_details);
        } else
            return null;
    }

    /**
     * Sets capabilities for contact
     * 
     * @param contact Contact Id
     * @param capabilities Capabilities to set
     */
    public void setContactCapabilities(ContactId contact, Capabilities capabilities) {
        setContactCapabilities(contact, capabilities, getContactStatus(contact),
                getRegistrationState(contact));
    }

    /**
     * Set contact capabilities
     * 
     * @param contact Contact Id
     * @param capabilities Capabilities
     * @param contactType Contact type
     * @param registrationState Three possible values : online/offline/unknown
     */
    public void setContactCapabilities(ContactId contact, Capabilities capabilities,
            RcsStatus contactType, RegistrationState registrationState) {
        /* Get the current information on this contact */
        ContactInfo oldInfo = getContactInfo(contact);
        ContactInfo newInfo = new ContactInfo(oldInfo);

        /* Set the contact type */
        newInfo.setRcsStatus(contactType);

        /* Set the registration state */
        newInfo.setRegistrationState(registrationState);

        /* Modify the capabilities regarding the registration state */
        boolean isRegistered = RegistrationState.ONLINE == registrationState;
        boolean isRcsContact = newInfo.isRcsContact();

        CapabilitiesBuilder capaBuilder = new CapabilitiesBuilder(capabilities);
        capaBuilder.setCsVideo(capaBuilder.isCsVideoSupported() && isRegistered);

        /*
         * File transfer. This capability is enabled: - if the capability is present and the contact
         * is registered - if the FT S&F is enabled and the contact is RCS capable.
         */
        capaBuilder.setFileTransfer(capaBuilder.isFileTransferSupported() && isRegistered
                || mRcsSettings.isFileTransferStoreForwardSupported() && isRcsContact);

        capaBuilder.setImageSharing(capaBuilder.isImageSharingSupported() && isRegistered);

        /*
         * IM session capability is enabled: - if the capability is present and the contact is
         * registered - if the IM store&forward is enabled and the contact is RCS capable.
         */
        capaBuilder.setImSession(capaBuilder.isImSessionSupported() && isRegistered
                || mRcsSettings.isImAlwaysOn() && isRcsContact);

        capaBuilder.setVideoSharing(capaBuilder.isVideoSharingSupported() && isRegistered);

        capaBuilder.setGeolocationPush(capaBuilder.isGeolocationPushSupported() && isRegistered);

        capaBuilder.setFileTransferThumbnail(capaBuilder.isFileTransferThumbnailSupported()
                && isRegistered);

        capaBuilder.setFileTransferHttp(capaBuilder.isFileTransferHttpSupported() && isRegistered
                || mRcsSettings.isFtHttpCapAlwaysOn() && isRcsContact);

        capaBuilder.setFileTransferStoreForward(capaBuilder.isFileTransferStoreForwardSupported()
                && isRegistered || mRcsSettings.isFtAlwaysOn() && isRcsContact);

        capaBuilder.setGroupChatStoreForward(capaBuilder.isGroupChatStoreForwardSupported()
                && isRegistered);

        capaBuilder.setIpVoiceCall(capaBuilder.isIPVoiceCallSupported() && isRegistered);

        capaBuilder.setIpVideoCall(capaBuilder.isIPVideoCallSupported() && isRegistered);

        capaBuilder.setPresenceDiscovery(capaBuilder.isPresenceDiscovery() && isRegistered);

        capaBuilder.setSocialPresence(capaBuilder.isSocialPresence() && isRegistered);

        if (!isRegistered) {
            /* If contact is not registered, do not put any extensions */
            capaBuilder.setExtensions(new HashSet<String>());
        }

        /* Add the capabilities */
        newInfo.setCapabilities(capaBuilder.build());

        /* Do not set contact info if they are unchanged */
        if (newInfo.getCapabilities().equals(oldInfo.getCapabilities())
                && newInfo.getRcsStatus() == oldInfo.getRcsStatus()
                && newInfo.getRegistrationState() == oldInfo.getRegistrationState()) {
            return;
        }

        /* Save the modifications */
        try {
            setContactInfo(newInfo, oldInfo);
        } catch (ContactManagerException e) {
            if (sLogger.isActivated()) {
                sLogger.error("Could not save the contact modifications", e);
            }
        }
    }

    /**
     * Get the RCS status for contact
     * 
     * @param contact
     * @return status
     */
    private RcsStatus getContactStatus(ContactId contact) {
        return getContactInfo(contact).getRcsStatus();
    }

    /**
     * Get the registration state for contact
     * 
     * @param contact
     * @return registration state
     */
    private RegistrationState getRegistrationState(ContactId contact) {
        return getContactInfo(contact).getRegistrationState();
    }

    /**
     * Get contact capabilities <br>
     * If contact has never been enriched with capability, returns null
     * 
     * @param contact Contact Id
     * @return capabilities or null if capabilities do not exist
     */
    public Capabilities getContactCapabilities(ContactId contact) {
        if (RcsStatus.NO_INFO.equals(getContactStatus(contact))) {
            return null;
        }
        return getContactInfo(contact).getCapabilities();
    }

    /**
     * Update time of last capabilities request for contact
     * 
     * @param contact Contact Id
     */
    public void updateCapabilitiesTimeLastRequest(ContactId contact) {
        String contactNumber = contact.toString();
        if (sLogger.isActivated()) {
            sLogger.debug("Update time of last capabilities request for ".concat(contactNumber));
        }
        ContactInfo contactInfo = getContactInfo(contact);
        Capabilities capabilities = contactInfo.getCapabilities();
        if (capabilities == null) {
            return;
        }
        long now = System.currentTimeMillis();
        /* Update the cache */
        CapabilitiesBuilder capaBuilder = new CapabilitiesBuilder(capabilities);
        capaBuilder.setTimestampOfLastRequest(now);
        contactInfo.setCapabilities(capaBuilder.build());

        ContentValues values = new ContentValues();
        values.put(KEY_CAPABILITY_TIMESTAMP_LAST_REQUEST, now);
        Uri uri = Uri.withAppendedPath(CONTENT_URI, contactNumber);
        mLocalContentResolver.update(uri, values, null, null);
    }

    /**
     * Utility method to create new "RCS" raw contact, that aggregates with other raw contact
     * 
     * @param info for the RCS raw contact
     * @param rawContactId of the raw contact we want to aggregate the RCS infos to
     * @return the RCS rawContactId concerning this newly created contact
     */
    private long createRcsRawContact(final ContactInfo info, final long rawContactId) {
        ContactId contact = info.getContact();
        String contactNumber = contact.toString();
        if (sLogger.isActivated()) {
            sLogger.debug("Create RCS rawcontact for '" + contactNumber
                    + "' associated to rawContactId " + rawContactId);
        }
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        /* Create raw contact for RCS */
        int rawContactRefIms = ops.size();
        ops.add(ContentProviderOperation
                .newInsert(RawContacts.CONTENT_URI)
                .withValue(RawContacts.AGGREGATION_MODE, RawContacts.AGGREGATION_MODE_SUSPENDED)
                .withValue(RawContacts.ACCOUNT_TYPE, RcsAccountManager.ACCOUNT_MANAGER_TYPE)
                .withValue(RawContacts.ACCOUNT_NAME,
                        mContext.getString(R.string.rcs_core_account_username)).build());

        /* Insert number */
        ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, rawContactRefIms)
                .withValue(Data.MIMETYPE, MIMETYPE_NUMBER).withValue(Data.DATA1, contactNumber)
                .build());

        /* Insert capabilities if supported */
        Capabilities capabilities = info.getCapabilities();
        if (capabilities.isFileTransferSupported()) {
            ops.add(createMimeTypeForContact(rawContactRefIms, contact,
                    MIMETYPE_CAPABILITY_FILE_TRANSFER));
        }
        if (capabilities.isImageSharingSupported()) {
            ops.add(createMimeTypeForContact(rawContactRefIms, contact,
                    MIMETYPE_CAPABILITY_IMAGE_SHARE));
        }
        if (capabilities.isImSessionSupported()) {
            ops.add(createMimeTypeForContact(rawContactRefIms, contact,
                    MIMETYPE_CAPABILITY_IM_SESSION));
        }
        if (capabilities.isVideoSharingSupported()) {
            ops.add(createMimeTypeForContact(rawContactRefIms, contact,
                    MIMETYPE_CAPABILITY_VIDEO_SHARE));
        }
        if (capabilities.isIPVoiceCallSupported()) {
            ops.add(createMimeTypeForContact(rawContactRefIms, contact,
                    MIMETYPE_CAPABILITY_IP_VOICE_CALL));
        }
        if (capabilities.isIPVideoCallSupported()) {
            ops.add(createMimeTypeForContact(rawContactRefIms, contact,
                    MIMETYPE_CAPABILITY_IP_VIDEO_CALL));
        }
        if (capabilities.isGeolocationPushSupported()) {
            ops.add(createMimeTypeForContact(rawContactRefIms, contact,
                    MIMETYPE_CAPABILITY_GEOLOCATION_PUSH));
        }
        /* Extensions */
        Set<String> exts = info.getCapabilities().getSupportedExtensions();
        if (exts.size() > 0) {
            ops.add(ContentProviderOperation
                    .newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, rawContactRefIms)
                    .withValue(Data.MIMETYPE, MIMETYPE_CAPABILITY_EXTENSIONS)
                    .withValue(Data.DATA1, contactNumber)
                    .withValue(Data.DATA2, ServiceExtensionManager.getExtensions(exts))
                    .withValue(Data.DATA3,
                            getMimeTypeDescriptionDetails(MIMETYPE_CAPABILITY_EXTENSIONS)).build());
        }

        ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, rawContactRefIms)
                .withValue(Data.MIMETYPE, MIMETYPE_REGISTRATION_STATE)
                .withValue(Data.DATA1, contactNumber)
                .withValue(Data.DATA2, info.getRegistrationState().toInt()).build());

        ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, rawContactRefIms)
                .withValue(Data.MIMETYPE, MIMETYPE_BLOCKING_STATE)
                .withValue(Data.DATA1, contactNumber)
                .withValue(Data.DATA2, info.getBlockingState().toInt()).build());

        ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, rawContactRefIms)
                .withValue(Data.MIMETYPE, MIMETYPE_BLOCKING_TIMESTAMP)
                .withValue(Data.DATA1, contactNumber)
                .withValue(Data.DATA2, info.getBlockingTimestamp()).build());

        /* Add raw contact as membership to RCS group */
        long rcsGroupId = getRcsGroupIdFromContactsContractGroups();
        if (INVALID_ID != rcsGroupId) {
            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, rawContactRefIms)
                    .withValue(Data.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE)
                    .withValue(GroupMembership.GROUP_ROW_ID, rcsGroupId).build());
        }

        /* Create the RCS raw contact and get its ID */
        long rcsRawContactId = INVALID_ID;
        try {
            ContentProviderResult[] results;
            results = mContentResolver.applyBatch(ContactsContract.AUTHORITY, ops);
            rcsRawContactId = ContentUris.parseId(results[rawContactRefIms].uri);
        } catch (RemoteException e) {
        } catch (OperationApplicationException e) {
            if (sLogger.isActivated()) {
                sLogger.error("Operation exception", e);
            }
            return INVALID_ID;
        }
        /* Aggregate the newly RCS raw contact and the raw contact that has the phone number */
        ops.clear();
        ops.add(ContentProviderOperation
                .newUpdate(ContactsContract.AggregationExceptions.CONTENT_URI)
                .withValue(AggregationExceptions.TYPE, AggregationExceptions.TYPE_KEEP_TOGETHER)
                .withValue(AggregationExceptions.RAW_CONTACT_ID1, rcsRawContactId)
                .withValue(AggregationExceptions.RAW_CONTACT_ID2, rawContactId).build());
        try {
            mContentResolver.applyBatch(ContactsContract.AUTHORITY, ops);
            /* Add to aggregation data provider */
            ContentValues values = new ContentValues();
            values.put(AggregationData.KEY_RAW_CONTACT_ID, rawContactId);
            values.put(AggregationData.KEY_RCS_RAW_CONTACT_ID, rcsRawContactId);
            values.put(AggregationData.KEY_RCS_NUMBER, contactNumber);
            mLocalContentResolver.insert(AggregationData.CONTENT_URI, values);
        } catch (RemoteException e) {
            if (sLogger.isActivated()) {
                sLogger.error("Remote exception", e);
            }
            return INVALID_ID;

        } catch (OperationApplicationException e) {
            if (sLogger.isActivated()) {
                sLogger.error("Operation exception", e);
            }
            return INVALID_ID;
        }
        return rcsRawContactId;
    }

    /**
     * Converts the specified bitmap to a byte array.
     * 
     * @param bitmap the Bitmap to convert
     * @return the bitmap as bytes, null if converting fails.
     */
    private byte[] convertBitmapToBytes(final Bitmap bitmap) {
        int size = bitmap.getRowBytes() * bitmap.getHeight();
        ByteArrayOutputStream out = new ByteArrayOutputStream(size);
        try {
            if (bitmap.compress(Bitmap.CompressFormat.PNG, 0 /* quality ignored for PNG */, out)) {
                return out.toByteArray();
            }
            if (sLogger.isActivated()) {
                sLogger.debug("Unable to convert bitmap, compression failed");
            }
            return null;

        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                // purposely left blank
            }
        }
    }

    /**
     * Utility method to create the "Me" raw contact.
     * 
     * @return the rawContactId of the newly created contact or INVALID_ID is social presence is not
     *         supported or creation failed
     */
    public long createMyContact() {
        if (!mRcsSettings.isSocialPresenceSupported()) {
            return INVALID_ID;
        }
        /* Check if IMS account exists before continue */
        AccountManager am = AccountManager.get(mContext);
        if (am.getAccountsByType(RcsAccountManager.ACCOUNT_MANAGER_TYPE).length == 0) {
            if (sLogger.isActivated()) {
                sLogger.error("Could not create \"Me\" contact, no RCS account found");
            }
            throw new IllegalStateException("No RCS account found");
        }
        /* Check if RCS raw contact for "Me" does not already exist */
        long imsRawContactId = getRawContactIdForMe();

        if (INVALID_ID != imsRawContactId) {
            if (sLogger.isActivated()) {
                sLogger.error("\"Me\" contact already exists, no need to recreate");
            }
            return imsRawContactId;
        }
        if (sLogger.isActivated()) {
            sLogger.error("\"Me\" contact does not already exists, creating it");
        }
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        /* Create raw contact for RCS */
        int rawContactRefIms = ops.size();
        ops.add(ContentProviderOperation
                .newInsert(RawContacts.CONTENT_URI)
                .withValue(RawContacts.ACCOUNT_TYPE, RcsAccountManager.ACCOUNT_MANAGER_TYPE)
                .withValue(RawContacts.ACCOUNT_NAME,
                        mContext.getString(R.string.rcs_core_account_username))
                .withValue(RawContacts.SOURCE_ID, MYSELF)
                .withValue(RawContacts.AGGREGATION_MODE, RawContacts.AGGREGATION_MODE_DISABLED)
                .build());
        /* Set name */
        ops.add(ContentProviderOperation
                .newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, rawContactRefIms)
                .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                .withValue(StructuredName.DISPLAY_NAME,
                        mContext.getString(R.string.rcs_core_my_profile)).build());
        /* Add raw contact as membership to RCS group */
        long rcsGroupId = getRcsGroupIdFromContactsContractGroups();
        if (INVALID_ID != rcsGroupId) {
            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, rawContactRefIms)
                    .withValue(Data.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE)
                    .withValue(GroupMembership.GROUP_ROW_ID, rcsGroupId).build());
        }
        try {
            ContentProviderResult[] results;
            results = mContentResolver.applyBatch(ContactsContract.AUTHORITY, ops);
            imsRawContactId = ContentUris.parseId(results[rawContactRefIms].uri);
        } catch (RemoteException e) {
            imsRawContactId = INVALID_ID;
        } catch (OperationApplicationException e) {
            imsRawContactId = INVALID_ID;
        }
        ops.clear();
        /* Set default free text to null and availability to online */
        List<ContentProviderOperation> registrationStateOps = modifyContactRegistrationStateForMyself(
                imsRawContactId, RegistrationState.ONLINE, RegistrationState.UNKNOWN, "", "");
        for (ContentProviderOperation registrationStateOp : registrationStateOps) {
            if (registrationStateOp != null) {
                ops.add(registrationStateOp);
            }
        }
        try {
            mContentResolver.applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (RemoteException e) {
            imsRawContactId = INVALID_ID;
        } catch (OperationApplicationException e) {
            imsRawContactId = INVALID_ID;
        }
        return imsRawContactId;
    }

    /**
     * Utility to find the rawContactIds for a specific phone number.
     * 
     * @param contact the contact ID to search for
     * @return set of contact Ids
     */
    private Set<Long> getRawContactIdsFromPhoneNumber(ContactId contact) {
        Set<Long> rawContactsIds = new HashSet<Long>();
        String[] selectionArgs = {
                Phone.CONTENT_ITEM_TYPE, contact.toString()
        };
        /* Starting LOOSE equal */
        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(Data.CONTENT_URI, PROJ_DATA_RAW_CONTACT,
                    SEL_RAW_CONTACT_FROM_NUMBER, selectionArgs, Data.RAW_CONTACT_ID);
            CursorUtil.assertCursorIsNotNull(cursor, Data.CONTENT_URI);
            if (cursor.moveToFirst()) {
                int contactColumnIdx = cursor.getColumnIndexOrThrow(Data.RAW_CONTACT_ID);
                do {
                    long rawContactId = cursor.getLong(contactColumnIdx);
                    /*
                     * We exclude the SIM only contacts, as they cannot be aggregated to a RCS raw
                     * contact (only if OS version if gingerbread or less).
                     */
                    if (!isSimAccount(rawContactId)) {
                        rawContactsIds.add(rawContactId);
                    }
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        /*
         * No match found using LOOSE equals, starting STRICT equals. This is done because of that
         * the PHONE_NUMBERS_EQUAL function in Android doesn't always return true when doing loose
         * lookup of a phone number against itself
         */
        String[] selectionArgsStrict = {
                Phone.CONTENT_ITEM_TYPE, contact.toString(), contact.toString()
        };
        cursor = null;
        try {
            cursor = mContentResolver.query(Data.CONTENT_URI, PROJ_DATA_RAW_CONTACT,
                    STRICT_SELECTION_RAW_CONTACT_FROM_NUMBER, selectionArgsStrict,
                    Data.RAW_CONTACT_ID);
            CursorUtil.assertCursorIsNotNull(cursor, Data.CONTENT_URI);
            if (!cursor.moveToFirst()) {
                return rawContactsIds;
            }
            int contactColumnIdx = cursor.getColumnIndexOrThrow(Data.RAW_CONTACT_ID);
            do {
                long rawContactId = cursor.getLong(contactColumnIdx);
                /*
                 * We exclude the SIM only contacts, as they cannot be aggregated to a RCS raw
                 * contact (only if OS version if gingerbread or less).
                 */
                if (!isSimAccount(rawContactId)) {
                    rawContactsIds.add(cursor.getLong(contactColumnIdx));
                }
            } while (cursor.moveToNext());
            return rawContactsIds;

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Utility to get the RCS rawContact associated to a raw contact
     * 
     * @param rawContactId the id of the rawContact
     * @param contact The contact ID
     * @return the id of the associated RCS rawContact
     */
    private long getAssociatedRcsRawContact(final long rawContactId, final ContactId contact) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(AggregationData.CONTENT_URI,
                    PROJ_RCS_RAW_CONTACT_ID, WHERE_RCS_RAW_CONTACT_ID_AND_RCS_NUMBER, new String[] {
                            contact.toString(), String.valueOf(rawContactId)
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, AggregationData.CONTENT_URI);
            if (cursor.moveToFirst()) {
                return cursor.getLong(cursor
                        .getColumnIndexOrThrow(AggregationData.KEY_RCS_RAW_CONTACT_ID));

            }
            return INVALID_ID;

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Utility to check if a phone number is associated to an entry in the RCS contact provider
     * 
     * @param contact The contact ID
     * @return true if contact has an entry in the RCS contact provider, else false
     */
    public boolean isContactIdAssociatedWithRcsContactProvider(final ContactId contact) {
        Cursor cursor = null;
        Uri uri = Uri.withAppendedPath(CONTENT_URI, contact.toString());
        try {
            cursor = mLocalContentResolver.query(uri, PROJ_RCSCONTACT_CONTACT, null, null, null);
            CursorUtil.assertCursorIsNotNull(cursor, uri);
            return cursor.moveToFirst();

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Utility method to check if a raw contact id is a SIM account
     * 
     * @param rawContactId raw contact ID
     * @return True if raw contact id is a SIM account
     */
    public boolean isSimAccount(final long rawContactId) {
        Cursor cursor = null;
        String[] selectionArgs = new String[] {
            String.valueOf(rawContactId)
        };
        try {
            cursor = mContentResolver.query(RawContacts.CONTENT_URI, PROJ_RAW_CONTACT_ID,
                    SIM_ACCOUNT_SELECTION, selectionArgs, null);
            CursorUtil.assertCursorIsNotNull(cursor, RawContacts.CONTENT_URI);
            return cursor.moveToFirst();

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Utility to set the photo icon attribute on a RCS contact.
     * 
     * @param rawContactId RCS raw contact
     * @param photoIcon The photoIcon
     * @param makeSuperPrimary whether or not to set the super primary flag
     * @return
     */
    private List<ContentProviderOperation> setContactPhoto(Long rawContactId, PhotoIcon photoIcon,
            boolean makeSuperPrimary) {
        List<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        /* Get the photo data id */
        String[] selectionArgs = {
                Long.toString(rawContactId), Photo.CONTENT_ITEM_TYPE
        };
        String sortOrder = new StringBuilder(Data._ID).append(" DESC").toString();

        Cursor cursor = mContentResolver.query(Data.CONTENT_URI, PROJ_DATA_ID,
                SEL_RAW_CONTACT_WITH_MIMETYPE, selectionArgs, sortOrder);
        if (cursor == null) {
            return ops;
        }
        byte[] iconData = null;
        if (photoIcon != null) {
            iconData = photoIcon.getContent();
        }
        /* Insert default avatar if icon is null and it is not for myself */
        if (iconData == null && rawContactId != getRawContactIdForMe()) {
            Bitmap rcsAvatar = BitmapFactory.decodeResource(mContext.getResources(),
                    R.drawable.rcs_core_default_portrait_icon);
            iconData = convertBitmapToBytes(rcsAvatar);
        }
        long dataId = INVALID_ID;
        try {
            if (iconData == null) {
                /*
                 * May happen only for myself. Remove photoIcon if no data.
                 */
                if (cursor.moveToNext()) {
                    dataId = cursor.getLong(cursor.getColumnIndex(Data._ID));
                    /* Add delete operation */
                    ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI)
                            .withSelection(SEL_DATA_ID, new String[] {
                                String.valueOf(dataId)
                            }).build());
                }
            } else {
                ContentValues values = new ContentValues();
                values.put(Data.RAW_CONTACT_ID, rawContactId);
                values.put(Photo.PHOTO, iconData);
                values.put(Data.IS_PRIMARY, 1);
                if (makeSuperPrimary) {
                    values.put(Data.IS_SUPER_PRIMARY, 1);
                }
                if (cursor.moveToNext()) {
                    /* We already have an icon, update it */
                    dataId = cursor.getLong(cursor.getColumnIndex(Data._ID));
                    ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                            .withSelection(SEL_DATA_ID, new String[] {
                                String.valueOf(dataId)
                            }).withValues(values).build());
                } else {
                    /* We did not have an icon, insert a new one */
                    ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValues(values)
                            .build());
                }
                values.clear();
                /* Set etag */
                values.put(Data.RAW_CONTACT_ID, rawContactId);
                String etag = null;
                if (photoIcon != null) {
                    etag = photoIcon.getEtag();
                }
                values.put(Data.DATA2, etag);

                String[] projection2 = {
                        Data._ID, Data.RAW_CONTACT_ID, Data.MIMETYPE
                };
                String[] selectionArgs2 = {
                    Long.toString(rawContactId)
                };
                Cursor cur2 = null;
                try {
                    cur2 = mContentResolver.query(Data.CONTENT_URI, projection2,
                            SEL_RAW_CONTACT_ID, selectionArgs2, null);
                    CursorUtil.assertCursorIsNotNull(cursor, Data.CONTENT_URI);
                    if (cur2.moveToNext()) {
                        dataId = cur2.getLong(cur2.getColumnIndexOrThrow(Data._ID));
                        /* We already have an etag, update it */
                        dataId = cursor.getLong(cursor.getColumnIndexOrThrow(Data._ID));
                        ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                                .withSelection(SEL_DATA_ID, new String[] {
                                    String.valueOf(dataId)
                                }).withValues(values).build());
                    } else {
                        /* Insert etag */
                        ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                                .withValues(values).build());
                    }
                } catch (Exception e) {
                    if (sLogger.isActivated()) {
                        sLogger.error("Cannot add/update etag", e);
                    }
                } finally {
                    if (cur2 != null) {
                        cur2.close();
                    }
                }
            }
            return ops;

        } finally {
            cursor.close();
        }
    }

    /**
     * Get the raw contact id of the "Me" contact.
     * 
     * @return rawContactId
     */
    private long getRawContactIdForMe() {
        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(RawContacts.CONTENT_URI, PROJ_RAW_CONTACT_ID,
                    SEL_RAW_CONTACT_ME, null, null);
            CursorUtil.assertCursorIsNotNull(cursor, RawContacts.CONTENT_URI);
            if (!cursor.moveToNext()) {
                return INVALID_ID;
            }
            return cursor.getLong(cursor.getColumnIndexOrThrow(RawContacts._ID));

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Get whether the contact is blocked or not
     * 
     * @param contact The contact ID
     * @return flag indicating if IM sessions with the contact are enabled or not
     */
    public boolean isBlockedForContact(ContactId contact) {
        return BlockingState.BLOCKED == getContactInfo(contact).getBlockingState();
    }

    /**
     * Utility to create a ContactInfo object from a cursor containing data
     * 
     * @param cursor
     * @return contactInfo
     */
    private ContactInfo getContactInfoFromCursor(Cursor cursor) {
        ContactInfo contactInfo = new ContactInfo();
        PresenceInfo presenceInfo = new PresenceInfo();
        Capabilities.CapabilitiesBuilder capaBuilder = new Capabilities.CapabilitiesBuilder();
        try {
            int idMimeTypeColumnIdx = cursor.getColumnIndexOrThrow(Data.MIMETYPE);
            while (cursor.moveToNext()) {
                String mimeTypeStr = cursor.getString(idMimeTypeColumnIdx);
                try {
                    /* Convert mime type string to enumerated */
                    MimeType mimeType = MimeType.getMimeType(mimeTypeStr);
                    switch (mimeType) {
                        case CAPABILITY_IMAGE_SHARE:
                            capaBuilder.setImageSharing(true);
                            break;
                        case CAPABILITY_VIDEO_SHARE:
                            capaBuilder.setVideoSharing(true);
                            break;
                        case CAPABILITY_IP_VOICE_CALL:
                            capaBuilder.setIpVoiceCall(true);
                            break;
                        case CAPABILITY_IP_VIDEO_CALL:
                            capaBuilder.setIpVideoCall(true);
                            break;
                        case CAPABILITY_IM_SESSION:
                            capaBuilder.setImSession(true);
                            break;
                        case CAPABILITY_FILE_TRANSFER:
                            capaBuilder.setFileTransfer(true);
                            break;
                        case CAPABILITY_GEOLOCATION_PUSH:
                            capaBuilder.setGeolocationPush(true);
                            break;
                        case CAPABILITY_EXTENSIONS: {
                            /* Set RCS extensions capability */
                            int columnIndex = cursor.getColumnIndex(Data.DATA2);
                            if (INVALID_ID != columnIndex) {
                                capaBuilder.setExtensions(ServiceExtensionManager
                                        .getExtensions(cursor.getString(columnIndex)));
                            }
                        }
                            break;
                        case REGISTRATION_STATE: {
                            /* Set registration state */
                            int columnIndex = cursor.getColumnIndex(Data.DATA2);
                            if (columnIndex != -1) {
                                int registrationState = cursor.getInt(columnIndex);
                                contactInfo.setRegistrationState(RegistrationState
                                        .valueOf(registrationState));
                            }
                        }
                            break;
                        case NUMBER: {
                            /* Set contact ID */
                            int columnIndex = cursor.getColumnIndex(Data.DATA1);
                            if (INVALID_ID != columnIndex) {
                                String contact = cursor.getString(columnIndex);
                                /* check validity for contact read from raw contact */
                                PhoneNumber number = ContactUtil
                                        .getValidPhoneNumberFromAndroid(contact);
                                if (number != null) {
                                    contactInfo.setContact(ContactUtil
                                            .createContactIdFromValidatedData(number));
                                } else {
                                    if (sLogger.isActivated()) {
                                        sLogger.warn("Cannot parse contact ".concat(contact));
                                    }
                                }
                            }
                        }
                            break;
                        case BLOCKING_STATE: {
                            /* Set blocking state */
                            int columnIndex = cursor.getColumnIndex(Data.DATA2);
                            if (columnIndex != -1) {
                                int state = cursor.getInt(columnIndex);
                                contactInfo.setBlockingState(BlockingState.valueOf(state));
                            }
                        }
                            break;
                        case BLOCKING_TIMESTAMP: {
                            /* Set blocking timestamp */
                            int columnIndex = cursor.getColumnIndex(Data.DATA2);
                            if (columnIndex != -1) {
                                long timestamp = cursor.getLong(columnIndex);
                                contactInfo.setBlockingTimestamp(timestamp);
                            }
                        }
                            break;
                        default:
                            if (sLogger.isActivated()) {
                                sLogger.warn("Unhandled mimetype ".concat(mimeTypeStr));
                            }
                            break;
                    }
                } catch (Exception e) {
                    if (sLogger.isActivated()) {
                        sLogger.warn("Invalid mimetype ".concat(e.getMessage()));
                    }
                }
            }
            contactInfo.setPresenceInfo(presenceInfo);
            contactInfo.setCapabilities(capaBuilder.build());
            return contactInfo;

        } finally {
            cursor.close();
        }
    }

    /**
     * Utility to extract data from a raw contact.
     * 
     * @param rawContactId the rawContactId
     * @return A cursor containing the requested data.
     */
    private Cursor getRawContactDataCursor(final long rawContactId) {
        String[] selectionArgs = {
            Long.toString(rawContactId)
        };
        Cursor cur = mContentResolver.query(Data.CONTENT_URI, PROJ_RAW_CONTACT_DATA_ALL,
                SEL_RAW_CONTACT_MIME_TYPES, selectionArgs, null);
        return cur;
    }

    /**
     * Update UI strings when device's locale has changed
     */
    public void updateStrings() {
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        /* Update My profile display name */
        ContentValues values = new ContentValues();
        values.put(StructuredName.DISPLAY_NAME, mContext.getString(R.string.rcs_core_my_profile));
        ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                .withSelection(SEL_RAW_CONTACT_WITH_MIMETYPE, new String[] {
                        Long.toString(getRawContactIdForMe()), StructuredName.DISPLAY_NAME
                }).withValues(values).build());

        /* Update file transfer menu */
        values.clear();
        values.put(Data.DATA2, getMimeTypeDescriptionSummary(MIMETYPE_CAPABILITY_FILE_TRANSFER));
        values.put(Data.DATA3, getMimeTypeDescriptionDetails(MIMETYPE_CAPABILITY_FILE_TRANSFER));
        ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                .withSelection(SEL_DATA_MIMETYPE_CAPABILITY_FILE_TRANSFER, null).withValues(values)
                .build());

        /* Update chat menu */
        values.clear();
        values.put(Data.DATA2, getMimeTypeDescriptionSummary(MIMETYPE_CAPABILITY_IM_SESSION));
        values.put(Data.DATA3, getMimeTypeDescriptionDetails(MIMETYPE_CAPABILITY_IM_SESSION));
        ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                .withSelection(SEL_DATA_MIMETYPE_CAPABILITY_IM_SESSION, null).withValues(values)
                .build());

        /* Update image sharing menu */
        values.clear();
        values.put(Data.DATA2, getMimeTypeDescriptionSummary(MIMETYPE_CAPABILITY_IMAGE_SHARE));
        values.put(Data.DATA3, getMimeTypeDescriptionDetails(MIMETYPE_CAPABILITY_IMAGE_SHARE));
        ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                .withSelection(SEL_DATA_MIMETYPE_CAPABILITY_IMAGE_SHARING, null).withValues(values)
                .build());

        /* Update video sharing menu */
        values.clear();
        values.put(Data.DATA2, getMimeTypeDescriptionSummary(MIMETYPE_CAPABILITY_VIDEO_SHARE));
        values.put(Data.DATA3, getMimeTypeDescriptionDetails(MIMETYPE_CAPABILITY_VIDEO_SHARE));
        ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                .withSelection(SEL_DATA_MIMETYPE_CAPABILITY_VIDEO_SHARING, null).withValues(values)
                .build());

        /* Update IP voice call menu */
        values.clear();
        values.put(Data.DATA2, getMimeTypeDescriptionSummary(MIMETYPE_CAPABILITY_IP_VOICE_CALL));
        values.put(Data.DATA3, getMimeTypeDescriptionDetails(MIMETYPE_CAPABILITY_IP_VOICE_CALL));
        ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                .withSelection(SEL_DATA_MIMETYPE_CAPABILITY_IP_VOICE_CALL, null).withValues(values)
                .build());

        /* Update IP video call menu */
        values.clear();
        values.put(Data.DATA2, getMimeTypeDescriptionSummary(MIMETYPE_CAPABILITY_IP_VIDEO_CALL));
        values.put(Data.DATA3, getMimeTypeDescriptionDetails(MIMETYPE_CAPABILITY_IP_VIDEO_CALL));
        ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                .withSelection(SEL_DATA_MIMETYPE_CAPABILITY_IP_VIDEO_CALL, null).withValues(values)
                .build());

        /*
         * Update extensions menu: do not update summary which contains the list of supported
         * extensions.
         */
        values.clear();
        values.put(Data.DATA3, getMimeTypeDescriptionDetails(MIMETYPE_CAPABILITY_EXTENSIONS));
        ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                .withSelection(SEL_DATA_MIMETYPE_CAPABILITY_EXTENSIONS, null).withValues(values)
                .build());

        if (!ops.isEmpty()) {
            /* Do the actual database modifications */
            try {
                mContentResolver.applyBatch(ContactsContract.AUTHORITY, ops);
            } catch (RemoteException e) {
                if (sLogger.isActivated()) {
                    sLogger.error("Something went wrong when updating the database strings", e);
                }
            } catch (OperationApplicationException e) {
                if (sLogger.isActivated()) {
                    sLogger.error("Something went wrong when updating the database strings", e);
                }
            }
        }
    }

    /**
     * Clean the RCS entries <br>
     * This removes the RCS entries that are associated to numbers not present in the address book
     * anymore <br>
     * This also creates a RCS raw contact for numbers that are present, have RCS raw contact but
     * not on all raw contacts (typical example: a RCS number is present in the address book and
     * another contact is created using the same number)
     */
    public void cleanRCSEntries() {
        cleanRCSRawContactsInAB();
        cleanEntriesInRcsContactProvider();
    }

    /**
     * Clean Address Book
     */
    private void cleanRCSRawContactsInAB() {
        /*
         * Get all RCS raw contacts id Delete RCS Entry where number is not in the address book
         * anymore.
         */
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(Data.CONTENT_URI, PROJ_RAW_CONTACT_DATA1,
                    SEL_DATA_MIMETYPE_NUMBER, null, null);
            CursorUtil.assertCursorIsNotNull(cursor, Data.CONTENT_URI);
            if (!cursor.moveToFirst()) {
                return;
            }
            int contactColumnIdx = cursor.getColumnIndexOrThrow(Data.RAW_CONTACT_ID);
            int data1ColumnIdx = cursor.getColumnIndexOrThrow(Data.DATA1);
            do {
                String phoneNumber = cursor.getString(data1ColumnIdx);
                PhoneNumber number = ContactUtil.getValidPhoneNumberFromAndroid(phoneNumber);
                if (number == null) {
                    if (sLogger.isActivated()) {
                        sLogger.warn("Cannot parse contact " + phoneNumber);
                    }
                    continue;
                }
                ContactId contact = ContactUtil.createContactIdFromValidatedData(number);
                long rawContactId = cursor.getLong(contactColumnIdx);
                if (getRawContactIdsFromPhoneNumber(contact).isEmpty()) {
                    ops.add(ContentProviderOperation.newDelete(RawContacts.CONTENT_URI)
                            .withSelection(SEL_RAW_CONTACT, new String[] {
                                Long.toString(rawContactId)
                            }).build());
                    /* Also delete the corresponding entries in the aggregation provider */
                    mLocalContentResolver.delete(AggregationData.CONTENT_URI,
                            WHERE_RCS_RAW_CONTACT_ID, new String[] {
                                Long.toString(rawContactId)
                            });
                }
            } while (cursor.moveToNext());
        } catch (Exception e) {
            if (sLogger.isActivated()) {
                sLogger.error("Exception occurred", e);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        if (!ops.isEmpty()) {
            /* Do the actual database modifications */
            try {
                mContentResolver.applyBatch(ContactsContract.AUTHORITY, ops);
            } catch (RemoteException e) {
                if (sLogger.isActivated()) {
                    sLogger.error("Something went wrong when updating the database strings", e);
                }
            } catch (OperationApplicationException e) {
                if (sLogger.isActivated()) {
                    sLogger.error("Something went wrong when updating the database strings", e);
                }
            }
        }
    }

    /**
     * Clean all in RCS contact provider
     */
    private void cleanEntriesInRcsContactProvider() {
        /* Get All contacts in RCS contact provider */
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(CONTENT_URI, PROJ_RCSCONTACT_CONTACT, null, null,
                    null);
            CursorUtil.assertCursorIsNotNull(cursor, CONTENT_URI);
            if (!cursor.moveToFirst()) {
                return;
            }
            int contactColumnIdx = cursor.getColumnIndexOrThrow(KEY_CONTACT);
            /* Delete RCS contact Entry where number is not in the address book anymore */
            do {
                String phoneNumber = cursor.getString(contactColumnIdx);
                /* Do not check validity for trusted data */
                PhoneNumber number = ContactUtil.getValidPhoneNumberFromAndroid(phoneNumber);
                ContactId contact = ContactUtil.createContactIdFromValidatedData(number);
                if (getRawContactIdsFromPhoneNumber(contact).isEmpty()) {
                    Uri uri = Uri.withAppendedPath(ContactData.CONTENT_URI, phoneNumber);
                    mLocalContentResolver.delete(uri, null, null);
                    if (sLogger.isActivated()) {
                        sLogger.debug("cleanEntriesInRcsContactProvider clear for contact="
                                .concat(phoneNumber));
                    }
                    mContactInfoCache.remove(contact);
                }
            } while (cursor.moveToNext());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Delete all RCS entries in databases
     */
    public void deleteRCSEntries() {
        /* Delete Aggregation data */
        mLocalContentResolver.delete(AggregationData.CONTENT_URI, null, null);
        /* Empty the cache */
        if (sLogger.isActivated()) {
            sLogger.debug("deleteRCSEntries");
        }
        mContactInfoCache.clear();
        /* Delete presence data */
        mLocalContentResolver.delete(CONTENT_URI, null, null);
    }

    /**
     * Is capability supported
     * 
     * @param cursor Cursor
     * @param column Column name
     * @return True if capability is supported
     */
    private boolean isCapabilitySupported(Cursor cursor, String column) {
        return cursor.getInt(cursor.getColumnIndexOrThrow(column)) == CapabilitiesLog.SUPPORTED;
    }

    /**
     * Sets the display name into the RCS contact provider
     * 
     * @param contact Contact ID
     * @param displayName the display name
     */
    public void setContactDisplayName(ContactId contact, String displayName) {
        ContentValues values = new ContentValues();
        values.put(KEY_DISPLAY_NAME, displayName);
        /* Check if record exists and if so then update is required */
        String oldDisplayName = getContactDisplayName(contact);
        boolean updateRequired = !StringUtils.equals(oldDisplayName, displayName);
        if (updateRequired) {
            if (sLogger.isActivated()) {
                sLogger.debug("Update display name '" + displayName + "' for contact:" + contact);
            }
            Uri uri = Uri.withAppendedPath(ContactData.CONTENT_URI, contact.toString());
            /* Contact already present and display name is new, update */
            mLocalContentResolver.update(uri, values, null, null);

            getContactInfo(contact).setDisplayName(displayName);
        }
    }

    /**
     * Get RCS display name for contact
     * 
     * @param contact ID
     * @return the display name or null
     */
    public String getContactDisplayName(ContactId contact) {
        return getContactInfo(contact).getDisplayName();
    }

    /**
     * Update the time of last capabilities refresh
     * 
     * @param contact ID
     */
    public void updateCapabilitiesTimeLastResponse(ContactId contact) {
        String contactNumber = contact.toString();
        if (sLogger.isActivated()) {
            sLogger.debug("Update the time of last capabilities response for "
                    .concat(contactNumber));
        }
        ContactInfo contactInfo = getContactInfo(contact);
        Capabilities capabilities = contactInfo.getCapabilities();
        if (capabilities == null) {
            return;
        }
        long now = System.currentTimeMillis();
        /* Update the cache */
        CapabilitiesBuilder capaBuilder = new CapabilitiesBuilder(capabilities);
        capaBuilder.setTimestampOfLastResponse(now);
        contactInfo.setCapabilities(capaBuilder.build());

        ContentValues values = new ContentValues();
        values.put(KEY_CAPABILITY_TIMESTAMP_LAST_RESPONSE, now);
        Uri uri = Uri.withAppendedPath(CONTENT_URI, contactNumber);
        mLocalContentResolver.update(uri, values, null, null);
    }

    /**
     * Set the blocking state for a given contact
     * 
     * @param contact Contact ID
     * @param state Blocking state
     * @throws ContactManagerException exception thrown if update operation has failed
     */
    public void setBlockingState(ContactId contact, BlockingState state)
            throws ContactManagerException {
        /* Get the current information on this contact */
        ContactInfo oldInfo = getContactInfo(contact);
        ContactInfo newInfo = new ContactInfo(oldInfo);
        /* Update the state */
        newInfo.setBlockingState(state);
        newInfo.setBlockingTimestamp(System.currentTimeMillis());
        /* Save the modifications */
        setContactInfo(newInfo, oldInfo);
    }

    /**
     * Gets the raw IDs for all contacts of the native address book
     * 
     * @return map of contact IDs associated with their raw contact IDs
     */
    public Map<ContactId, Set<Long>> getAllRawIdsInPhoneAddressBook() {
        Map<ContactId, Set<Long>> result = new HashMap<ContactId, Set<Long>>();
        Cursor cursor = null;
        try {
            /* Query all phone numbers of the native address book */
            cursor = mContentResolver.query(Phone.CONTENT_URI, PHONE_PROJ_NUMBER_RAW_CONTACT_ID,
                    null, null, null);
            CursorUtil.assertCursorIsNotNull(cursor, Phone.CONTENT_URI);
            if (!cursor.moveToFirst()) {
                return result;
            }
            int columnIndexPhoneNumber = cursor.getColumnIndexOrThrow(Phone.NUMBER);
            int columnIndexRawContactId = cursor.getColumnIndexOrThrow(Phone.RAW_CONTACT_ID);
            do {
                String phoneNumber = cursor.getString(columnIndexPhoneNumber);
                PhoneNumber validatedNumber = ContactUtil
                        .getValidPhoneNumberFromAndroid(phoneNumber);
                if (validatedNumber == null) {
                    if (sLogger.isActivated()) {
                        sLogger.warn("Cannot parse phone number '" + phoneNumber + "'");
                    }
                    continue;
                }
                Long rawContactId = cursor.getLong(columnIndexRawContactId);
                if (ContactManager.INVALID_ID == rawContactId) {
                    if (sLogger.isActivated()) {
                        sLogger.warn("Phone number '" + phoneNumber + "' has no raw contact ID");
                    }
                    continue;
                }
                ContactId contact = ContactUtil.createContactIdFromValidatedData(validatedNumber);
                if (result.containsKey(contact)) {
                    Set<Long> rawIds = result.get(contact);
                    rawIds.add(rawContactId);
                    result.put(contact, rawIds);
                } else {
                    Set<Long> rawIds = new HashSet<Long>();
                    rawIds.add(rawContactId);
                    result.put(contact, rawIds);
                }
            } while (cursor.moveToNext());
            return result;

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Checks if a RCS raw contact exists for the raw contact of the native address book
     * 
     * @param rawContactId the raw contact ID of the native address book
     * @param contact contactId
     * @return true if a RCS raw contact exists for the raw contact of the native address book
     */
    public boolean isAssociatedRcsRawContact(long rawContactId, final ContactId contact) {
        long rcsRawContactId = getAssociatedRcsRawContact(rawContactId, contact);
        return (ContactManager.INVALID_ID != rcsRawContactId);
    }

    /**
     * Aggregate contact with RCS raw contact
     * 
     * @param contactInfo contact to aggregate with RCS raw contact. The contact must be RCS.
     * @throws ContactManagerException thrown is contact information setting fails
     */
    public void aggregateContactWithRcsRawContact(ContactInfo contactInfo)
            throws ContactManagerException {
        /*
         * if RCS contact does not exist, it will be created and RCS raw contact also. if RCS
         * contact already exists, it will enforce creation and association of RCS raw contact.
         */
        setContactInfo(contactInfo, contactInfo);
    }

    /**
     * Gets the RCS group ID or INVALID_ID if it does not exist
     * 
     * @return the RCS group ID or INVALID_ID if it does not exist
     */
    public long getRcsGroupIdFromContactsContractGroups() {
        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(Groups.CONTENT_URI, PROJ_CONTACT_GROUP_ID,
                    SEL_RCS_CONTACT_GROUP_ACCOUNT_TYPE, null, null);
            CursorUtil.assertCursorIsNotNull(cursor, Groups.CONTENT_URI);
            if (!cursor.moveToNext()) {
                return ContactManager.INVALID_ID;
            }
            return cursor.getLong(cursor.getColumnIndexOrThrow(Groups._ID));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Checks if a set of raw contact IDs is only associated to a SIM account
     * 
     * @param rawContactIds the set of raw contact Identifiers
     * @return true if the raw contact identifiers are only associated to a SIM account, else false
     */
    private boolean isOnlySimAssociated(Set<Long> rawContactIds) {
        for (Long rawContactId : rawContactIds) {
            if (isSimAccount(rawContactId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the set of contacts only SIM associated
     * 
     * @param contacts the map of contacts with their raw contact IDs
     * @return the set of contacts only SIM associated
     */
    public Set<ContactId> getContactsOnlySimAssociated(Map<ContactId, Set<Long>> contacts) {
        Set<ContactId> result = new HashSet<ContactId>();
        for (Entry<ContactId, Set<Long>> contactEntry : contacts.entrySet()) {
            Set<Long> nativeRawContactIds = contactEntry.getValue();
            if (isOnlySimAssociated(nativeRawContactIds)) {
                result.add(contactEntry.getKey());
            }
        }
        return result;
    }
}
