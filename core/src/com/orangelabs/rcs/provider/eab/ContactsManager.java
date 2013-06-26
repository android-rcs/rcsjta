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

package com.orangelabs.rcs.provider.eab;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.gsma.joyn.capability.CapabilitiesLog;
import org.gsma.joyn.contacts.ContactsProvider;

import android.accounts.AccountManager;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.AggregationExceptions;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.Website;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.StatusUpdates;

import com.orangelabs.rcs.R;
import com.orangelabs.rcs.addressbook.AuthenticationService;
import com.orangelabs.rcs.core.ims.service.ContactInfo;
import com.orangelabs.rcs.core.ims.service.capability.Capabilities;
import com.orangelabs.rcs.core.ims.service.presence.FavoriteLink;
import com.orangelabs.rcs.core.ims.service.presence.Geoloc;
import com.orangelabs.rcs.core.ims.service.presence.PhotoIcon;
import com.orangelabs.rcs.core.ims.service.presence.PresenceInfo;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Contains utility methods for interfacing with the Android SDK ContactsProvider.
 *
 * @author Jean-Marc AUFFRET
 * @author Deutsche Telekom AG
 */
public final class ContactsManager {
	/**
	 * Current instance
	 */
	private static ContactsManager instance = null;
	
	/**
	 * Context
	 */
	private Context ctx;
	
    /** 
     * Constant for invalid id. 
     */
	private static final int INVALID_ID = -1;

    /** 
     * MIME type for contact number
     */
    private static final String MIMETYPE_NUMBER = ContactsProvider.MIME_TYPE_PHONE_NUMBER;

    /** 
     * MIME type for RCS status 
     */
    private static final String MIMETYPE_RCS_STATUS = "vnd.android.cursor.item/com.orangelabs.rcs.rcs-status";

    /** 
     * MIME type for RCS registration state 
     */
    private static final String MIMETYPE_REGISTRATION_STATE = "vnd.android.cursor.item/com.orangelabs.rcs.registration-state";
    
    /** 
     * MIME type for RCS status timestamp
     */
    private static final String MIMETYPE_RCS_STATUS_TIMESTAMP = "vnd.android.cursor.item/com.orangelabs.rcs.rcs-status.timestamp";
    
    /**
     * MIME type for presence status
     */
    private static final String MIMETYPE_PRESENCE_STATUS = "vnd.android.cursor.item/com.orangelabs.rcs.presence-status";

    /**
     * MIME type for free text
     */
    private static final String MIMETYPE_FREE_TEXT = "vnd.android.cursor.item/com.orangelabs.rcs.free-text";
    
    /** 
     * MIME type for web link 
     */
    private static final String MIMETYPE_WEBLINK = "vnd.android.cursor.item/com.orangelabs.rcs.weblink";

    /** 
     * MIME type for photo icon 
     */
    private static final String MIMETYPE_PHOTO = ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE;

    /** 
     * MIME type for photo icon etag 
     */
    private static final String MIMETYPE_PHOTO_ETAG = "vnd.android.cursor.item/com.orangelabs.rcs.photo-etag";

    /** 
     * MIME type for presence timestamp 
     */
    private static final String MIMETYPE_PRESENCE_TIMESTAMP = "vnd.android.cursor.item/com.orangelabs.rcs.presence.timestamp";
    
    /** 
     * MIME type for capability timestamp 
     */
    private static final String MIMETYPE_CAPABILITY_TIMESTAMP = "vnd.android.cursor.item/com.orangelabs.rcs.capability.timestamp";
    
    /** 
     * MIME type for CS_VIDEO capability
     */
    private static final String MIMETYPE_CAPABILITY_CS_VIDEO = "vnd.android.cursor.item/com.orangelabs.rcs.capability.cs-video";

    /** 
     * MIME type for GSMA_CS_IMAGE (image sharing) capability 
     */
    private static final String MIMETYPE_CAPABILITY_IMAGE_SHARING = ContactsProvider.MIME_TYPE_IMAGE_SHARING;
    
    /** 
     * MIME type for 3GPP_CS_VOICE (video sharing) capability 
     */
    private static final String MIMETYPE_CAPABILITY_VIDEO_SHARING = ContactsProvider.MIME_TYPE_VIDEO_SHARING;

    /** 
     * MIME type for RCS_IM (IM session) capability 
     */
    private static final String MIMETYPE_CAPABILITY_IM_SESSION = ContactsProvider.MIME_TYPE_IM_SESSION;

    /** 
     * MIME type for RCS_FT (file transfer) capability 
     */
    private static final String MIMETYPE_CAPABILITY_FILE_TRANSFER = ContactsProvider.MIME_TYPE_FILE_TRANSFER;

    /** 
     * MIME type for presence discovery capability 
     */
    private static final String MIMETYPE_CAPABILITY_PRESENCE_DISCOVERY = "vnd.android.cursor.item/com.orangelabs.rcs.capability.presence-discovery";

    /** 
     * MIME type for social presence capability 
     */
    private static final String MIMETYPE_CAPABILITY_SOCIAL_PRESENCE = "vnd.android.cursor.item/com.orangelabs.rcs.capability.social-presence";

    /** 
     * MIME type for social presence capability 
     */
    private static final String MIMETYPE_CAPABILITY_GEOLOCATION_PUSH = "vnd.android.cursor.item/com.orangelabs.rcs.capability.geolocation-push";
    
    /** 
     * MIME type for file transfer thumbnail capability
     */
    private static final String MIMETYPE_CAPABILITY_FILE_TRANSFER_THUMBNAIL = "vnd.android.cursor.item/com.orangelabs.rcs.capability.file-transfer-thumbnail";
    
    /** 
     * MIME type for file transfer over HTTP capability 
     */
    private static final String MIMETYPE_CAPABILITY_FILE_TRANSFER_HTTP = "vnd.android.cursor.item/com.orangelabs.rcs.capability.file-transfer-http";
    
    /** 
     * MIME type for RCS extensions 
     */
    private static final String MIMETYPE_CAPABILITY_EXTENSIONS = ContactsProvider.MIME_TYPE_EXTENSIONS;

    /** 
     * MIME type when RCS extensions that I also support are present 
     */
    private static final String MIMETYPE_CAPABILITY_COMMON_EXTENSION = "vnd.android.cursor.item/com.orangelabs.rcs.capability.support.extension";
    
    /** 
     * MIME type for seeing my profile 
     */
    private static final String MIMETYPE_SEE_MY_PROFILE = "vnd.android.cursor.item/com.orangelabs.rcs.my-profile";
    
    /** 
     * MIME type for a RCS contact 
     */
    private static final String MIMETYPE_RCS_CONTACT = "vnd.android.cursor.item/com.orangelabs.rcs.rcs-contact";

    /** 
     * MIME type for a RCS capable contact 
     */
    private static final String MIMETYPE_RCS_CAPABLE_CONTACT = "vnd.android.cursor.item/com.orangelabs.rcs.rcs-capable-contact";
    
    /** 
     * MIME type for a non RCS contact 
     */
    private static final String MIMETYPE_NOT_RCS_CONTACT = "vnd.android.cursor.item/com.orangelabs.rcs.not-rcs-contact";

    /** 
     * MIME type for block IM status 
     */
    private static final String MIMETYPE_IM_BLOCKED = "vnd.android.cursor.item/com.orangelabs.rcs.im-blocked";

    /** 
     * MIME type for block FT status 
     */
    private static final String MIMETYPE_FT_BLOCKED = "vnd.android.cursor.item/com.orangelabs.rcs.ft-blocked";
    
    /**
     * ONLINE available status
     */
    private static final int PRESENCE_STATUS_ONLINE = 5; //StatusUpdates.AVAILABLE;

    /**
     * OFFLINE available status
     */
    private static final int PRESENCE_STATUS_OFFLINE = 0; //StatusUpdates.OFFLINE;
    
    /**
     * NOT SET available status
     */
    private static final int PRESENCE_STATUS_NOT_SET = 1; //StatusUpdates.INVISIBLE;

    /**
     * Account name for SIM contacts
     */
    private static final String SIM_ACCOUNT_NAME = "com.anddroid.contacts.sim";
    
    /**
     * Contact for "Me"
     */
    private static final String MYSELF = "myself";
    
	/**
	 * The logger
	 */
	private Logger logger = Logger.getLogger(getClass().getName());
    
	/**
	 * Create instance
	 * 
	 * @param ctx Context
	 */
	public static synchronized void createInstance(Context ctx) {
		if (instance == null) {
			instance = new ContactsManager(ctx);
		}
	}

	/**
	 * Returns instance
	 * 
	 * @return Instance
	 */
	public static ContactsManager getInstance() {
		return instance;
	}
	
    /**
     * Constructor
     *      
     * @param ctx Application context
     */
    private ContactsManager(Context ctx) {
    	this.ctx = ctx;
    }
    
    /**
	 * Set my presence info in the EAB
	 * 
	 * @param info Presence info
	 * @throws ContactsManagerException
	 */
	public void setMyInfo(PresenceInfo newPresenceInfo) throws ContactsManagerException {
		if (logger.isActivated()) {
			logger.info("Set my presence info");
		}
		if (!RcsSettings.getInstance().isSocialPresenceSupported()){
			return;
		}

		long myRawContactId = getRawContactIdForMe();
		
		PresenceInfo oldPresenceInfo = getMyPresenceInfo();
		
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		
		// Modify capability timestamp
		ContentProviderOperation op = modifyCapabilityTimestampForContact(myRawContactId, MYSELF, newPresenceInfo.getTimestamp());
		if (op!=null){
			ops.add(op);
		}
		
		// Set the new availability status
		int newAvailability = ContactInfo.REGISTRATION_STATUS_UNKNOWN;
		if (newPresenceInfo.isOnline()){
			newAvailability = ContactInfo.REGISTRATION_STATUS_ONLINE;	
		}else if (newPresenceInfo.isOffline()){
			newAvailability = ContactInfo.REGISTRATION_STATUS_OFFLINE;
		}
		
		// Set the old availability status
		int oldAvailability = ContactInfo.REGISTRATION_STATUS_UNKNOWN;
		if (oldPresenceInfo.isOnline()){
			oldAvailability = ContactInfo.REGISTRATION_STATUS_ONLINE;	
		}else if (oldPresenceInfo.isOffline()){
			oldAvailability = ContactInfo.REGISTRATION_STATUS_OFFLINE;
		}
		
		// Modify presence
		List<ContentProviderOperation> presenceOps = modifyPresenceForContact(myRawContactId, MYSELF, newPresenceInfo, oldPresenceInfo);
		for (int i=0;i<presenceOps.size();i++){
			op = presenceOps.get(i);
			if (op!=null){
				ops.add(op);
			}
		}
		
		// Modify contact registration state (for native address book)
		List<ContentProviderOperation> registrationOps = modifyContactRegistrationState(myRawContactId, MYSELF, newAvailability, oldAvailability, newPresenceInfo.getFreetext(), oldPresenceInfo.getFreetext());
		for (int i=0;i<registrationOps.size();i++){
			op = registrationOps.get(i);
			if (op!=null){
				ops.add(op);
			}
		}
	
		if (!ops.isEmpty()){
			// Do the actual database modifications
			try {
				ctx.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
			} catch (RemoteException e) {
				if (logger.isActivated()){
					logger.error("Something went wrong when updating the database with the contact info",e);
				}
				throw new ContactsManagerException(e.getMessage());
			} catch (OperationApplicationException e) {
				if (logger.isActivated()){
					logger.error("Something went wrong when updating the database with the contact info",e);
				}
				throw new ContactsManagerException(e.getMessage());
			}
		}		
	}
	
	/**
	 * Set my photo-icon in the EAB
	 * 
	 * @param photo Photo
	 * @throws ContactsManagerException
	 */
	public void setMyPhotoIcon(PhotoIcon photo)	throws ContactsManagerException {
		if (logger.isActivated()) {
			logger.info("Set my photo-icon");
		}
		
		if (!RcsSettings.getInstance().isSocialPresenceSupported()){
			return;
		}

		try {
			setContactPhotoIcon(MYSELF, photo);
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Internal exception", e);
			}
			throw new ContactsManagerException(e.getMessage());
		}
	}

	/**
	 * Remove my photo-icon in the EAB
	 * 
	 * @throws ContactsManagerException
	 */
	public void removeMyPhotoIcon() throws ContactsManagerException {
		if (logger.isActivated()) {
			logger.info("Remove my photo-icon");
		}
		
		if (!RcsSettings.getInstance().isSocialPresenceSupported()){
			return;
		}

		try {
			setContactPhotoIcon(MYSELF, null);
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Internal exception", e);
			}
			throw new ContactsManagerException(e.getMessage());
		}
	}

	/**
	 * Returns my presence info from the EAB
	 * 
	 * @return Presence info or null in case of error
	 */
	public PresenceInfo getMyPresenceInfo() {
		if (logger.isActivated()) {
			logger.info("Get my presence info");
		}
		if (!RcsSettings.getInstance().isSocialPresenceSupported()){
			return new PresenceInfo();
		}
		
		long rawContactId = getRawContactIdForMe();
		
		Cursor cursor = getRawContactDataCursor(rawContactId);
		
		return getContactInfoFromCursor(cursor).getPresenceInfo(); 
	}

    /**
     * Return the row id of a profile number in the EAB
     *
     * @param number Profile number
     * @return Row id
     */
    private int getProfileRowId(String number) {
        int rowId = -1;
        try {
            String where = RichAddressBookData.KEY_CONTACT_NUMBER + " = " + "\"" + number + "\"";
            Cursor cur = ctx.getContentResolver().query(RichAddressBookData.CONTENT_URI, null, where, null, null);
            if (cur.moveToFirst()) {
                rowId = cur.getInt(cur.getColumnIndex(RichAddressBookData.KEY_ID));
            }
            cur.close();
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Internal exception", e);
            }
        }
        return rowId;
    }

	/**
	 * Set the info of a contact
	 * 
	 * @param newInfo New contact info
	 * @param oldInfo Old contact info
	 * @throws ContactsManagerException
	 */
	public void setContactInfo(ContactInfo newInfo, ContactInfo oldInfo) throws ContactsManagerException {
		if (logger.isActivated()) {
			logger.info("Set contact info for " + newInfo.getContact());
		}

		// May be called from outside the core, so be sure the number format is international before doing the queries
		String contact = PhoneUtils.extractNumberFromUri(newInfo.getContact());

		// Check if we have an entry for the contact
		boolean hasEntryInRichAddressBook = false;
		Cursor cur = ctx.getContentResolver().query(RichAddressBookData.CONTENT_URI,
				new String[]{RichAddressBookData.KEY_CONTACT_NUMBER},
				RichAddressBookData.KEY_CONTACT_NUMBER +"=?",
				new String[]{contact},
				null);
		if (cur!=null && cur.moveToFirst()) {
			hasEntryInRichAddressBook = true;
		}
		cur.close();
		ContentValues values = new ContentValues();
		values.put(RichAddressBookData.KEY_CONTACT_NUMBER, contact);

        // Save RCS status
        values.put(RichAddressBookData.KEY_RCS_STATUS, newInfo.getRcsStatus());
        values.put(RichAddressBookData.KEY_RCS_STATUS_TIMESTAMP, newInfo.getRcsStatusTimestamp());

		// Save capabilities, if the contact is not registered, do not set the capability to true
		boolean isRegistered = (newInfo.getRegistrationState() == ContactInfo.REGISTRATION_STATUS_ONLINE);
		Capabilities newCapabilities = newInfo.getCapabilities();
		values.put(RichAddressBookData.KEY_CAPABILITY_CS_VIDEO, setCapabilityToColumn(newCapabilities.isCsVideoSupported() && isRegistered));
		values.put(RichAddressBookData.KEY_CAPABILITY_FILE_TRANSFER, setCapabilityToColumn(newCapabilities.isFileTransferSupported() && isRegistered));
		values.put(RichAddressBookData.KEY_CAPABILITY_IMAGE_SHARING, setCapabilityToColumn(newCapabilities.isImageSharingSupported() && isRegistered));
		values.put(RichAddressBookData.KEY_CAPABILITY_IM_SESSION, setCapabilityToColumn((newCapabilities.isImSessionSupported() && isRegistered)||(RcsSettings.getInstance().isImAlwaysOn() && newInfo.isRcsContact())));
		values.put(RichAddressBookData.KEY_CAPABILITY_PRESENCE_DISCOVERY, setCapabilityToColumn(newCapabilities.isPresenceDiscoverySupported() && isRegistered));
		values.put(RichAddressBookData.KEY_CAPABILITY_SOCIAL_PRESENCE, setCapabilityToColumn(newCapabilities.isSocialPresenceSupported() && isRegistered));
		values.put(RichAddressBookData.KEY_CAPABILITY_VIDEO_SHARING, setCapabilityToColumn(newCapabilities.isVideoSharingSupported() && isRegistered));
		values.put(RichAddressBookData.KEY_CAPABILITY_GEOLOCATION_PUSH, setCapabilityToColumn(newCapabilities.isGeolocationPushSupported() && isRegistered));
		values.put(RichAddressBookData.KEY_CAPABILITY_FILE_TRANSFER_HTTP, setCapabilityToColumn(newCapabilities.isFileTransferHttpSupported() && isRegistered));
		values.put(RichAddressBookData.KEY_CAPABILITY_FILE_TRANSFER_THUMBNAIL, setCapabilityToColumn(newCapabilities.isFileTransferThumbnailSupported() && isRegistered));

		// Save the capabilities extensions
		ArrayList<String> newExtensions = newCapabilities.getSupportedExtensions();
		StringBuffer aggregatedExtensions = new StringBuffer();
		for (int i=0; i<newExtensions.size(); i++){
			aggregatedExtensions.append(newExtensions.get(i)+";");
		}
		values.put(RichAddressBookData.KEY_CAPABILITY_EXTENSIONS, aggregatedExtensions.toString());

		// Save capabilities timestamp
		values.put(RichAddressBookData.KEY_CAPABILITY_TIMESTAMP, newCapabilities.getTimestamp());

		// Save presence infos
        PresenceInfo newPresenceInfo = newInfo.getPresenceInfo();
        values.put(RichAddressBookData.KEY_PRESENCE_SHARING_STATUS, newPresenceInfo.getPresenceStatus());
        values.put(RichAddressBookData.KEY_PRESENCE_FREE_TEXT, newPresenceInfo.getFreetext());
        FavoriteLink favLink = newPresenceInfo.getFavoriteLink();
        if (favLink == null) {
            values.put(RichAddressBookData.KEY_PRESENCE_WEBLINK_NAME, "");
            values.put(RichAddressBookData.KEY_PRESENCE_WEBLINK_URL, "");
        } else {
            values.put(RichAddressBookData.KEY_PRESENCE_WEBLINK_NAME, favLink.getName());
            values.put(RichAddressBookData.KEY_PRESENCE_WEBLINK_URL, favLink.getLink());
        }

        Geoloc geoloc = newPresenceInfo.getGeoloc();
        if (geoloc == null) {
            values.put(RichAddressBookData.KEY_PRESENCE_GEOLOC_EXIST_FLAG, RichAddressBookData.FALSE_VALUE);
            values.put(RichAddressBookData.KEY_PRESENCE_GEOLOC_LATITUDE, 0);
            values.put(RichAddressBookData.KEY_PRESENCE_GEOLOC_LONGITUDE, 0);
            values.put(RichAddressBookData.KEY_PRESENCE_GEOLOC_ALTITUDE, 0);
        } else {
            values.put(RichAddressBookData.KEY_PRESENCE_GEOLOC_EXIST_FLAG,  RichAddressBookData.TRUE_VALUE);
            values.put(RichAddressBookData.KEY_PRESENCE_GEOLOC_LATITUDE, geoloc.getLatitude());
            values.put(RichAddressBookData.KEY_PRESENCE_GEOLOC_LONGITUDE, geoloc.getLongitude());
            values.put(RichAddressBookData.KEY_PRESENCE_GEOLOC_ALTITUDE, geoloc.getAltitude());
        }
        values.put(RichAddressBookData.KEY_PRESENCE_TIMESTAMP, newPresenceInfo.getTimestamp());

        PhotoIcon photoIcon = newPresenceInfo.getPhotoIcon();
        if (photoIcon == null) {
            values.put(RichAddressBookData.KEY_PRESENCE_PHOTO_ETAG, "");
            values.put(RichAddressBookData.KEY_PRESENCE_PHOTO_EXIST_FLAG, RichAddressBookData.FALSE_VALUE);
        } else {
            if (photoIcon.getContent() != null) {
                values.put(RichAddressBookData.KEY_PRESENCE_PHOTO_EXIST_FLAG, RichAddressBookData.TRUE_VALUE);
            } else {
                values.put(RichAddressBookData.KEY_PRESENCE_PHOTO_EXIST_FLAG, RichAddressBookData.FALSE_VALUE);
            }
            values.put(RichAddressBookData.KEY_PRESENCE_PHOTO_ETAG, photoIcon.getEtag());
        }

		// Save registration state
		values.put(RichAddressBookData.KEY_REGISTRATION_STATE, newInfo.getRegistrationState());

        if (hasEntryInRichAddressBook) {
            // Update
            ctx.getContentResolver().update(RichAddressBookData.CONTENT_URI,
                    values, RichAddressBookData.KEY_CONTACT_NUMBER + "=?",
                    new String[] { contact });
        } else {
            // Insert
            ctx.getContentResolver().insert(RichAddressBookData.CONTENT_URI, values);
        }

        // Save presence photo content
        if (photoIcon != null) {
            byte photoContent[] = photoIcon.getContent();
            if (photoContent != null) {
                int rowId = getProfileRowId(contact);
                Uri photoUri = ContentUris.withAppendedId(RichAddressBookData.CONTENT_URI, rowId);
                try {
                    OutputStream outstream = ctx.getContentResolver().openOutputStream(photoUri);
                    outstream.write(photoContent);
                    outstream.flush();
                    outstream.close();
                } catch (FileNotFoundException e) {
                    if (logger.isActivated()){
                        logger.error("Photo can't be saved",e);
                    }
                } catch (IOException e) {
                    if (logger.isActivated()){
                        logger.error("Photo can't be saved",e);
                    }
                }
            }
        }

        // Get all the Ids from raw contacts that have this phone number
        List<Long> rawContactIds = getRawContactIdsFromPhoneNumber(contact);
        if (rawContactIds.isEmpty()) {
            // If the number is not in the native address book, we are done.
            return;
        }

        // For each, prepare the modifications
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        for (int i = 0; i < rawContactIds.size(); i++) {
            long rawContactId = rawContactIds.get(i);
            // Get the associated RCS raw contact id
            long rcsRawContactId = getAssociatedRcsRawContact(rawContactId, contact);

			if (!newInfo.isRcsContact()) {
				// If the contact is not a RCS contact anymore, we have to delete the corresponding native raw contacts
    			ops.add(ContentProviderOperation.newDelete(RawContacts.CONTENT_URI)
    					.withSelection(RawContacts._ID + "=?", new String[]{Long.toString(rcsRawContactId)})
    					.build());
        		// Also delete the corresponding entries in the aggregation provider
    			ctx.getContentResolver().delete(AggregationData.CONTENT_URI,
    					AggregationData.KEY_RCS_RAW_CONTACT_ID + "=?", 
    					new String[]{Long.toString(rcsRawContactId)});
			} else {
    			// If the contact is still a RCS contact, we have to update the native raw contacts
    			if (rcsRawContactId == INVALID_ID) {
    				// If no RCS raw contact id is associated to the raw contact, create one with the right infos
    				rcsRawContactId = createRcsContact(newInfo, rawContactId);
    				// Nothing to modify, as the new contact will have taken the new infos
    				continue;
    			}
    			
    			// Modify the contact type
    			List<ContentProviderOperation> contactTypeOps = modifyContactTypeForContact(rcsRawContactId, contact, newInfo.getRcsStatus(), oldInfo.getRcsStatus());
                for (int j = 0; j < contactTypeOps.size(); j++) {
    				ContentProviderOperation op = contactTypeOps.get(j); 
    				if (op!=null){
    					ops.add(op);
    				}
    			}
    			
    			// Modify the capabilities
    			// If the contact is not registered, do not set the capability to true
    			
    			// Cs Video
    			ContentProviderOperation op = modifyMimeTypeForContact(rcsRawContactId, contact, MIMETYPE_CAPABILITY_CS_VIDEO, newInfo.getCapabilities().isCsVideoSupported()&& isRegistered, oldInfo.getCapabilities().isCsVideoSupported());
    			if (op!=null){
    				ops.add(op);
    			}
    			// File transfer
    			op = modifyMimeTypeForContact(rcsRawContactId, contact, MIMETYPE_CAPABILITY_FILE_TRANSFER, newInfo.getCapabilities().isFileTransferSupported() && isRegistered, oldInfo.getCapabilities().isFileTransferSupported());
    			if (op!=null){
    				ops.add(op);
    			}
    			// Image sharing
    			op = modifyMimeTypeForContact(rcsRawContactId, contact, MIMETYPE_CAPABILITY_IMAGE_SHARING, newInfo.getCapabilities().isImageSharingSupported() && isRegistered, oldInfo.getCapabilities().isImageSharingSupported());
    			if (op!=null){
    				ops.add(op);
    			}
    			// IM session
    			// For IM, also check if the IM capability always on is activated, for RCS contacts
    			op = modifyMimeTypeForContact(rcsRawContactId, contact, MIMETYPE_CAPABILITY_IM_SESSION, (newInfo.getCapabilities().isImSessionSupported() && isRegistered)||(RcsSettings.getInstance().isImAlwaysOn() && newInfo.isRcsContact()), oldInfo.getCapabilities().isImSessionSupported());
    			if (op!=null){
    				ops.add(op);
    			}
    			// Video sharing
    			op = modifyMimeTypeForContact(rcsRawContactId, contact, MIMETYPE_CAPABILITY_VIDEO_SHARING, newInfo.getCapabilities().isVideoSharingSupported() && isRegistered, oldInfo.getCapabilities().isVideoSharingSupported());
    			if (op!=null){
    				ops.add(op);
    			}
    			// Presence discovery
    			op = modifyMimeTypeForContact(rcsRawContactId, contact, MIMETYPE_CAPABILITY_PRESENCE_DISCOVERY, newInfo.getCapabilities().isPresenceDiscoverySupported() && isRegistered, oldInfo.getCapabilities().isPresenceDiscoverySupported());
    			if (op!=null){
    				ops.add(op);
    			}
    			// Social presence
    			op = modifyMimeTypeForContact(rcsRawContactId, contact, MIMETYPE_CAPABILITY_SOCIAL_PRESENCE, newInfo.getCapabilities().isSocialPresenceSupported() && isRegistered, oldInfo.getCapabilities().isSocialPresenceSupported());
    			if (op!=null){
    				ops.add(op);
    			}
    			// Geolocation push
    			op = modifyMimeTypeForContact(rcsRawContactId, contact, MIMETYPE_CAPABILITY_GEOLOCATION_PUSH, newInfo.getCapabilities().isGeolocationPushSupported() && isRegistered, oldInfo.getCapabilities().isGeolocationPushSupported());
    			if (op!=null){
    				ops.add(op);
    			}
    			// File transfer thumbnail
    			op = modifyMimeTypeForContact(rcsRawContactId, contact, MIMETYPE_CAPABILITY_FILE_TRANSFER_THUMBNAIL, newInfo.getCapabilities().isFileTransferThumbnailSupported() && isRegistered, oldInfo.getCapabilities().isFileTransferThumbnailSupported());
    			if (op!=null){
    				ops.add(op);
    			}
    			// File transfer http
    			op = modifyMimeTypeForContact(rcsRawContactId, contact, MIMETYPE_CAPABILITY_FILE_TRANSFER_HTTP, newInfo.getCapabilities().isFileTransferHttpSupported() && isRegistered, oldInfo.getCapabilities().isFileTransferHttpSupported());
    			if (op!=null){
    				ops.add(op);
    			}
    			// RCS extensions
    			ArrayList<String> extensions = newInfo.getCapabilities().getSupportedExtensions();
    			if (!isRegistered){
    				// If contact is not registered, do not put any extensions
    				extensions.clear();
    			}
    			List<ContentProviderOperation> extensionOps = modifyExtensionsCapabilityForContact(rcsRawContactId, contact, extensions, oldInfo.getCapabilities().getSupportedExtensions());
    			for (int j=0;j<extensionOps.size();j++){
    				op = extensionOps.get(j);
    				if (op!=null){
    					ops.add(op);
    				}
    			}
    			// Contact capabilities timestamp
    			op = modifyCapabilityTimestampForContact(rcsRawContactId, contact, newInfo.getCapabilities().getTimestamp());
    			if (op!=null){
    				ops.add(op);
    			}
    			
    			// New contact registration state
    			String newFreeText = "";
    			if (newInfo.getPresenceInfo()!=null){
    				newFreeText = newInfo.getPresenceInfo().getFreetext();
    			}
    			// Old contact registration state
    			String oldFreeText = "";
    			if (oldInfo.getPresenceInfo()!=null){
    				oldFreeText = oldInfo.getPresenceInfo().getFreetext();
    			}
    			List<ContentProviderOperation> registrationOps = modifyContactRegistrationState(rcsRawContactId, contact, newInfo.getRegistrationState(), oldInfo.getRegistrationState(), newFreeText, oldFreeText);
    			for (int j=0;j<registrationOps.size();j++){
    				op = registrationOps.get(j);
    				if (op!=null){
    					ops.add(op);
    				}
    			}
    
    			// Presence fields
    			List<ContentProviderOperation> presenceOps = modifyPresenceForContact(rcsRawContactId, contact, newInfo.getPresenceInfo(), oldInfo.getPresenceInfo());
    			for (int j=0;j<presenceOps.size();j++){
    				op = presenceOps.get(j);
    				if (op!=null){
    					ops.add(op);
    				}
    			}
			}
		}
		
		if (!ops.isEmpty()){
			// Do the actual database modifications
			try {
				ctx.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
			} catch (RemoteException e) {
				if (logger.isActivated()){
					logger.error("Something went wrong when updating the database with the contact info",e);
				}
				throw new ContactsManagerException(e.getMessage());
			} catch (OperationApplicationException e) {
				if (logger.isActivated()){
					logger.error("Something went wrong when updating the database with the contact info",e);
				}
				throw new ContactsManagerException(e.getMessage());
			}
		}
	}

	/**
	 * Set the photo-icon of a contact in the EAB
	 * 
	 * @param contact Contact
	 * @param photoIcon PhotoIcon
	 * @throws ContactsManagerException
	 */
	public void setContactPhotoIcon(String contact, PhotoIcon photoIcon) throws ContactsManagerException {
		if (logger.isActivated()) {
			logger.info("Set photo-icon for contact " + contact);
		}

		if (!contact.equalsIgnoreCase(MYSELF)){
			// May be called from outside the core, so be sure the number format is international before doing the queries 
			contact = PhoneUtils.extractNumberFromUri(contact);
		}
		
		ContactInfo oldInfo = getContactInfo(contact);
		ContactInfo newInfo = new ContactInfo(oldInfo);
		// Get the presence info and modify the photo icon
		PresenceInfo presenceInfo = newInfo.getPresenceInfo();
		presenceInfo.setPhotoIcon(photoIcon);
		newInfo.setPresenceInfo(presenceInfo);
		// Set the new info
		setContactInfo(newInfo, oldInfo);
	}
	
	/**
	 * Remove the photo-icon of a contact in the EAB
	 * 
	 * @param contact Contact
	 * @param photoIcon PhotoIcon
	 * @throws ContactsManagerException
	 */
	public void removeContactPhotoIcon(String contact) throws ContactsManagerException {
		if (logger.isActivated()) {
			logger.info("Remove the photo-icon for contact " + contact);
		}

		if (!contact.equalsIgnoreCase(MYSELF)){
			// May be called from outside the core, so be sure the number format is international before doing the queries 
			contact = PhoneUtils.extractNumberFromUri(contact);
		}
		
		ContactInfo oldInfo = getContactInfo(contact);
		ContactInfo newInfo = new ContactInfo(oldInfo);
		// Get the presence info and remove the photo icon
		PresenceInfo presenceInfo = newInfo.getPresenceInfo();
		presenceInfo.setPhotoIcon(null);
		newInfo.setPresenceInfo(presenceInfo);
		// Set the new info
		setContactInfo(newInfo, oldInfo);
	}

	/**
	 * Get the infos of a contact in the EAB
	 *  	
	 * @param contact Contact
	 * @return Contact info
	 */
	public ContactInfo getContactInfo(String contact) {
		// May be called from outside the core, so be sure the number format is international before doing the queries 
		contact = PhoneUtils.extractNumberFromUri(contact);
		
		ContactInfo infos = new ContactInfo();
		infos.setRcsStatus(ContactInfo.NO_INFO);
		infos.setRcsStatusTimestamp(System.currentTimeMillis());
		infos.setContact(contact);		
		Capabilities capabilities = new Capabilities();
		PresenceInfo presenceInfo = new PresenceInfo();
		
		infos.setRegistrationState(ContactInfo.REGISTRATION_STATUS_UNKNOWN);

		Cursor cur = ctx.getContentResolver().query(RichAddressBookData.CONTENT_URI,
				null,
				RichAddressBookData.KEY_CONTACT_NUMBER + "= ?",
				new String[]{contact},
				null);
		if (cur != null) {
			if (cur.moveToFirst()) {
                // Get RCS Status
                infos.setRcsStatus(cur.getInt(cur.getColumnIndex(RichAddressBookData.KEY_RCS_STATUS)));
                infos.setRcsStatusTimestamp(cur.getLong(cur.getColumnIndex(RichAddressBookData.KEY_RCS_STATUS_TIMESTAMP)));
                infos.setRegistrationState(cur.getInt(cur.getColumnIndex(RichAddressBookData.KEY_REGISTRATION_STATE)));

                // Get Presence info
                presenceInfo.setPresenceStatus(cur.getString(cur.getColumnIndex(RichAddressBookData.KEY_PRESENCE_SHARING_STATUS)));

                FavoriteLink favLink = new FavoriteLink(
                        cur.getString(cur.getColumnIndex(RichAddressBookData.KEY_PRESENCE_WEBLINK_NAME)),
                        cur.getString(cur.getColumnIndex(RichAddressBookData.KEY_PRESENCE_WEBLINK_URL)));
                presenceInfo.setFavoriteLink(favLink);
                presenceInfo.setFavoriteLinkUrl(favLink.getLink());

                presenceInfo.setFreetext(cur.getString(cur.getColumnIndex(RichAddressBookData.KEY_PRESENCE_FREE_TEXT)));

                Geoloc geoloc = null;
                if (Boolean.parseBoolean(cur.getString(cur.getColumnIndex(RichAddressBookData.KEY_PRESENCE_GEOLOC_EXIST_FLAG)))) {
                    geoloc = new Geoloc(
                            cur.getDouble(cur.getColumnIndex(RichAddressBookData.KEY_PRESENCE_GEOLOC_LATITUDE)),
                            cur.getDouble(cur.getColumnIndex(RichAddressBookData.KEY_PRESENCE_GEOLOC_LONGITUDE)),
                            cur.getDouble(cur.getColumnIndex(RichAddressBookData.KEY_PRESENCE_GEOLOC_ALTITUDE)));
                }
                presenceInfo.setGeoloc(geoloc);

                presenceInfo.setTimestamp(cur.getLong(cur.getColumnIndex(RichAddressBookData.KEY_PRESENCE_TIMESTAMP)));

                PhotoIcon photoIcon = null;
                if (Boolean.parseBoolean(cur.getString(cur.getColumnIndex(RichAddressBookData.KEY_PRESENCE_PHOTO_EXIST_FLAG)))) {
                    try {
                        int rowId = cur.getInt(cur.getColumnIndex(RichAddressBookData.KEY_ID));
                        Uri photoUri = ContentUris.withAppendedId(RichAddressBookData.CONTENT_URI, rowId);
                        String etag = cur.getString(cur.getColumnIndex(RichAddressBookData.KEY_PRESENCE_PHOTO_ETAG));
                        InputStream stream = ctx.getContentResolver().openInputStream(photoUri);
                        byte[] content = new byte[stream.available()];
                        stream.read(content, 0, content.length);
                        Bitmap bmp = BitmapFactory.decodeByteArray(content, 0, content.length);
                        if (bmp != null) {
                            photoIcon = new PhotoIcon(content, bmp.getWidth(), bmp.getHeight(), etag);
                        }
                    } catch (FileNotFoundException e) {
                        if (logger.isActivated()){
                            logger.error("Can't get the photo",e);
                        }
                    } catch (IOException e) {
                        if (logger.isActivated()){
                            logger.error("Can't get the photo",e);
                        }
                    }
                }
                presenceInfo.setPhotoIcon(photoIcon);

                // Get the capabilities infos
                capabilities.setCsVideoSupport(getCapabilityFronColumn(cur, RichAddressBookData.KEY_CAPABILITY_CS_VIDEO));
                capabilities.setFileTransferSupport(getCapabilityFronColumn(cur, RichAddressBookData.KEY_CAPABILITY_FILE_TRANSFER));
                capabilities.setImageSharingSupport(getCapabilityFronColumn(cur, RichAddressBookData.KEY_CAPABILITY_IMAGE_SHARING));
                capabilities.setImSessionSupport(getCapabilityFronColumn(cur, RichAddressBookData.KEY_CAPABILITY_IM_SESSION));
                capabilities.setPresenceDiscoverySupport(getCapabilityFronColumn(cur, RichAddressBookData.KEY_CAPABILITY_PRESENCE_DISCOVERY));
                capabilities.setSocialPresenceSupport(getCapabilityFronColumn(cur, RichAddressBookData.KEY_CAPABILITY_SOCIAL_PRESENCE));
                capabilities.setGeolocationPushSupport(getCapabilityFronColumn(cur, RichAddressBookData.KEY_CAPABILITY_GEOLOCATION_PUSH));
                capabilities.setVideoSharingSupport(getCapabilityFronColumn(cur, RichAddressBookData.KEY_CAPABILITY_VIDEO_SHARING));
                capabilities.setFileTransferThumbnailSupport(getCapabilityFronColumn(cur, RichAddressBookData.KEY_CAPABILITY_FILE_TRANSFER_THUMBNAIL));
                capabilities.setFileTransferHttpSupport(getCapabilityFronColumn(cur, RichAddressBookData.KEY_CAPABILITY_FILE_TRANSFER_HTTP));

                // Set RCS extensions capability
				String extensions = cur.getString(cur.getColumnIndex(RichAddressBookData.KEY_CAPABILITY_EXTENSIONS));
				String[] extensionList = extensions.split(";");
				for (int i=0;i<extensionList.length;i++){
					if (extensionList[i].trim().length()>0){
						capabilities.addSupportedExtension(extensionList[i]);
					}
				}
				capabilities.setTimestamp(cur.getLong(cur.getColumnIndex(RichAddressBookData.KEY_CAPABILITY_TIMESTAMP)));

			}
			cur.close();
		}
		
		infos.setPresenceInfo(presenceInfo);
		infos.setCapabilities(capabilities);

		return infos;
	}

	/**
	 * Set the sharing status of a contact in the EAB
	 * 
	 * @param contact Contact
	 * @param status Sharing status
	 * @param reason Reason associated to the status
	 * @throws ContactsManagerException
	 */
	public void setContactSharingStatus(String contact, String status, String reason) throws ContactsManagerException {
		if (logger.isActivated()) {
			logger.info("Set sharing status for contact " + contact + " to "+status+ " with reason "+reason);
		}
		// May be called from outside the core, so be sure the number format is international before doing the queries 
		contact= PhoneUtils.extractNumberFromUri(contact);
		
		if (!isRcsValidNumber(contact)){
			if (logger.isActivated()){
				logger.debug(contact +" is not a RCS valid number");
			}
			return;
		}
		
		try {
			// Get the current contact sharing status EAB database, if there is one
			int currentStatus = getContactSharingStatus(contact);
			final ContactInfo oldInfo = getContactInfo(contact);
			ContactInfo newInfo = new ContactInfo(oldInfo);
			
			if (currentStatus==-1 || currentStatus!=ContactInfo.RCS_CANCELLED){
				// We already are in a given RCS state, different from cancelled
				/**
				 * INVITED STATE
				 */
				if (currentStatus==ContactInfo.RCS_PENDING_OUT){
					// State: we have invited the remote contact
					// We leave this state only on a "terminated/rejected" or an "active" status
					if(status.equalsIgnoreCase("terminated") &&
							(reason != null) && reason.equalsIgnoreCase("rejected")){
						// Contact has rejected our invitation, go back to RCS_CAPABLE state
						newInfo.setRcsStatus(ContactInfo.RCS_CAPABLE);
						setContactInfo(newInfo, oldInfo);
						if (logger.isActivated()) {
							logger.debug(contact + " has passed from \"pending_out\" to \"terminated/rejected\" state");
							logger.debug("=> Remove contact entry from EAB");
						}
						return;
					}else if (status.equalsIgnoreCase(PresenceInfo.RCS_ACTIVE)){
						// Contact has accepted our invitation, we are now active
						// Set contact type from RCS-capable to RCS
						newInfo.setRcsStatus(ContactInfo.RCS_ACTIVE);
						setContactInfo(newInfo, oldInfo);
						if (logger.isActivated()) {
							logger.debug(contact + " has passed from \"pending_out\" to \"active\" state");
						}
						return;
					}
				}
					
				/**
				 * WILLING STATE
				 */
				if (currentStatus==ContactInfo.RCS_PENDING){
					// State: we have been invited by the remote contact
					if(status.equalsIgnoreCase(PresenceInfo.RCS_ACTIVE)){
						// We have accepted the invitation, we are now active
						newInfo.setRcsStatus(ContactInfo.RCS_ACTIVE);
						setContactInfo(newInfo, oldInfo);
						if (logger.isActivated()) {
							logger.debug(contact + " has passed from \"pending\" to \"active\" state");
						}
						return;
					}else if (status.equalsIgnoreCase("terminated") &&
							(reason != null) && reason.equalsIgnoreCase("giveup")){
						// Contact has cancelled its invitation
						// Set contact type from RCS-capable to RCS
						newInfo.setRcsStatus(ContactInfo.RCS_CAPABLE);
						setContactInfo(newInfo, oldInfo);
						if (logger.isActivated()) {
							logger.debug(contact + " has passed from \"pending\" to \"terminated/giveup\" state");
							logger.debug("=> Remove contact entry from EAB");
						}
						return;
					}else if (status.equalsIgnoreCase(PresenceInfo.RCS_BLOCKED)){
						// We have declined the invitation
						// Set contact type from RCS-capable to RCS
						newInfo.setRcsStatus(ContactInfo.RCS_BLOCKED);
						setContactInfo(newInfo, oldInfo);
						if (logger.isActivated()) {
							logger.debug(contact + " has passed from \"pending\" to \"blocked\" state");
						}
						return;
					}
				}
				
				/**
				 * ACTIVE STATE
				 */
				if (currentStatus==ContactInfo.RCS_ACTIVE){
					// State: we have shared our profile with contact
					// We leave this state on a "terminated/rejected" status
					if(status.equalsIgnoreCase("terminated") &&
							(reason != null) && reason.equalsIgnoreCase("rejected")){
						// We have ended our profile sharing, destroy entry in EAB
						// Set contact type from RCS-capable to RCS
						newInfo.setRcsStatus(ContactInfo.RCS_CAPABLE);
						setContactInfo(newInfo, oldInfo);
						if (logger.isActivated()) {
							logger.debug(contact + " has passed from \"active\" to \"terminated/rejected\" state");
							logger.debug("=> Remove contact entry from EAB");
						}
						return;
					}
					// Or if we revoked the contact
					if (status.equalsIgnoreCase(PresenceInfo.RCS_REVOKED)){
						// Set contact type from RCS-capable to RCS
						newInfo.setRcsStatus(ContactInfo.RCS_CAPABLE);
						setContactInfo(newInfo, oldInfo);
						if (logger.isActivated()) {
							logger.debug(contact + " has passed from \"active\" to \"revoked\" state");
							logger.debug("=> Remove contact entry from EAB");
						}
						return;
					}
				}
				
				/**
				 * BLOCKED STATE
				 */
				if (currentStatus==ContactInfo.RCS_BLOCKED){
					// State: we have blocked this contact
					// We leave this state only when user unblocks, ie destroys the entry
						return;
				}
				
			}else if (currentStatus==-1||(currentStatus==ContactInfo.RCS_CANCELLED)){
				// We have no entry for contact in EAB or it was in cancelled state
				/**
				 * NO ENTRY IN EAB
				 */
				
				if (status.equalsIgnoreCase(PresenceInfo.RCS_PENDING_OUT)){
					// We invite contact to share presence
					// Contact has accepted our invitation, we are now active
					// Update entry in EAB
					// Set contact type from RCS-capable to RCS
					newInfo.setRcsStatus(ContactInfo.RCS_PENDING_OUT);
					setContactInfo(newInfo, oldInfo);
					if (logger.isActivated()) {
						logger.debug(contact + " has been added to the EAB with the \"pending_out\" state");
					}
					return;
				}
				
				if (status.equalsIgnoreCase(PresenceInfo.RCS_ACTIVE)){
					// We received an active state from the contact, but we have no entry for him in EAB yet
					// It may occur if the number was deleted from native EAB, or if there was an error when we deleted/modified it
					// or if we logged on this RCS account on a new phone
					// Set contact type from RCS-capable to RCS
					newInfo.setRcsStatus(ContactInfo.RCS_ACTIVE);
					setContactInfo(newInfo, oldInfo);
					if (logger.isActivated()) {
						logger.debug(contact + " has been added to the EAB with the \"active\" state");
					}
					return;
				}

				if (status.equalsIgnoreCase(PresenceInfo.RCS_PENDING)){
					// We received a "pending" notification => contact has invited us 
					// Update entry in EAB
					// Set contact type from RCS-capable to RCS
					newInfo.setRcsStatus(ContactInfo.RCS_PENDING);
					setContactInfo(newInfo, oldInfo);
					if (logger.isActivated()) {
						logger.debug(contact + " has been added to the EAB with the \"pending\" state");
					}
					return;
				}
				
				if (status.equalsIgnoreCase(PresenceInfo.RCS_BLOCKED)){
					// We block the contact to prevent invitations from him
					// Update entry in EAB
					// Set contact type from RCS-capable to RCS
					newInfo.setRcsStatus(ContactInfo.RCS_BLOCKED);
					setContactInfo(newInfo, oldInfo);
					if (logger.isActivated()) {
						logger.debug(contact + " has been added to the EAB with the \"blocked\" state");
					}
					return;
				}
			}

		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Internal exception", e);
			}
			throw new ContactsManagerException(e.getMessage());
		}
	}
	
	/**
	 * Get sharing status of a contact
	 *  
	 * @param contact Contact
	 * @return Status or -1 if contact not found or in case of error
	 */
	public int getContactSharingStatus(String contact) {
		if (logger.isActivated()) {
			logger.info("Get sharing status for contact " + contact);
		}
		
		// May be called from outside the core, so be sure the number format is international before doing the queries 
		contact = PhoneUtils.extractNumberFromUri(contact);
		
		int result = -1;
		try {
			// Get this number status in address book provider
			Cursor cursor = ctx.getContentResolver().query(RichAddressBookData.CONTENT_URI, 
					new String[]{RichAddressBookData.KEY_PRESENCE_SHARING_STATUS}, 
					RichAddressBookData.KEY_CONTACT_NUMBER + "=?", 
					new String[]{contact},
					null);
			if (cursor.moveToFirst()){
				result = cursor.getInt(0);
			}
			cursor.close();
			
	        if (logger.isActivated()) {
				logger.debug("Sharing status is " + result);
			}			
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Internal exception", e);
			}
		}
		return result;
	}

	/**
	 * Revoke a contact
	 * 
	 * @param contact Contact
	 * @throws ContactsManagerException
	 */
	public void revokeContact(String contact) throws ContactsManagerException {
		if (logger.isActivated()) {
			logger.info("Revoke contact " + contact);
		}

		try {
			ContactInfo oldInfo = getContactInfo(contact);
			ContactInfo newInfo = new ContactInfo(oldInfo);
			newInfo.setRcsStatus(ContactInfo.RCS_REVOKED);
			setContactInfo(newInfo, oldInfo);
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Internal exception", e);
			}
			throw new ContactsManagerException(e.getMessage());
		}
	}

	/**
	 * Unrevoke a contact
	 * 
	 * @param contact Contact
	 * @throws ContactsManagerException
	 */
	public void unrevokeContact(String contact) throws ContactsManagerException {
		if (logger.isActivated()) {
			logger.info("Unrevoke contact " + contact);
		}

		try {
			// Go back to RCS_CAPABLE state
			ContactInfo oldInfo = getContactInfo(contact);
			ContactInfo newInfo = new ContactInfo(oldInfo);
			newInfo.setRcsStatus(ContactInfo.RCS_CAPABLE);
			setContactInfo(newInfo, oldInfo);
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Internal exception", e);
			}
			throw new ContactsManagerException(e.getMessage());
		}
	}

	/**
	 * Block a contact
	 * 
	 * @param contact Contact
	 * @throws ContactsManagerException
	 */	
	public void blockContact(String contact) throws ContactsManagerException {
		if (logger.isActivated()) {
			logger.info("Block contact " + contact);
		}
		try{
			// Go to RCS_BLOCKED state
			ContactInfo oldInfo = getContactInfo(contact);
			ContactInfo newInfo = new ContactInfo(oldInfo);
			newInfo.setRcsStatus(ContactInfo.RCS_BLOCKED);
			setContactInfo(newInfo, oldInfo);
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Internal exception", e);
			}
			throw new ContactsManagerException(e.getMessage());
		}
	}

	/**
	 * Unblock a contact
	 * 
	 * @param contact Contact
	 * @throws ContactsManagerException
	 */	
	public void unblockContact(String contact) throws ContactsManagerException {
		if (logger.isActivated()) {
			logger.info("Unblock contact " + contact);
		}

		try {
			// Go back to RCS_CAPABLE state
			ContactInfo oldInfo = getContactInfo(contact);
			ContactInfo newInfo = new ContactInfo(oldInfo);
			newInfo.setRcsStatus(ContactInfo.RCS_CAPABLE);
			setContactInfo(newInfo, oldInfo);
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Internal exception", e);
			}
			throw new ContactsManagerException(e.getMessage());
		}
	}

	/**
	 * Flush the rich address book provider
	 */
	public void flushContactProvider(){
		String where = RichAddressBookData.KEY_CONTACT_NUMBER +"<> NULL";
		ctx.getContentResolver().delete(RichAddressBookData.CONTENT_URI, where, null);
	}
	
	/**
	 * Add, modify or delete a contact number to the rich address book provider
	 * 
	 * @param contact
	 * @param RCS status
	 */
	public void modifyRcsContactInProvider(String contact, int rcsStatus){
		
		// Check if an add or a modify must be done
		Cursor cursor = ctx.getContentResolver().query(RichAddressBookData.CONTENT_URI, 
				new String[]{RichAddressBookData.KEY_ID}, 
				RichAddressBookData.KEY_CONTACT_NUMBER + "=?", 
				new String[]{contact}, 
				null);
		long contactRowID = INVALID_ID;
		if (cursor.moveToFirst()){
			contactRowID = cursor.getLong(0);
		}
		cursor.close();

		ContentValues values = new ContentValues();
		values.put(RichAddressBookData.KEY_CONTACT_NUMBER, contact);
		values.put(RichAddressBookData.KEY_PRESENCE_SHARING_STATUS, rcsStatus);
		values.put(RichAddressBookData.KEY_TIMESTAMP, System.currentTimeMillis());
		if (contactRowID==INVALID_ID){
			// Contact not present in provider, insert
			ctx.getContentResolver().insert(RichAddressBookData.CONTENT_URI, values);
		}else{
			// Contact already present, update
			ctx.getContentResolver().update(RichAddressBookData.CONTENT_URI, 
					values,
					RichAddressBookData.KEY_CONTACT_NUMBER +"=?",
					new String[]{contact});
		}
	}

	/**
	 * Get the RCS contacts in the rich address book provider which have a presence relationship with the user
	 * 
	 * @return list containing all RCS contacts, "Me" item excluded 
	 */
	public List<String> getRcsContactsWithSocialPresence(){
		List<String> rcsNumbers = new ArrayList<String>();
		// Filter the rcs status
        String selection = "(" + RichAddressBookData.KEY_RCS_STATUS + "<>? AND " 
                + RichAddressBookData.KEY_RCS_STATUS + "<>? AND "
                + RichAddressBookData.KEY_RCS_STATUS + "<>? )";
        String[] selectionArgs = {
                String.valueOf(ContactInfo.NO_INFO),
                String.valueOf(ContactInfo.RCS_CAPABLE),
                String.valueOf(ContactInfo.NOT_RCS),
        };
		Cursor c = ctx.getContentResolver().query(RichAddressBookData.CONTENT_URI, 
				new String[] { RichAddressBookData.KEY_CONTACT_NUMBER}, 
				selection, 
				selectionArgs, 
				null);
		while (c.moveToNext()) {
			rcsNumbers.add(c.getString(0));
		}
		c.close();
		return rcsNumbers;
	}

	/**
	 * Get the RCS contacts in the contact contract provider
	 *
	 * @return list containing all RCS contacts 
	 */
	public List<String> getRcsContacts(){
        List<String> rcsNumbers = new ArrayList<String>();
        String[] projection = {
                RichAddressBookData.KEY_CONTACT_NUMBER
        };
        // Filter the rcs status
        String selection = "(" + RichAddressBookData.KEY_RCS_STATUS + "<>? AND " 
                + RichAddressBookData.KEY_RCS_STATUS + "<>? )";
        String[] selectionArgs = {
                String.valueOf(ContactInfo.NO_INFO),
                String.valueOf(ContactInfo.NOT_RCS),
        };
        Cursor cur = ctx.getContentResolver().query(RichAddressBookData.CONTENT_URI, 
                projection, 
                selection, 
                selectionArgs, 
                null);
        while (cur.moveToNext()) {
            String number = cur.getString(0);
            if (!rcsNumbers.contains(number)){
                rcsNumbers.add(number);
            }
        }
        cur.close();
		return rcsNumbers;
	}

	/**
	 * Get all the contacts in the rich address book provider
	 *
	 * @return list containing all contacts that have been at least queried once for capabilities
	 */
	public List<String> getAllContacts(){
		List<String> numbers = new ArrayList<String>();
		String[] projection = {
                RichAddressBookData.KEY_CONTACT_NUMBER
        };

        Cursor cur = ctx.getContentResolver().query(RichAddressBookData.CONTENT_URI, 
        		projection, 
        		null, 
        		null, 
        		null);
		
		while (cur.moveToNext()) {
			String number = cur.getString(0);
			if (!numbers.contains(number)){
				numbers.add(number);
			}
		}
		cur.close();
		return numbers;
	}

    /**
	 * Get blocked RCS contacts in the rich address book provider
	 * 
	 * @return list containing all RCS blocked contacts
	 */
	public List<String> getRcsBlockedContacts(){
		List<String> rcsNumbers = new ArrayList<String>();
		Cursor c = ctx.getContentResolver().query(RichAddressBookData.CONTENT_URI, 
				new String[] { RichAddressBookData.KEY_CONTACT_NUMBER}, 
				RichAddressBookData.KEY_PRESENCE_SHARING_STATUS + "=\""+PresenceInfo.RCS_BLOCKED+"\"", 
				null, 
				null);
		while (c.moveToNext()) {
			rcsNumbers.add(c.getString(0));
		}
		c.close();
		return rcsNumbers;
	}
	
	/**
	 * Get Invited RCS contacts in the rich address book provider
	 * 
	 * @return list containing all RCS invited contacts
	 */
	public List<String> getRcsInvitedContacts(){
		List<String> rcsNumbers = new ArrayList<String>();
		Cursor c = ctx.getContentResolver().query(RichAddressBookData.CONTENT_URI, 
				new String[] { RichAddressBookData.KEY_CONTACT_NUMBER}, 
				RichAddressBookData.KEY_PRESENCE_SHARING_STATUS + "=\""+PresenceInfo.RCS_PENDING_OUT+"\"", 
				null, 
				null);
		while (c.moveToNext()) {
			rcsNumbers.add(c.getString(0));
		}
		c.close();
		return rcsNumbers;
	}
	
	/**
	 * Get Willing RCS contacts in the rich address book provider
	 * 
	 * @return list containing all RCS willing contacts
	 */
	public List<String> getRcsWillingContacts(){
		List<String> rcsNumbers = new ArrayList<String>();
		Cursor c = ctx.getContentResolver().query(RichAddressBookData.CONTENT_URI, 
				new String[] { RichAddressBookData.KEY_CONTACT_NUMBER}, 
				RichAddressBookData.KEY_PRESENCE_SHARING_STATUS + "=\""+PresenceInfo.RCS_PENDING+"\"", 
				null, 
				null);
		while (c.moveToNext()) {
			rcsNumbers.add(c.getString(0));
		}
		c.close();
		return rcsNumbers;
	}
	
	/**
	 * Get Canceled RCS contacts in the rich address book provider
	 * 
	 * @return list containing all RCS cancelled contacts
	 */
	public List<String> getRcsCancelledContacts(){
		List<String> rcsNumbers = new ArrayList<String>();
		Cursor c = ctx.getContentResolver().query(RichAddressBookData.CONTENT_URI, 
				new String[] { RichAddressBookData.KEY_CONTACT_NUMBER}, 
				RichAddressBookData.KEY_PRESENCE_SHARING_STATUS + "=\""+PresenceInfo.RCS_CANCELLED+"\"", 
				null, 
				null);
		while (c.moveToNext()) {
			rcsNumbers.add(c.getString(0));
		}
		c.close();
		return rcsNumbers;
	}
	
	/**
	 * Remove a cancelled invitation in the rich address book provider
	 * 
	 * @param contact
	 */
	public void removeCancelledPresenceInvitation(String contact){
        // Remove entry from rich address book provider
		ctx.getContentResolver().delete(RichAddressBookData.CONTENT_URI, 
				RichAddressBookData.KEY_CONTACT_NUMBER +"=?" + " AND " + RichAddressBookData.KEY_PRESENCE_SHARING_STATUS + "=?",
				new String[]{contact, Integer.toString(ContactInfo.RCS_CANCELLED)});
	}
	
	/**
	 * Is the number in the RCS blocked list
	 * 
	 * @param number Number to check
	 * @return boolean
	 */
	public boolean isNumberBlocked(String number) {
		// Get this number status in address book
		int status = getContactSharingStatus(number);
		if (status==ContactInfo.RCS_BLOCKED){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * Is the number in the RCS buddy list
	 * 
	 * @param number Number to check
	 * @return boolean
	 */
	public boolean isNumberShared(String number) {
		// Get this number status in address book provider
		int status = -1;
		Cursor cursor = ctx.getContentResolver().query(RichAddressBookData.CONTENT_URI, 
				new String[]{RichAddressBookData.KEY_PRESENCE_SHARING_STATUS}, 
				RichAddressBookData.KEY_CONTACT_NUMBER + "=?", 
				new String[]{number},
				null);
		if (cursor.moveToFirst()){
			status = cursor.getInt(0);
		}
		cursor.close();
		if (status==ContactInfo.RCS_ACTIVE){
			return true;
		}else{
			return false;
		}
	}

	/**
	 * Has the number been invited to RCS
	 * 
	 * @param number Number to check
	 * @return boolean
	 */
	public boolean isNumberInvited(String number) {
		// Get this number status in address book provider
		int status = -1;
		Cursor cursor = ctx.getContentResolver().query(RichAddressBookData.CONTENT_URI, 
				new String[]{RichAddressBookData.KEY_PRESENCE_SHARING_STATUS}, 
				RichAddressBookData.KEY_CONTACT_NUMBER + "=?", 
				new String[]{number},
				null);
		if (cursor.moveToFirst()){
			status = cursor.getInt(0);
		}
		cursor.close();
		if (status==ContactInfo.RCS_PENDING){
			return true;
		}else{
			return false;
		}
	}

	/**
	 * Has the number invited us to RCS
	 * 
	 * @param number Number to check
	 * @return boolean
	 */
	public boolean isNumberWilling(String number) {
		// Get this number status in address book provider
		int status = -1;
		Cursor cursor = ctx.getContentResolver().query(RichAddressBookData.CONTENT_URI, 
				new String[]{RichAddressBookData.KEY_PRESENCE_SHARING_STATUS}, 
				RichAddressBookData.KEY_CONTACT_NUMBER + "=?", 
				new String[]{number},
				null);
		if (cursor.moveToFirst()){
			status = cursor.getInt(0);
		}
		cursor.close();
		if (status==ContactInfo.RCS_PENDING_OUT){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * Has the number invited us to RCS then be cancelled
	 * 
	 * @param number Number to check
	 * @return boolean
	 */
	public boolean isNumberCancelled(String number) {
		// Get this number status in address book provider
		int status = -1;
		Cursor cursor = ctx.getContentResolver().query(RichAddressBookData.CONTENT_URI, 
				new String[]{RichAddressBookData.KEY_PRESENCE_SHARING_STATUS}, 
				RichAddressBookData.KEY_CONTACT_NUMBER + "=?", 
				new String[]{number},
				null);
		if (cursor.moveToFirst()){
			status = cursor.getInt(0);
		}
		cursor.close();
		if (status==ContactInfo.RCS_CANCELLED){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * Check if number provided is a valid number for RCS
	 * <br>It is not valid if :
	 * <li>well formatted (not digits only or '+')
	 * <li>minimum length
	 * 
	 * @param phoneNumber
	 * @return true if it is a RCS valid number
	 */
    public boolean isRcsValidNumber(String phoneNumber){
        return android.telephony.PhoneNumberUtils.isGlobalPhoneNumber(phoneNumber) && (phoneNumber.length()>3);
    }
	
	/**
	 * Modify the contact type for the contact
	 * 
	 * @param rawContactId Raw contact id of the RCS contact
	 * @param number RCS number of the contact
	 * @param newContactType
	 * @param oldContactType 
	 * @return list of ContentProviderOperation to be done
	 */
	private ArrayList<ContentProviderOperation> modifyContactTypeForContact(long rawContactId, String rcsNumber, int newContactType, int oldContactType){
		if (newContactType==oldContactType){
			// Nothing to do
			return new ArrayList<ContentProviderOperation>();
		}
		
    	// Update data in rich address book provider
    	modifyRcsContactInProvider(rcsNumber, newContactType);
    	
    	ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
    	
    	switch(newContactType){
    	case ContactInfo.NOT_RCS:{
    		// We are now not RCS
    		if (oldContactType==ContactInfo.RCS_CAPABLE){
    			// Remove mime-type capable
    			ops.add(deleteMimeTypeForContact(rawContactId, rcsNumber, MIMETYPE_RCS_CAPABLE_CONTACT));
    		}else if (oldContactType==ContactInfo.RCS_ACTIVE){
    			// Remove mime-type rcs active
    			ops.add(deleteMimeTypeForContact(rawContactId, rcsNumber, MIMETYPE_RCS_CONTACT));
    		}

    		// Add mime-type not capable
    		ops.add(insertMimeTypeForContact(rawContactId, rcsNumber, MIMETYPE_NOT_RCS_CONTACT));
    	}
    	break;
    	case ContactInfo.RCS_ACTIVE:{
    		// We are now active
    		if (oldContactType==ContactInfo.RCS_CAPABLE){
    			// Remove mime-type capable
    			ops.add(deleteMimeTypeForContact(rawContactId, rcsNumber, MIMETYPE_RCS_CAPABLE_CONTACT));

    		}else if (oldContactType==ContactInfo.NOT_RCS || oldContactType==ContactInfo.NO_INFO){
    			// Remove mime-type not capable
    			ops.add(deleteMimeTypeForContact(rawContactId, rcsNumber, MIMETYPE_NOT_RCS_CONTACT));
    		}
    		// Add mime-type active
    		ops.add(insertMimeTypeForContact(rawContactId, rcsNumber, MIMETYPE_RCS_CONTACT));
    	}
    	break;
    	default:{
    		// Other types : contact is RCS capable
    		if (oldContactType==ContactInfo.NOT_RCS || oldContactType==ContactInfo.NO_INFO){
    			// Remove mime-type not capable active
    			ops.add(deleteMimeTypeForContact(rawContactId, rcsNumber, MIMETYPE_NOT_RCS_CONTACT));
    		}else if (oldContactType==ContactInfo.RCS_ACTIVE){
    			// Remove mime-type active
    			ops.add(deleteMimeTypeForContact(rawContactId, rcsNumber, MIMETYPE_RCS_CONTACT));
    		}

    		// Add mime-type RCS capable
    		ops.add(insertMimeTypeForContact(rawContactId, rcsNumber, MIMETYPE_RCS_CAPABLE_CONTACT));
    	}
    	}

    	// Update the RCS status row
    	ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
    			.withSelection(Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "=?", new String[]{String.valueOf(rawContactId), MIMETYPE_RCS_STATUS})
    			.withValue(Data.DATA2, newContactType)
    			.build());
    	
    	return ops;
	}
    
	/**
	 * Modify the corresponding mimetype row for the contact
	 * 
	 * @param rawContactId Raw contact id of the RCS contact
	 * @param number RCS number of the contact
	 * @param mimeType Mime type associated to the capability
	 * @param newState True if the capability must be enabled, else false
	 * @param oldState True if the capability was enabled, else false
	 * @return ContentProviderOperation to be done
	 */
	private ContentProviderOperation modifyMimeTypeForContact(long rawContactId, String rcsNumber, String mimeType, boolean newState, boolean oldState){
		
		if (newState==oldState){
			// Nothing to do
			return null;
		}
		if (newState==true){
			// We have to insert a new data in the raw contact
			return insertMimeTypeForContact(rawContactId, rcsNumber, mimeType);		
		}else{
			// We have to remove the data from the raw contact
			return deleteMimeTypeForContact(rawContactId, rcsNumber, mimeType);
		}
	}

    /**
     * Create (first time) the corresponding mimetype row for the contact
     *
     * @param rawContactId
     * @param rcsNumber
     * @param mimeType
     * @return ContentProviderOperation to be done
     */
    private ContentProviderOperation createMimeTypeForContact(int rawContactId, String rcsNumber, String mimeType) {
        String mimeTypeDescription = getMimeTypeDescription(mimeType);
        if (mimeTypeDescription != null) {
            // Check if there is a mimetype description to be added
            return ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, rawContactId)
                    .withValue(Data.MIMETYPE, mimeType)
                    .withValue(Data.DATA1, rcsNumber)
                    .withValue(Data.DATA2, mimeTypeDescription)
                    .withValue(Data.DATA3, rcsNumber)
                    .build();
        } else {
            return ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, rawContactId)
                    .withValue(Data.MIMETYPE, mimeType)
                    .withValue(Data.DATA1, rcsNumber)
                    .build();
        }
    }

    /**
     * Insert the corresponding mimetype row for the contact
     *
     * @param rawContactId
     * @param rcsNumber
     * @param mimeType
     * @return ContentProviderOperation to be done
     */
    private ContentProviderOperation insertMimeTypeForContact(long rawContactId, String rcsNumber, String mimeType) {
        String mimeTypeDescription = getMimeTypeDescription(mimeType);
        if (mimeTypeDescription != null) {
            // Check if there is a mimetype description to be added
            return ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValue(Data.RAW_CONTACT_ID, rawContactId)
                    .withValue(Data.MIMETYPE, mimeType)
                    .withValue(Data.DATA1, rcsNumber)
                    .withValue(Data.DATA2, mimeTypeDescription)
                    .withValue(Data.DATA3, rcsNumber)
                    .build();
        } else {
            return ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValue(Data.RAW_CONTACT_ID, rawContactId)
                    .withValue(Data.MIMETYPE, mimeType)
                    .withValue(Data.DATA1, rcsNumber)
                    .build();
        }
    }

	/**
	 * Remove the corresponding mimetype row for the contact
	 * 
	 * @param rawContactId
	 * @param rcsNumber
	 * @param mimeType
	 * @return ContentProviderOperation to be done
	 */
	private ContentProviderOperation deleteMimeTypeForContact(long rawContactId, String rcsNumber, String mimeType){
		// We have to remove a data from the raw contact
		return ContentProviderOperation.newDelete(Data.CONTENT_URI)
        .withSelection(Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "=?" + " AND "+ Data.DATA1 + "=?", new String[]{String.valueOf(rawContactId), mimeType, rcsNumber})
        .build();		
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
	private ArrayList<ContentProviderOperation> modifyContactRegistrationState(long rawContactId, String rcsNumber, int newRegistrationState, int oldRegistrationState, String newFreeText, String oldFreeText){
		
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		boolean registrationChanged = true;
		if ((newRegistrationState==oldRegistrationState || newRegistrationState==ContactInfo.REGISTRATION_STATUS_UNKNOWN)){
			registrationChanged = false;			
		}
		
		if (registrationChanged){
			// Modify registration status
			ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
					.withSelection(Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "=?" + " AND " + Data.DATA1 + "=?", new String[]{Long.toString(rawContactId), MIMETYPE_REGISTRATION_STATE, rcsNumber})
					.withValue(Data.DATA2, newRegistrationState)
					.build());
		}
		
		if (stringsHaveChanged(newFreeText, oldFreeText) || registrationChanged){
			int availability = PRESENCE_STATUS_NOT_SET;
			if (newRegistrationState==ContactInfo.REGISTRATION_STATUS_ONLINE){
				availability = PRESENCE_STATUS_ONLINE;
			}else if (newRegistrationState==ContactInfo.REGISTRATION_STATUS_OFFLINE){
				availability = PRESENCE_STATUS_OFFLINE;
			}

			// Get the id of the status update data linked to this raw contact id
			String[] projection = {Data._ID, Data.RAW_CONTACT_ID};

			long dataId = INVALID_ID;
			String selection = Data.RAW_CONTACT_ID + "=?";
			String[] selectionArgs = { Long.toString(rawContactId)};
			Cursor cur = ctx.getContentResolver().query(Data.CONTENT_URI, 
					projection, 
					selection, 
					selectionArgs, 
					null);
			if (!cur.moveToNext()) {
				dataId = INVALID_ID;
			}else{
				dataId = cur.getLong(0);
			}
			cur.close();

			ops.add(ContentProviderOperation.newInsert(StatusUpdates.CONTENT_URI)
					.withValue(StatusUpdates.DATA_ID, dataId)
					.withValue(StatusUpdates.STATUS, newFreeText)
					.withValue(StatusUpdates.STATUS_RES_PACKAGE, ctx.getPackageName())
					.withValue(StatusUpdates.STATUS_LABEL, R.string.rcs_core_account_id)
					.withValue(StatusUpdates.STATUS_ICON, R.drawable.rcs_icon)
					.withValue(StatusUpdates.PRESENCE, availability)
					// Needed for inserting PRESENCE
					.withValue(StatusUpdates.PROTOCOL, Im.PROTOCOL_CUSTOM)
					.withValue(StatusUpdates.CUSTOM_PROTOCOL, " " /* Intentional left blank */)
					.withValue(StatusUpdates.STATUS_TIMESTAMP, System.currentTimeMillis())
					.build());
		}
		
		return ops;
	}
	
	/**
	 * Modify the RCS extensions capability for the contact
	 * 
	 * @param rawContactId Raw contact id of the RCS contact
	 * @param number RCS number of the contact
	 * @param newExtensions New extensions capabilities
	 * @param oldExtensions Old extensions capabilities 
	 * @return list of contentProviderOperation to be done
	 */
	private List<ContentProviderOperation> modifyExtensionsCapabilityForContact(long rawContactId, String rcsNumber, ArrayList<String> newExtensions, ArrayList<String> oldExtensions){

		List<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		
		// Compare the two lists of extensions
		if (newExtensions.containsAll(oldExtensions) && oldExtensions.containsAll(newExtensions)){
			// Both lists have the same tags, no need to update
			return ops;
		}
		
		StringBuffer extension = new StringBuffer();
		for (int j=0;j<newExtensions.size();j++){
			extension.append(newExtensions.get(j)+";");
		}
		
        // Check if we support at least one of the extensions this contact has
        boolean oldHasCommonExtensions = false;
        boolean newHasCommonExtensions = false;
        
        // Get my extensions
        String exts = RcsSettings.getInstance().getSupportedRcsExtensions();
		if ((exts != null) && (exts.length() > 0)) {
			String[] ext = exts.split(",");
			for(int i=0; i < ext.length; i++) {
				String capability = ext[i];
				if (newExtensions.contains(capability)){
					newHasCommonExtensions = true;
				}
				if (oldExtensions.contains(capability)){
					oldHasCommonExtensions = true;
				}
			}
		}
        
		// Add or remove the common extensions mimetype
		ops.add(modifyMimeTypeForContact(rawContactId, rcsNumber, MIMETYPE_CAPABILITY_COMMON_EXTENSION, newHasCommonExtensions, oldHasCommonExtensions));
		
        // Update extensions        
        ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
        		.withSelection(Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "=?" + " AND " + Data.DATA1 + "=?", new String[]{String.valueOf(rawContactId), MIMETYPE_CAPABILITY_EXTENSIONS, rcsNumber})
        		.withValue(Data.DATA2, extension.toString())
        		.build());
		return ops;
	}
	
	/**
	 * Modify the presence info for a contact
	 * 
	 * @param rawContactId Raw contact id of the RCS contact
	 * @param number RCS number of the contact
	 * @param newPresenceInfo
	 * @param oldPresenceInfo
	 * @return list of ContentProviderOperation to be done
	 */
	private ArrayList<ContentProviderOperation> modifyPresenceForContact(long rawContactId, String rcsNumber, PresenceInfo newPresenceInfo, PresenceInfo oldPresenceInfo){
    	ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

    	if (newPresenceInfo!=null && oldPresenceInfo!=null){
    		// Both are not null, check the differences and update fields
    		if (newPresenceInfo.isOffline()!=oldPresenceInfo.isOffline()
    				|| newPresenceInfo.isOnline()!=oldPresenceInfo.isOnline()){
    			int availability = PRESENCE_STATUS_NOT_SET;
    			if (newPresenceInfo.isOnline()){
    				availability = PRESENCE_STATUS_ONLINE;	
    			}else if (newPresenceInfo.isOffline()){
    				availability = PRESENCE_STATUS_OFFLINE;
    			}

    			// Modify the presence status
    			ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
    					.withSelection(Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "=?" + " AND " + Data.DATA1 + "=?", new String[]{String.valueOf(rawContactId), MIMETYPE_PRESENCE_STATUS, rcsNumber})
    					.withValue(Data.DATA2, availability)
    					.build());
    		}

    		if (stringsHaveChanged(newPresenceInfo.getFreetext(), oldPresenceInfo.getFreetext())){
    			// Modify the free text
    			ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
    					.withSelection(Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "=?" + " AND " + Data.DATA1 + "=?", new String[]{String.valueOf(rawContactId), MIMETYPE_FREE_TEXT, rcsNumber})
    					.withValue(Data.DATA2, newPresenceInfo.getFreetext())
    					.build());
    		}

    		if (stringsHaveChanged(newPresenceInfo.getFavoriteLinkUrl(), oldPresenceInfo.getFavoriteLinkUrl())){
    			// Modify the web link
    			ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
    					.withSelection(Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "=?" + " AND " + Data.DATA1 + "=?", new String[]{String.valueOf(rawContactId), MIMETYPE_WEBLINK, rcsNumber})
    					.withValue(Data.DATA2, newPresenceInfo.getFavoriteLinkUrl())
    					.build());
    			
        		//Add the weblink to the native @book
    			ContentValues values = new ContentValues();
    			values.put(Data.RAW_CONTACT_ID, rawContactId);
    			values.put(Data.MIMETYPE, MIMETYPE_WEBLINK);
    			values.put(Website.URL, newPresenceInfo.getFavoriteLinkUrl());
    			values.put(Website.TYPE, Website.TYPE_HOMEPAGE);
    			values.put(Data.IS_PRIMARY, 1);
   				values.put(Data.IS_SUPER_PRIMARY, 1);

				// Get the id of the current weblink mimetype
				long currentNativeWebLinkDataId = INVALID_ID;
    	    	Cursor cur = ctx.getContentResolver().query(Data.CONTENT_URI, 
    	    			new String[]{ Data._ID }, 
    	    			Data.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "=? AND " + Website.TYPE + "=?", 
    	    			new String[]{ Long.toString(rawContactId), MIMETYPE_WEBLINK, String.valueOf(Website.TYPE_HOMEPAGE) },
    	    			null);
    	    	if (cur.moveToNext()) {
    				currentNativeWebLinkDataId = cur.getLong(0);
    	    	}
    	    	cur.close();
   				
    			if (oldPresenceInfo.getFavoriteLinkUrl()==null){
    				// There was no weblink, insert
        			ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
        					.withValues(values)
        					.build());
    			}else if (newPresenceInfo.getFavoriteLinkUrl()!=null){
    				// Update the existing weblink
    				ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
    					.withSelection(Data._ID + "=?", new String[]{String.valueOf(currentNativeWebLinkDataId)})
    					.withValues(values)
    					.build());
    			}else{
    				// Remove the existing weblink
    				ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI)
        					.withSelection(Data._ID + "=?", new String[]{String.valueOf(currentNativeWebLinkDataId)})
        					.build());
    			}
    		}

    		// Set the photo-icon
    		PhotoIcon oldPhotoIcon = oldPresenceInfo.getPhotoIcon();
    		PhotoIcon newPhotoIcon = newPresenceInfo.getPhotoIcon();
    		// Check if photo etags are the same between the two presenceInfo
    		boolean haveSameEtags = false;
    		String oldPhotoIconEtag = null;
    		String newPhotoIconEtag = null;
    		if (oldPhotoIcon!=null){
    			oldPhotoIconEtag = oldPhotoIcon.getEtag();
    		}
    		if (newPhotoIcon!=null){
    			newPhotoIconEtag = newPhotoIcon.getEtag();
    		}
    		if (oldPhotoIconEtag==null && newPhotoIconEtag==null){
    			haveSameEtags = true;
    		}else if(oldPhotoIconEtag!=null && newPhotoIconEtag!=null){
    			haveSameEtags = (oldPhotoIconEtag.equalsIgnoreCase(newPhotoIconEtag));
    		}
    			
    		if (!haveSameEtags){
    			// Not the same etag, so photo changed
    			// Replace photo and etag
    			List<ContentProviderOperation> photoOps = setContactPhoto(rawContactId, newPhotoIcon, true);
    			for (int i=0;i<photoOps.size();i++){
    				ContentProviderOperation op = photoOps.get(i);
    				if (op!=null){
    					ops.add(op);
    				}
    			}
    		}

    		if (oldPresenceInfo.getTimestamp()!=newPresenceInfo.getTimestamp()){
    			// Update the presence timestamp
    			ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
    					.withSelection(Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "=?" + " AND " + Data.DATA1 + "=?", new String[]{String.valueOf(rawContactId), MIMETYPE_PRESENCE_TIMESTAMP, rcsNumber})
    					.withValue(Data.DATA2, newPresenceInfo.getTimestamp())
    					.build());
    		}
    	} else if (newPresenceInfo!=null) {
    		// The new presence info is not null but the old one was, add new fields 
    		int availability = ContactInfo.REGISTRATION_STATUS_UNKNOWN;
    		if (newPresenceInfo.isOnline()){
    			availability = ContactInfo.REGISTRATION_STATUS_ONLINE;	
    		}else if (newPresenceInfo.isOffline()){
    			availability = ContactInfo.REGISTRATION_STATUS_OFFLINE;
    		}
    		
    		// Add the presence status to native address book
    		ArrayList<ContentProviderOperation> registrationStateOps = modifyContactRegistrationState(rawContactId, rcsNumber, availability, -1, newPresenceInfo.getFreetext(), "");
    		for (int i=0;i<registrationStateOps.size();i++){
    			ContentProviderOperation op = registrationStateOps.get(i);
    			if (op!=null){
    				ops.add(op);
    			}
    		}

    		// Insert presence status
    		ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
    				.withValue(Data.RAW_CONTACT_ID, rawContactId)
    				.withValue(Data.MIMETYPE, MIMETYPE_PRESENCE_STATUS)
    				.withValue(Data.DATA1, rcsNumber)
    				.withValue(Data.DATA2, newPresenceInfo.getPresenceStatus())
    				.build());

    		// Insert presence free text        
    		ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
    				.withValue(Data.RAW_CONTACT_ID, rawContactId)
    				.withValue(Data.MIMETYPE, MIMETYPE_FREE_TEXT)
    				.withValue(Data.DATA1, rcsNumber)
    				.withValue(Data.DATA2, newPresenceInfo.getFreetext())
    				.build());

    		// Insert presence web link        
    		ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
    				.withValue(Data.RAW_CONTACT_ID, rawContactId)
    				.withValue(Data.MIMETYPE, MIMETYPE_WEBLINK)
    				.withValue(Data.DATA1, rcsNumber)
    				.withValue(Data.DATA2, newPresenceInfo.getFavoriteLinkUrl())
    				.build());

    		//Add the weblink to the native @book
			ContentValues values = new ContentValues();
			values.put(Data.RAW_CONTACT_ID, rawContactId);
			values.put(Data.MIMETYPE, MIMETYPE_WEBLINK);
			values.put(Website.URL, newPresenceInfo.getFavoriteLinkUrl());
			values.put(Website.TYPE, Website.TYPE_HOMEPAGE);
			values.put(Data.IS_PRIMARY, 1);
			values.put(Data.IS_SUPER_PRIMARY, 1);

			// Get the id of the current weblink mimetype
			long currentNativeWebLinkDataId = INVALID_ID;
	    	Cursor cur = ctx.getContentResolver().query(Data.CONTENT_URI, 
	    			new String[]{ Data._ID }, 
	    			Data.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "=? AND " + Website.TYPE + "=?", 
	    			new String[]{ Long.toString(rawContactId), MIMETYPE_WEBLINK, String.valueOf(Website.TYPE_HOMEPAGE) },
	    			null);
	    	if (cur.moveToNext()) {
				currentNativeWebLinkDataId = cur.getLong(0);
	    	}
	    	cur.close();
				
			if (oldPresenceInfo.getFavoriteLinkUrl()==null){
				// There was no weblink, insert
    			ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
    					.withValues(values)
    					.build());
			}else if (newPresenceInfo.getFavoriteLinkUrl()!=null){
				// Update the existing weblink
				ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
					.withSelection(Data._ID + "=?", new String[]{String.valueOf(currentNativeWebLinkDataId)})
					.withValues(values)
					.build());
			}else{
				// Remove the existing weblink
				ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI)
    					.withSelection(Data._ID + "=?", new String[]{String.valueOf(currentNativeWebLinkDataId)})
    					.build());
			}

    		// Set the photo
			List<ContentProviderOperation> photoOps = setContactPhoto(rawContactId, newPresenceInfo.getPhotoIcon(), true);
			for (int i=0;i<photoOps.size();i++){
				ContentProviderOperation op = photoOps.get(i);
				if (op!=null){
					ops.add(op);
				}
			}			

			// Update timestamp
    		if (oldPresenceInfo.getTimestamp()!=newPresenceInfo.getTimestamp()){
    			// Update the presence timestamp
    			ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
    					.withSelection(Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "=?" + " AND " + Data.DATA1 + "=?", new String[]{String.valueOf(rawContactId), MIMETYPE_PRESENCE_TIMESTAMP, rcsNumber})
    					.withValue(Data.DATA2, newPresenceInfo.getTimestamp())
    					.build());
    		}
    	} else if (oldPresenceInfo!=null) {
    		// The new presence info is null but the old one was not, remove fields
    		
    		// Remove the presence status to native address book
    		// Force presence status to offline and free text to null
    		ArrayList<ContentProviderOperation> registrationStateOps = modifyContactRegistrationState(rawContactId, rcsNumber, ContactInfo.REGISTRATION_STATUS_OFFLINE, -1, "", oldPresenceInfo.getFreetext());
    		for (int i=0;i<registrationStateOps.size();i++){
    			ContentProviderOperation op = registrationStateOps.get(i);
    			if (op!=null){
    				ops.add(op);
    			}
    		}
    		
    		// Remove presence status
    		ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI)
    				.withSelection(Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "=?" + " AND " + Data.DATA1 + "=?", new String[]{String.valueOf(rawContactId), MIMETYPE_PRESENCE_STATUS, rcsNumber})
    				.build());

    		// Remove presence free text        
    		ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI)
    				.withSelection(Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "=?" + " AND " + Data.DATA1 + "=?", new String[]{String.valueOf(rawContactId), MIMETYPE_FREE_TEXT, rcsNumber})
    				.build());
    		
    		// Remove presence web link        
    		ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI)
    				.withSelection(Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "=?" + " AND " + Data.DATA1 + "=?", new String[]{String.valueOf(rawContactId), MIMETYPE_WEBLINK, rcsNumber})
    				.build());

    		//Remove presence web link in native address book
    		//Add the weblink to the native @book
			// Get the id of the current weblink mimetype
			long currentNativeWebLinkDataId = INVALID_ID;
	    	Cursor cur = ctx.getContentResolver().query(Data.CONTENT_URI, 
	    			new String[]{ Data._ID }, 
	    			Data.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "=? AND " + Website.TYPE + "=?", 
	    			new String[]{ Long.toString(rawContactId), MIMETYPE_WEBLINK, String.valueOf(Website.TYPE_HOMEPAGE) },
	    			null);
	    	if (cur.moveToNext()) {
				currentNativeWebLinkDataId = cur.getLong(0);
	    	}
	    	cur.close();
				
			ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI)
    					.withSelection(Data._ID + "=?", new String[]{String.valueOf(currentNativeWebLinkDataId)})
    					.build());

    		// Set the photo
			List<ContentProviderOperation> photoOps = setContactPhoto(rawContactId, null, true);
			for (int i=0;i<photoOps.size();i++){
				ContentProviderOperation op = photoOps.get(i);
				if (op!=null){
					ops.add(op);
				}
			}
    		
			// Update the presence timestamp
			ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
					.withSelection(Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "=?" + " AND " + Data.DATA1 + "=?", new String[]{String.valueOf(rawContactId), MIMETYPE_PRESENCE_TIMESTAMP, rcsNumber})
					.withValue(Data.DATA2, System.currentTimeMillis())
					.build());
    	}
    	
    	return ops;
	}
	
	/**
	 * Check if strings have changed
	 * 
	 * @param new string
	 * @param old string
	 * @return true if the string are the same, else false
	 */
	private boolean stringsHaveChanged(String newString, String oldString){
        if (newString == null) {
            if (oldString == null) {
                // Both are null
                return false;
            } else {
                // One string is null and not the other one
                return true;
            }
        } else {
            if (oldString == null) {
                // One string is null and not the other one
                return true;
            } else {
                // Both strings are not null, compare
                return (!newString.equalsIgnoreCase(oldString));
            }
        }
	}
	
	/**
	 * Get description associated to a MIME type. This string will be visible in the contact card
	 * 
	 * @param mimeType MIME type
	 * @return String
	 */
	private String getMimeTypeDescription(String mimeType){
		if (mimeType.equalsIgnoreCase(MIMETYPE_CAPABILITY_FILE_TRANSFER)) {
			return ctx.getString(R.string.rcs_core_contact_file_transfer);
		} else
		if (mimeType.equalsIgnoreCase(MIMETYPE_CAPABILITY_IM_SESSION)) {
			return ctx.getString(R.string.rcs_core_contact_im_session);
		} else
		if (mimeType.equalsIgnoreCase(MIMETYPE_CAPABILITY_CS_VIDEO)) {
			return ctx.getString(R.string.rcs_core_contact_cs_video);
		} else
		if (mimeType.equalsIgnoreCase(MIMETYPE_CAPABILITY_COMMON_EXTENSION)) {
			return ctx.getString(R.string.rcs_core_contact_extensions);
		} else 
			return null;
	}
	
	/**
	 * Get a list of numbers supporting Instant Messaging Session capability
	 * 
	 * @return list containing all contacts that are "IM capable" 
	 */
	public List<String> getImSessionCapableContacts() {
		List<String> IMCapableNumbers = new ArrayList<String>();
        String[] projection = {Data.DATA1, Data.MIMETYPE};
        String selectionImCapOff = Data.MIMETYPE + "=?";
        String[] selectionArgsImCapOff = { MIMETYPE_CAPABILITY_IM_SESSION };
    	Cursor c =  ctx.getContentResolver().query(Data.CONTENT_URI, 
        		projection, 
        		selectionImCapOff, 
        		selectionArgsImCapOff, 
        		null);
		
		while (c.moveToNext()) {
			String imCapableNumber = c.getString(0);
			if (!IMCapableNumbers.contains(imCapableNumber)){
				IMCapableNumbers.add(imCapableNumber);
			}
		}
		c.close();
		return IMCapableNumbers;
	}
	
	/**
	 * Get a list of numbers that can use richcall features
	 * <li>These are the contacts that can do either image sharing, video sharing, or both 
	 * 
	 * @return list containing all contacts that can use richcall features 
	 */
	public List<String> getRichcallCapableContacts() {
		List<String> richcallCapableNumbers = new ArrayList<String>();
        String[] projection = {Data.DATA1, Data.MIMETYPE};

        String selection = "( " + Data.MIMETYPE + "=? OR " + Data.MIMETYPE + "=? )";
        
        // Get all image sharing or video sharing capable contacts
        String[] selectionArgs = { MIMETYPE_CAPABILITY_IMAGE_SHARING, MIMETYPE_CAPABILITY_VIDEO_SHARING };
        Cursor c = ctx.getContentResolver().query(Data.CONTENT_URI, 
        		projection, 
        		selection, 
        		selectionArgs, 
        		null);
		
		while (c.moveToNext()) {
			String richcallCapableNumber = c.getString(0);
			if (!richcallCapableNumbers.contains(richcallCapableNumber)){
				richcallCapableNumbers.add(richcallCapableNumber);
			}
		}
		c.close();
		
		return richcallCapableNumbers;
	}
	
	/**
	 * Get a list of numbers that are available (answered last OPTIONS check)
	 *  
	 * @return list containing all contacts that are available 
	 */
	public List<String> getAvailableContacts() {
		List<String> availableNumbers = new ArrayList<String>();
        String[] projection = {Data.DATA1, Data.DATA2, Data.MIMETYPE};
        String selection = Data.MIMETYPE + "=?";
        
        // Get all registration state entries
        String[] selectionArgs = { MIMETYPE_REGISTRATION_STATE };
        Cursor c = ctx.getContentResolver().query(Data.CONTENT_URI, 
        		projection, 
        		selection, 
        		selectionArgs, 
        		null);
		while (c.moveToNext()) {
			String availableNumber = c.getString(0);
			int registrationState = c.getInt(1);
			if (registrationState==ContactInfo.REGISTRATION_STATUS_ONLINE && !availableNumbers.contains(availableNumber)){
				// If the registration status is online, add the number to the list
				availableNumbers.add(availableNumber);
			}
		}
		c.close();
		
		return availableNumbers;
	}
	
	/**
	 * Is the contact RCS active
	 * 
	 * @param contact
	 * @return boolean
	 */
	public boolean isContactRcsActive(String contact){
		contact = PhoneUtils.extractNumberFromUri(contact);
		if (isNumberShared(contact)){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * Set contact capabilities
	 * 
	 * @param contact Contact
	 * @param capabilities Capabilities
	 * @param contactType Contact type
	 * @param registrationState Three possible values : online/offline/unknown
	 */
	public void setContactCapabilities(String contact, Capabilities capabilities, int contactType, int registrationState) {
        
		contact = PhoneUtils.extractNumberFromUri(contact);

		// Get the current information on this contact 
		ContactInfo oldInfo = getContactInfo(contact);
		ContactInfo newInfo = new ContactInfo(oldInfo);
		
		// Set the contact type 
		newInfo.setRcsStatus(contactType);
		
		// Set the registration state
		newInfo.setRegistrationState(registrationState);

		// Modify the capabilities regarding the registration state		
		boolean isRegistered = (registrationState==ContactInfo.REGISTRATION_STATUS_ONLINE);
		// Cs Video
		capabilities.setCsVideoSupport(capabilities.isCsVideoSupported() && isRegistered);

		// File transfer
		capabilities.setFileTransferSupport(capabilities.isFileTransferSupported() && isRegistered);
		// Image sharing
		capabilities.setImageSharingSupport(capabilities.isImageSharingSupported() && isRegistered);
		// IM session
		// This capability is enabled:
		// - if the capability is present and the contact is registered
		// - if the IM store&forward is enabled and the contact is RCS capable
		capabilities.setImSessionSupport((capabilities.isImSessionSupported() && isRegistered) 
				|| (RcsSettings.getInstance().isImAlwaysOn() && newInfo.isRcsContact()));
		// Video sharing
		capabilities.setVideoSharingSupport(capabilities.isVideoSharingSupported() && isRegistered);
		
		// Geolocation push
		capabilities.setGeolocationPushSupport(capabilities.isGeolocationPushSupported() && isRegistered);

		// FT Thumbnail
		capabilities.setFileTransferThumbnailSupport(capabilities.isFileTransferThumbnailSupported() && isRegistered);

		// FT Http
		capabilities.setFileTransferHttpSupport(capabilities.isFileTransferHttpSupported() && isRegistered);
		
		// Add the capabilities
		newInfo.setCapabilities(capabilities);

		// Save the modifications
		try {
			setContactInfo(newInfo, oldInfo);
		} catch (ContactsManagerException e) {
			if (logger.isActivated()){
				logger.error("Could not save the contact modifications",e);
			}
		}
	}
	
	/**
	 * Set contact capabilities
	 * 
	 * @param contact Contact
	 * @param capabilities Capabilities
	 */
	public void setContactCapabilities(String contact, Capabilities capabilities) {
		
		contact = PhoneUtils.extractNumberFromUri(contact);

		// Get the current information on this contact 
		ContactInfo oldInfo = getContactInfo(contact);
		ContactInfo newInfo = new ContactInfo(oldInfo);
		
		newInfo.setCapabilities(capabilities);
		
		// Save the modifications
		try {
			setContactInfo(newInfo, oldInfo);
		} catch (ContactsManagerException e) {
			if (logger.isActivated()){
				logger.error("Could not save the contact modifications",e);
			}
		}
	}
	
	/**
	 * Get contact capabilities
	 * <br>If contact has never been enriched with capability, returns null
	 * 
	 * @param contact
	 * @return capabilities
	 */
	public Capabilities getContactCapabilities(String contact){
		ContactInfo contactInfo = getContactInfo(contact);
		if (contactInfo.getRcsStatus()==ContactInfo.NO_INFO){
			return null;
		}else{
			return contactInfo.getCapabilities();
		}
	}
	
	/**
	 * Set contact capabilities timestamp
	 * 
	 * @param contact
	 * @param timestamp
	 */
	public void setContactCapabilitiesTimestamp(String contact, long timestamp){
		if (logger.isActivated()){
			logger.debug("Setting contact capabilities timestamp for "+contact +" to "+timestamp);
		}
		ContactInfo oldInfo = getContactInfo(contact);
		ContactInfo newInfo = new ContactInfo(oldInfo);
		Capabilities capabilities = newInfo.getCapabilities();
		capabilities.setTimestamp(timestamp);
		newInfo.setCapabilities(capabilities);
		try {
			setContactInfo(newInfo, oldInfo);
		} catch (ContactsManagerException e) {
			if (logger.isActivated()){
				logger.error("Could not update the contact capabilities timestamp",e);
			}
		}
	}
	
	/**
	 * Modify the RCS capability timestamp for the contact
	 * 
	 * @param rawContactId Raw contact id of the RCS contact
	 * @param number RCS number of the contact
	 * @param timestamp New timestamp 
	 * @return content
	 */
	private ContentProviderOperation modifyCapabilityTimestampForContact(long rawContactId, String rcsNumber, long timestamp){
		return ContentProviderOperation.newUpdate(Data.CONTENT_URI)
		.withSelection(Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "=?" + " AND " + Data.DATA1 + "=?", new String[]{Long.toString(rawContactId), MIMETYPE_CAPABILITY_TIMESTAMP, rcsNumber})
		.withValue(Data.DATA2, timestamp)
		.build();
	}
	
    /**
     * Utility method to create new "RCS" raw contact, that aggregates with other raw contact
     *
     * @param contact info for the RCS raw contact
     * @param id of the raw contact we want to aggregate the RCS infos to
     * @return the RCS rawContactId concerning this newly created contact
     */
    public long createRcsContact(final ContactInfo info, final long rawContactId) {
        // If phone number can't be loosely compared with itself then we don't
        // make the phone number RCS.
        if (!phoneNumbersEqual(info.getContact(), info.getContact(), false)) {
        	if (logger.isActivated()){
        		logger.debug("RCS contact could not be created loose comparison failed");
        	}
            return INVALID_ID;
        }

        if (logger.isActivated()){
        	logger.debug("Creating new RCS rawcontact for "+info.getContact()+" to be associated to rawContactId "+rawContactId);
        }
        
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

        //Create rawcontact for RCS
        int rawContactRefIms = ops.size();
        ops.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
        		 .withValue(RawContacts.AGGREGATION_MODE, RawContacts.AGGREGATION_MODE_SUSPENDED)
                 .withValue(RawContacts.ACCOUNT_TYPE, AuthenticationService.ACCOUNT_MANAGER_TYPE)
                 .withValue(RawContacts.ACCOUNT_NAME, ctx.getString(R.string.rcs_core_account_username))
                 .build());

        // Insert number
        ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                 .withValueBackReference(Data.RAW_CONTACT_ID, rawContactRefIms)
                 .withValue(Data.MIMETYPE, MIMETYPE_NUMBER)
                 .withValue(Data.DATA1, info.getContact())
                 .build());
        
        // Create RCS status row
        ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, rawContactRefIms)
                .withValue(Data.MIMETYPE, ContactsManager.MIMETYPE_RCS_STATUS)
                .withValue(Data.DATA1, info.getContact())
                .withValue(Data.DATA2, info.getRcsStatus())
                .build());

        // Create RCS status timestamp row
        ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, rawContactRefIms)
                .withValue(Data.MIMETYPE, ContactsManager.MIMETYPE_RCS_STATUS_TIMESTAMP)
                .withValue(Data.DATA1, info.getContact())
                .withValue(Data.DATA2, System.currentTimeMillis())
                .build());
        
        // Insert presence timestamp
        ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, rawContactRefIms)
                .withValue(Data.MIMETYPE, MIMETYPE_PRESENCE_TIMESTAMP)
                .withValue(Data.DATA1, info.getContact())
                .withValue(Data.DATA2, System.currentTimeMillis())
                .build());
        
        if (info.getPresenceInfo()!=null) {
            // Insert presence free text
        	ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
        			.withValueBackReference(Data.RAW_CONTACT_ID, rawContactRefIms)
        			.withValue(Data.MIMETYPE, MIMETYPE_FREE_TEXT)
        			.withValue(Data.DATA1, info.getContact())
        			.withValue(Data.DATA2, info.getPresenceInfo().getFreetext())
        			.build());

        	// Insert presence status
        	ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
        			.withValueBackReference(Data.RAW_CONTACT_ID, rawContactRefIms)
        			.withValue(Data.MIMETYPE, MIMETYPE_PRESENCE_STATUS)
        			.withValue(Data.DATA1, info.getContact())
        			.withValue(Data.DATA2, info.getPresenceInfo().getPresenceStatus())
        			.build());
        	
            // Insert presence web link
    		//Add the weblink to the native @book
			ContentValues values = new ContentValues();
			values.put(Data.RAW_CONTACT_ID, rawContactId);
			values.put(Data.MIMETYPE, MIMETYPE_WEBLINK);
			values.put(Website.URL, info.getPresenceInfo().getFavoriteLinkUrl());
			values.put(Website.TYPE, Website.TYPE_HOMEPAGE);
			values.put(Data.IS_PRIMARY, 1);
			values.put(Data.IS_SUPER_PRIMARY, 1);

			ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
					.withValues(values)
					.build());
        } else {
        	// No presence info
            // Insert presence free text
        	ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
        			.withValueBackReference(Data.RAW_CONTACT_ID, rawContactRefIms)
        			.withValue(Data.MIMETYPE, MIMETYPE_FREE_TEXT)
        			.withValue(Data.DATA1, info.getContact())
        			.withValue(Data.DATA2, "")
        			.build());

        	// Insert presence status
        	ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
        			.withValueBackReference(Data.RAW_CONTACT_ID, rawContactRefIms)
        			.withValue(Data.MIMETYPE, MIMETYPE_PRESENCE_STATUS)
        			.withValue(Data.DATA1, info.getContact())
        			.withValue(Data.DATA2, PRESENCE_STATUS_NOT_SET)
        			.build());
        }

        // Insert capabilities timestamp
        ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, rawContactRefIms)
                .withValue(Data.MIMETYPE, MIMETYPE_CAPABILITY_TIMESTAMP)
                .withValue(Data.DATA1, info.getContact())
                .withValue(Data.DATA2, System.currentTimeMillis())
                .build());
        
        // Insert capabilities if present
        Capabilities capabilities = info.getCapabilities();
        // Cs Video
        if (capabilities.isCsVideoSupported()) {
            ops.add(createMimeTypeForContact(rawContactRefIms, info.getContact(), MIMETYPE_CAPABILITY_CS_VIDEO));
        }
        // File transfer
        if (capabilities.isFileTransferSupported()) {
            ops.add(createMimeTypeForContact(rawContactRefIms, info.getContact(), MIMETYPE_CAPABILITY_FILE_TRANSFER));
        }
        // Image sharing
        if (capabilities.isImageSharingSupported()) {
            ops.add(createMimeTypeForContact(rawContactRefIms, info.getContact(), MIMETYPE_CAPABILITY_IMAGE_SHARING));
        }
        // IM session
        if (capabilities.isImSessionSupported()) {
            ops.add(createMimeTypeForContact(rawContactRefIms, info.getContact(), MIMETYPE_CAPABILITY_IM_SESSION));
        }
        // Video sharing
        if (capabilities.isVideoSharingSupported()) {
            ops.add(createMimeTypeForContact(rawContactRefIms, info.getContact(), MIMETYPE_CAPABILITY_VIDEO_SHARING));
        }
        // Presence discovery
        if (capabilities.isPresenceDiscoverySupported()) {
            ops.add(createMimeTypeForContact(rawContactRefIms, info.getContact(), MIMETYPE_CAPABILITY_PRESENCE_DISCOVERY));
        }
        // Social presence
        if (capabilities.isSocialPresenceSupported()) {
            ops.add(createMimeTypeForContact(rawContactRefIms, info.getContact(), MIMETYPE_CAPABILITY_SOCIAL_PRESENCE));
        }
        // Geolocation push
        if (capabilities.isGeolocationPushSupported()) {
            ops.add(createMimeTypeForContact(rawContactRefIms, info.getContact(), MIMETYPE_CAPABILITY_GEOLOCATION_PUSH));
        }
        // File transfer thumbnail
        if (capabilities.isFileTransferThumbnailSupported()) {
            ops.add(createMimeTypeForContact(rawContactRefIms, info.getContact(), MIMETYPE_CAPABILITY_FILE_TRANSFER_THUMBNAIL));
        }
        // File transfer HTTP
        if (capabilities.isFileTransferHttpSupported()) {
            ops.add(createMimeTypeForContact(rawContactRefIms, info.getContact(), MIMETYPE_CAPABILITY_FILE_TRANSFER_HTTP));
        }
        // Insert extensions
		boolean hasCommonExtensions = false;
		StringBuffer extension = new StringBuffer();
        ArrayList<String> newExtensions = info.getCapabilities().getSupportedExtensions();
        String exts = RcsSettings.getInstance().getSupportedRcsExtensions();
        for (int j = 0; j < newExtensions.size(); j++) {
            extension.append(newExtensions.get(j) +";");
	        // Check if we support at least one of the extensions this contact has
	        // Get my extensions
			if ((exts != null) && (exts.length() > 0)) {
				String[] ext = exts.split(",");
				for(int i=0; i < ext.length; i++) {
					String capability = ext[i];
					if (newExtensions.contains(capability)){
						hasCommonExtensions = true;
					}
				}
			}
		}
		ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawContactRefIms)
				.withValue(Data.MIMETYPE, MIMETYPE_CAPABILITY_EXTENSIONS)
				.withValue(Data.DATA1, info.getContact())
				.withValue(Data.DATA2, extension.toString())
				.build());
		if (hasCommonExtensions) {
			// Insert common extensions item
            ops.add(createMimeTypeForContact(rawContactRefIms, info.getContact(), MIMETYPE_CAPABILITY_COMMON_EXTENSION));
		}

    	// Insert registration status
    	ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
    			.withValueBackReference(Data.RAW_CONTACT_ID, rawContactRefIms)
    			.withValue(Data.MIMETYPE, MIMETYPE_REGISTRATION_STATE)
    			.withValue(Data.DATA1, info.getContact())
    			.withValue(Data.DATA2, info.getRegistrationState())
    			.build());

        // Insert contact type, it is either RCS active, RCS capable, not RCS or we have no info on it
        if (info.getRcsStatus()==ContactInfo.RCS_ACTIVE) {
            ops.add(createMimeTypeForContact(rawContactRefIms, info.getContact(), MIMETYPE_RCS_CONTACT));

            // Insert avatar, only if status is "active"
            // (we do not want a default RCS picture if we do not share our presence profile yet)
    		Bitmap rcsAvatar = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.rcs_core_default_portrait_icon);
    		byte[] iconData = convertBitmapToBytes(rcsAvatar);
        	if (info.getPresenceInfo()!=null && info.getPresenceInfo().getPhotoIcon()!=null){
        		iconData = info.getPresenceInfo().getPhotoIcon().getContent();
        	}
        	ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
        			.withValueBackReference(Data.RAW_CONTACT_ID, rawContactRefIms)
        			.withValue(Data.MIMETYPE, ContactsManager.MIMETYPE_PHOTO)
        			.withValue(Photo.PHOTO, iconData)
        			.withValue(Data.IS_PRIMARY, 1)
        			.build());
        } else if (info.getRcsStatus()==ContactInfo.NOT_RCS) {
            ops.add(createMimeTypeForContact(rawContactRefIms, info.getContact(), MIMETYPE_NOT_RCS_CONTACT));
        } else if (info.getRcsStatus()!=ContactInfo.NO_INFO) {
            // In all other cases, contact is RCS capable
            ops.add(createMimeTypeForContact(rawContactRefIms, info.getContact(), MIMETYPE_RCS_CAPABLE_CONTACT));
        }
        
        // Create the RCS raw contact and get its id        
        long rcsRawContactId = INVALID_ID;
        try {
        	ContentProviderResult[] results;
        	results = ctx.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        	rcsRawContactId =  ContentUris.parseId(results[rawContactRefIms].uri);
        } catch (RemoteException e) {
        } catch (OperationApplicationException e) {
        	return INVALID_ID;
        }

        // Aggregate the newly RCS raw contact and the raw contact that has the phone number
        ops.clear();
        ops.add(ContentProviderOperation.newUpdate(ContactsContract.AggregationExceptions.CONTENT_URI)
        		.withValue(AggregationExceptions.TYPE, AggregationExceptions.TYPE_KEEP_TOGETHER)
        		.withValue(AggregationExceptions.RAW_CONTACT_ID1, rcsRawContactId)
        		.withValue(AggregationExceptions.RAW_CONTACT_ID2, rawContactId).build());

        try {
        	ctx.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        	// Add to exception provider
        	ContentValues values = new ContentValues();
        	values.put(AggregationData.KEY_RAW_CONTACT_ID, rawContactId);
        	values.put(AggregationData.KEY_RCS_RAW_CONTACT_ID, rcsRawContactId);
        	values.put(AggregationData.KEY_RCS_NUMBER, info.getContact());
			ctx.getContentResolver().insert(AggregationData.CONTENT_URI, values);
        } catch (RemoteException e) {
        	if (logger.isActivated()){
        		logger.debug("Remote exception => "+e);
        	}
        	return INVALID_ID;
        } catch (OperationApplicationException e) {
        	if (logger.isActivated()){
        		logger.debug("Operation exception => "+e);
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
        byte[] iconData = null;
        int size = bitmap.getRowBytes() * bitmap.getHeight();

        ByteArrayOutputStream out = new ByteArrayOutputStream(size);
        try {
            if (bitmap.compress(Bitmap.CompressFormat.PNG, 0 /* quality ignored for PNG */, out)) {
                out.close();
                iconData = out.toByteArray();
            } else {
                out.close();
                if (logger.isActivated()){
                	logger.debug("Unable to convert bitmap, compression failed");
                }
            }
        } catch (IOException e) {
        	if (logger.isActivated()){
        		logger.error("Unable to convert bitmap", e);
        	}
            iconData = null;
        }

        return iconData;
    }
    
    /**
     * Utility method to create the "Me" raw contact.
     *
     * @param context The application context.
     * @return the rawContactId of the newly created contact
     */
    public long createMyContact() {
    	RcsSettings.createInstance(ctx);
		if (!RcsSettings.getInstance().isSocialPresenceSupported()){
			return INVALID_ID;
		}

        // Check if IMS account exists before continue
        AccountManager am = AccountManager.get(ctx);
        if (am.getAccountsByType(AuthenticationService.ACCOUNT_MANAGER_TYPE).length == 0) {
        	if (logger.isActivated()){
        		logger.error("Could not create \"Me\" contact, no RCS account found");
        	}
            throw new IllegalStateException("No RCS account found");
        }

        // Check if RCS raw contact for "Me" does not already exist
        long imsRawContactId = getRawContactIdForMe();
        
        if (imsRawContactId != INVALID_ID) {
        	if (logger.isActivated()){
        		logger.error("\"Me\" contact already exists, no need to recreate");
        	}
        }else{
        	if (logger.isActivated()){
        		logger.error("\"Me\" contact does not already exists, creating it");
        	}
        	
            ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

            //Create rawcontact for RCS
            int rawContactRefIms = ops.size();
            ops.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                     .withValue(RawContacts.ACCOUNT_TYPE, AuthenticationService.ACCOUNT_MANAGER_TYPE)
                     .withValue(RawContacts.ACCOUNT_NAME, ctx.getString(R.string.rcs_core_account_username))
                     .withValue(RawContacts.SOURCE_ID, MYSELF)                     
                     .withValue(RawContacts.AGGREGATION_MODE, RawContacts.AGGREGATION_MODE_DISABLED)
                     .build());

            // Set name
            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, rawContactRefIms)
                    .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(StructuredName.DISPLAY_NAME, ctx.getString(R.string.rcs_core_my_profile))
                    .build());
            
            // Create RCS status row
            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, rawContactRefIms)
                    .withValue(Data.MIMETYPE, ContactsManager.MIMETYPE_RCS_STATUS)
                    .withValue(Data.DATA1, MYSELF)
                    .withValue(Data.DATA2, ContactInfo.RCS_CAPABLE)
                    .build());
            
            // Create RCS status timestamp row
            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, rawContactRefIms)
                    .withValue(Data.MIMETYPE, ContactsManager.MIMETYPE_RCS_STATUS_TIMESTAMP)
                    .withValue(Data.DATA1, MYSELF)
                    .withValue(Data.DATA2, System.currentTimeMillis())
                    .build());
            
            // Create my profile shortcut
            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, rawContactRefIms)
                    .withValue(Data.MIMETYPE, ContactsManager.MIMETYPE_SEE_MY_PROFILE)
                    .withValue(Data.DATA1, MYSELF)
                    .build());
            
            // Insert presence timestamp
            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, rawContactRefIms)
                    .withValue(Data.MIMETYPE, MIMETYPE_PRESENCE_TIMESTAMP)
                    .withValue(Data.DATA1, MYSELF)
                    .withValue(Data.DATA2, System.currentTimeMillis())
                    .build());
            
            // Insert presence free text
            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, rawContactRefIms)
                    .withValue(Data.MIMETYPE, MIMETYPE_FREE_TEXT)
                    .withValue(Data.DATA1, MYSELF)
                    .withValue(Data.DATA2, "")
                    .build());
            
            // Insert presence status
            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, rawContactRefIms)
                    .withValue(Data.MIMETYPE, MIMETYPE_PRESENCE_STATUS)
                    .withValue(Data.DATA1, MYSELF)
                    .withValue(Data.DATA2, PRESENCE_STATUS_NOT_SET)
                    .build());
            
            // Insert capabilities timestamp
            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, rawContactRefIms)
                    .withValue(Data.MIMETYPE, MIMETYPE_CAPABILITY_TIMESTAMP)
                    .withValue(Data.DATA1, MYSELF)
                    .withValue(Data.DATA2, System.currentTimeMillis())
                    .build());
            
            // Insert default avatar
            Bitmap rcsAvatar = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.rcs_core_default_portrait_icon);
            byte[] iconData = convertBitmapToBytes(rcsAvatar);
            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, rawContactRefIms)
                    .withValue(Data.MIMETYPE, ContactsManager.MIMETYPE_PHOTO)
                    .withValue(Photo.PHOTO, iconData)
                    .withValue(Data.IS_PRIMARY, 1)
                    .build());
            
            try {
                ContentProviderResult[] results;
                results = ctx.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
                imsRawContactId = ContentUris.parseId(results[rawContactRefIms].uri);
            } catch (RemoteException e) {
            	imsRawContactId = INVALID_ID;
            } catch (OperationApplicationException e) {
            	imsRawContactId =  INVALID_ID;
            }
            
            ops.clear();
            
            // Set default free text to null and availability to online
    		ArrayList<ContentProviderOperation> registrationStateOps = modifyContactRegistrationState(imsRawContactId, MYSELF, ContactInfo.REGISTRATION_STATUS_ONLINE, -1, "", "");
    		for (int i=0;i<registrationStateOps.size();i++){
    			ContentProviderOperation op = registrationStateOps.get(i);
    			if (op!=null){
    				ops.add(op);
    			}
    		}
    		
            try {
                ctx.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            } catch (RemoteException e) {
            	imsRawContactId = INVALID_ID;
            } catch (OperationApplicationException e) {
            	imsRawContactId =  INVALID_ID;
            }
        }

        return imsRawContactId;
    }

    /**
     * Utility to find the RCS rawContactIds for a specific phone number.
     *
     * @param phoneNumber the phoneNumber to search for
     * @return list of contactIds, empty list if none was found
     */
    private List<Long> getRcsRawContactIdFromPhoneNumber(String phoneNumber) {
        List<Long> contactsIds = new ArrayList<Long>();
        String[] projection = {
                Data.RAW_CONTACT_ID,
                Data.DATA1
                };
        String selection = Data.MIMETYPE + "=? AND PHONE_NUMBERS_EQUAL(" + Phone.NUMBER + ", ?)";
        String[] selectionArgs = { MIMETYPE_NUMBER, phoneNumber };
        String sortOrder = Data.RAW_CONTACT_ID;

        Cursor cur = ctx.getContentResolver().query(Data.CONTENT_URI, 
        		projection, 
        		selection, 
        		selectionArgs,
                sortOrder);
        if (cur != null) {
            while (cur.moveToNext()) {
                long rcsRawContactId = cur.getLong(cur.getColumnIndex(Data.RAW_CONTACT_ID));
                String phone = cur.getString(cur.getColumnIndex(Data.DATA1));
                String interPhone = PhoneUtils.formatNumberToInternational(phone);
                if (phoneNumber.equals(interPhone)) {
                    contactsIds.add(rcsRawContactId);
                    break;
                }
            }
            cur.close();
        }

        return contactsIds;
    }

    /**
     * Utility to find the rawContactIds for a specific phone number.
     *
     * @param phoneNumber the phoneNumber to search for
     * @return list of contactIds
     */
    private List<Long> getRawContactIdsFromPhoneNumber(String phoneNumber) {
        List<Long> rawContactsIds = new ArrayList<Long>(); 
    	String[] projection = { Data.RAW_CONTACT_ID };
        String selection = Data.MIMETYPE + "=? AND PHONE_NUMBERS_EQUAL(" + Phone.NUMBER + ", ?)";
        String[] selectionArgs = { Phone.CONTENT_ITEM_TYPE, phoneNumber };
        String sortOrder = Data.RAW_CONTACT_ID;

        // Starting LOOSE equal
        Cursor cur = ctx.getContentResolver().query(Data.CONTENT_URI, 
        		projection, 
        		selection, 
        		selectionArgs,
                sortOrder);
        if (cur != null) {
            while (cur.moveToNext()) {
                long rawContactId = cur.getLong(cur.getColumnIndex(Data.RAW_CONTACT_ID));
                if (!rawContactsIds.contains(rawContactId) 
                        && (!isSimAssociated(rawContactId) || (Build.VERSION.SDK_INT > 10))) { //Build.VERSION_CODES.GINGERBREAD_MR1
                    // We exclude the SIM only contacts, as they cannot be aggregated to a RCS raw contact
                    // only if OS version if gingebread or fewer
                    rawContactsIds.add(rawContactId);
                }
            }
            cur.close();
        }

        /* No match found using LOOSE equals, starting STRICT equals.
         *
         * This is done because of that the PHONE_NUMBERS_EQUAL function in Android
         * dosent always return true when doing loose lookup of a phone number
         * against itself
         */
        String selectionStrict = Data.MIMETYPE + "=? AND (NOT PHONE_NUMBERS_EQUAL(" + Phone.NUMBER
                + ", ?) AND PHONE_NUMBERS_EQUAL(" + Phone.NUMBER + ", ?, 1))";
        String[] selectionArgsStrict = { Phone.CONTENT_ITEM_TYPE, phoneNumber, phoneNumber };
        cur = ctx.getContentResolver().query(Data.CONTENT_URI, 
        		projection, 
        		selectionStrict, 
        		selectionArgsStrict,
                sortOrder);
        if (cur != null) {
            while (cur.moveToNext()) {
                long rawContactId = cur.getLong(cur.getColumnIndex(Data.RAW_CONTACT_ID));
                if (!rawContactsIds.contains(rawContactId) 
                        && (!isSimAssociated(rawContactId) || (Build.VERSION.SDK_INT > 10))) { //Build.VERSION_CODES.GINGERBREAD_MR1
                    // We exclude the SIM only contacts, as they cannot be aggregated to a RCS raw contact
                    // only if OS version if gingebread or fewer
                    rawContactsIds.add(rawContactId);
                }
            }
            cur.close();
        }
        
        return rawContactsIds;
    }
    
    /**
     * Utility to get the RCS rawContact associated to a raw contact
     *
     * @param rawContactId the id of the rawContact
     * @param rcsNumber The RCS number
     * @return the id of the associated RCS rawContact
     */
    public long getAssociatedRcsRawContact(final long rawContactId, final String rcsNumber) {
    	long result = INVALID_ID;
    	Cursor cursor = ctx.getContentResolver().query(AggregationData.CONTENT_URI, 
				new String[]{AggregationData.KEY_RCS_RAW_CONTACT_ID, AggregationData.KEY_RCS_NUMBER, AggregationData.KEY_RAW_CONTACT_ID}, 
				AggregationData.KEY_RCS_NUMBER + "=?" + " AND "+ AggregationData.KEY_RAW_CONTACT_ID + "=?", 
				new String[]{rcsNumber, String.valueOf(rawContactId)},
				null);
		if (cursor.moveToFirst()){
			result = cursor.getLong(0);
		}
		cursor.close();
    	return result;
    }
    
    /**
     * Utility to check if a phone number is associated to an entry in the rich address book provider
     *
     * @param phoneNumber The phone number associated to the RCS contact
     * @return true if contact has an entry in the rich address book provider, else false
     */
    public boolean isRcsAssociated(final String phoneNumber) {
        boolean result = false;
        Cursor cur = ctx.getContentResolver().query(RichAddressBookData.CONTENT_URI, 
                new String[]{RichAddressBookData.KEY_CONTACT_NUMBER}, 
                RichAddressBookData.KEY_CONTACT_NUMBER + "=?", 
                new String[]{phoneNumber}, 
                null);
        if (cur!=null){
            if (cur.moveToFirst()) {
                result = true;
            }
            cur.close();
        }else{
            result = false;
        }
        return result;
    }

    /**
     * Utility method to check if a raw contact is only associated to a SIM account
     *
     * @param rawContactId The id associated to the SIM account
     * @return true if the raw contact is only associated to a SIM account, else false
     */
    public boolean isOnlySimAssociated(final String phoneNumber) {
    	List<Long> rawContactIds = getRawContactIdsFromPhoneNumber(phoneNumber);
    	for (int i=0;i<rawContactIds.size();i++){
    		Cursor rawCur = ctx.getContentResolver().query(RawContacts.CONTENT_URI, 
    				new String[]{RawContacts._ID}, 
    				"("+RawContacts.ACCOUNT_TYPE + " IS NULL OR "+RawContacts.ACCOUNT_TYPE +" <> \'"+SIM_ACCOUNT_NAME+"\') AND " + RawContacts._ID + "= "+ Long.toString(rawContactIds.get(i)),
    				null, 
    				null);
    		if (rawCur != null){ 
    			if (rawCur.getCount() > 0) {
        			rawCur.close();
    				return false;
    			}
    			rawCur.close();
    		}
    	}
        return true;
    }
    
    /**
     * Utility method to check if a raw contact id is a SIM account
     * 
     * @param rawContactId
     * @return
     */
    public boolean isSimAssociated(final long rawContactId){
    	boolean result = false;
    	Cursor rawCur = ctx.getContentResolver().query(RawContacts.CONTENT_URI, 
				new String[]{RawContacts._ID}, 
				RawContacts.ACCOUNT_TYPE + "= \'"+SIM_ACCOUNT_NAME+"\' AND " + RawContacts._ID + "= "+ Long.toString(rawContactId),
				null, 
				null);
		if (rawCur != null){ 
			if (rawCur.getCount() > 0) {
				result=true;
			}
			rawCur.close();
		}
		return result;
    }
    
    /**
     * Utility method to check if a raw contact id is a SIM account
     * 
     * @param rawContactId
     * @return
     */
    public boolean isSimAccount(final long rawContactId){
    	boolean result = false;
    	Cursor rawCur = ctx.getContentResolver().query(RawContacts.CONTENT_URI, 
				new String[]{RawContacts._ID}, 
				RawContacts.ACCOUNT_TYPE + "= \'"+SIM_ACCOUNT_NAME+"\' AND " + RawContacts._ID + "= "+ Long.toString(rawContactId),
				null, 
				null);
		if (rawCur != null){ 
			if (rawCur.getCount() > 0) {
				result=true;
			}
			rawCur.close();
		}
		return result;
    }
    
    /**
     * Utility to get access to Android's PHONE_NUMBERS_EQUAL SQL function.
     *
     * @note Impl and comments can be found in
     *       /external/sqlite/android/PhoneNumberUtils.cpp
     *       (phone_number_compare_inter)
     *
     * @param phone1 the first phone number
     * @param phone2 the second phone number
     * @param useStrictComparison set to false if loose comparison should be
     *            used (normal), true if strict comparison should be used
     * @return true when equal
     */
    private boolean phoneNumbersEqual(final String phone1, final String phone2, final boolean useStrictComparison) {
        boolean result = false;
        // Create a temporary db in memory to get access to the SQL engine
        SQLiteDatabase db = SQLiteDatabase.create(null);
        if (db == null) {
            throw new IllegalStateException("Could not retrieve db");
        }
        // CSOFF: InlineConditionals
        String test = "SELECT CASE WHEN PHONE_NUMBERS_EQUAL(" + phone1 + "," + phone2 + ","
                + Integer.toString((useStrictComparison) ? 1 : 0) + ") " + "THEN 1 ELSE 0 END";
        // CSON: InlineConditionals
        Cursor cur = db.rawQuery(test, null);
        if (cur != null){
        	if (cur.moveToNext()) {
	            if (cur.getString(0).equals("1")) {
	                result = true;
	            } else {
	                result = false;
	            }
        	}
            cur.close();
        }
        db.close();
        return result;
    }

    /**
     * Get the data id associated to a given mimeType for a contact.
     *
     * @param rawContactId the RCS rawcontact
     * @param mimeType The searched mimetype 
     * @return The id of the data
     */
    private long getDataIdForRawContact(final long rawContactId, final String mimeType) {

    	long dataId = INVALID_ID;
        String[] projection = {Data._ID, Data.RAW_CONTACT_ID, Data.MIMETYPE };

        String selection = Data.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "=?";
        String[] selectionArgs = { Long.toString(rawContactId), mimeType };
        Cursor cur = ctx.getContentResolver().query(Data.CONTENT_URI, 
        		projection, 
        		selection, 
        		selectionArgs, 
        		null);
        if (cur == null) {
        	return INVALID_ID;
        }
        if (cur.moveToNext()) {
        	dataId = cur.getLong(0);
        }
        cur.close();
        
        return dataId;
    }

    /**
     * Get the rcs raw contact ids for the given contact
     * 
     * @param contact The contact
     * @return list of raw contact ids associated to this contact
     */
    private List<Long> getRcsRawContactIdsFromContact(final String contact){
    	if (MYSELF.equalsIgnoreCase(contact)){
    		List<Long> rawContactId = new ArrayList<Long>();
    		rawContactId.add(getRawContactIdForMe());
    		return rawContactId;
    	}else{
    		return getRcsRawContactIdFromPhoneNumber(contact);
    	}
    }
    
    /**
     * Utility to set the photo icon attribute on a RCS contact.
     *
     * @param rawContactId RCS rawcontact
     * @param photoIcon The photoIcon
     * @param makeSuperPrimary whether or not to set the super primary flag
     * @return 
     */
    private List<ContentProviderOperation> setContactPhoto(Long rawContactId, PhotoIcon photoIcon, boolean makeSuperPrimary) {

    	List<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
    	
    	// Get the photo data id
    	String[] projection = { Data._ID };
    	String selection = Data.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "=?";
    	String[] selectionArgs = { Long.toString(rawContactId), Photo.CONTENT_ITEM_TYPE };
    	String sortOrder = Data._ID + " DESC";

    	Cursor cur = ctx.getContentResolver().query(Data.CONTENT_URI, 
    			projection, 
    			selection,
    			selectionArgs, 
    			sortOrder);
    	if (cur == null) {
    		return ops;
    	}

    	byte[] iconData = null;
    	if (photoIcon!=null){
    		iconData = photoIcon.getContent();	
    	}         

    	// Insert default avatar if icon is null and it is not for myself
    	if (iconData == null
    			&& rawContactId != getRawContactIdForMe()) {
    		Bitmap rcsAvatar = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.rcs_core_default_portrait_icon);
    		iconData = convertBitmapToBytes(rcsAvatar);
    	}

    	try {
    		long dataId = INVALID_ID;
    		if (iconData == null) {
    			// May happen only for myself
    			// Remove photoIcon if no data
    			if (cur.moveToNext()) {
    				dataId = cur.getLong(cur.getColumnIndex(Data._ID));
    				// Add delete operation
    				ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI)
	        			.withSelection(Data._ID+"=?", new String[]{String.valueOf(dataId)})
	        			.build());
    			}
    		} else {
    			ContentValues values = new ContentValues();
    			values.put(Data.RAW_CONTACT_ID, rawContactId);
    			values.put(Data.MIMETYPE, MIMETYPE_PHOTO);
    			values.put(Photo.PHOTO, iconData);
    			values.put(Data.IS_PRIMARY, 1);
    			if (makeSuperPrimary) {
    				values.put(Data.IS_SUPER_PRIMARY, 1);
    			}
    			if (cur.moveToNext()) {
    				// We already had an icon, update it
    				dataId = cur.getLong(cur.getColumnIndex(Data._ID));
    				ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
    	        			.withSelection(Data._ID+"=?", new String[]{String.valueOf(dataId)})
    	        			.withValues(values)
    	        			.build());
    			} else {
    				// We did not have an icon, insert a new one
    				ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
    	        			.withValues(values)
    	        			.build());
    			}

    			values.clear();

    			// Set etag
    			values.put(Data.RAW_CONTACT_ID, rawContactId);
    			values.put(Data.MIMETYPE, MIMETYPE_PHOTO_ETAG);
    			String etag = null;
    			if (photoIcon!=null){
    				etag = photoIcon.getEtag();
    			}
    			values.put(Data.DATA2, etag);

    			String[] projection2 = { Data._ID, Data.RAW_CONTACT_ID, Data.MIMETYPE };
    			String selection2 = Data.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "=?";
    			String[] selectionArgs2 = { Long.toString(rawContactId), MIMETYPE_PHOTO_ETAG };

    			Cursor cur2 = ctx.getContentResolver().query(Data.CONTENT_URI, 
    					projection2, 
    					selection2,
    					selectionArgs2, 
    					null);
    			if (cur2.moveToNext()){
    				dataId = cur2.getLong(0);
    				// We already had an etag, update it
    				dataId = cur.getLong(cur.getColumnIndex(Data._ID));
    				ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
    	        			.withSelection(Data._ID+"=?", new String[]{String.valueOf(dataId)})
    	        			.withValues(values)
    	        			.build());
    			}else{
    				// Insert etag
    				ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
    	        			.withValues(values)
    	        			.build());
    			}              
    			cur2.close();
    		}
    	} finally {
    		cur.close();
    	}
    	return ops;
    }

    /**
     * Utility to get the etag of a contact icon.
     *
     * @param contact
     * @return the icon etag 
     */
    public String getContactPhotoEtag(String contact) {
        String etag = null;
		
        contact = PhoneUtils.extractNumberFromUri(contact);
		
        List<Long> rawContactIds = getRcsRawContactIdsFromContact(contact);
        if (rawContactIds.isEmpty()){
        	return null;
        }
		// The data in all the rcs raw contacts is the same, so just take the first one
        long rawContactId = rawContactIds.get(0);
        
        String[] projection = { Data.DATA2 };
        
        String selection = Data.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "=?";
        String[] selectionArgs = { Long.toString(rawContactId), MIMETYPE_PHOTO_ETAG };

        Cursor cur = ctx.getContentResolver().query(Data.CONTENT_URI, 
        		projection, 
        		selection,
                selectionArgs, 
                null);
         if (cur.moveToNext()){
        	 etag = cur.getString(0);
        }
        cur.close();
        
        return etag;
    }
    
    /**
     * Get the raw contact id of the "Me" contact.
     *
     * @return rawContactId
     */
    private long getRawContactIdForMe() {
        String[] projection = {
                RawContacts.ACCOUNT_TYPE, 
                RawContacts._ID,
                RawContacts.SOURCE_ID
        };
        String selection = RawContacts.ACCOUNT_TYPE + "=? AND " 
        		+ RawContacts.SOURCE_ID + "=?";
        String[] selectionArgs = {
        		AuthenticationService.ACCOUNT_MANAGER_TYPE,
                MYSELF
        };

        Cursor cur = ctx.getContentResolver().query(RawContacts.CONTENT_URI, 
        		projection, 
        		selection, 
        		selectionArgs,
                null);
        if (cur == null) {
            return INVALID_ID;
        }
        if (!cur.moveToNext()) {
            cur.close();
            return INVALID_ID;
        }

        long rawContactId = cur.getLong(1);
        cur.close();
        return rawContactId;
    }
    
    /**
     * Mark the contact as "blocked for IM"
     * 
     * @param contact
     * @param flag indicating if we enable or disable the IM sessions with the contact
     */
    public void setImBlockedForContact(String contact, boolean flag){
		// May be called from outside the core, so be sure the number format is international before doing the queries
    	contact = PhoneUtils.extractNumberFromUri(contact);

		// Update the database		
		// Get all the Ids from raw contacts that have this phone number
		List<Long> rawContactIds = getRawContactIdsFromPhoneNumber(contact);

		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		// For each, prepare the modifications
		for (int i=0;i<rawContactIds.size();i++){
			long rawContactId = rawContactIds.get(i);
			
			// Get the associated RCS raw contact id
			long rcsRawContactId = getAssociatedRcsRawContact(rawContactId, contact);
			
			if (rcsRawContactId==INVALID_ID){
				// If no RCS raw contact id is associated to the raw contact, create a new one
				ContactInfo newInfo = new ContactInfo();
				newInfo.setContact(contact);
				rcsRawContactId = createRcsContact(newInfo, rawContactId);
			}
			// Get the row id of this capability for this raw contact
			long dataId = getDataIdForRawContact(rawContactId, MIMETYPE_IM_BLOCKED);
	        
	        if (dataId == INVALID_ID) {
	        	// The capability is not present for now
	        	if (flag){
	        		// We have to add it
	        		ops.add(insertMimeTypeForContact(rawContactId, contact, MIMETYPE_IM_BLOCKED));
	        	}
	        }else{
	        	// The capability is present
	        	if (!flag){
	        		// We have to remove it
	        		ops.add(deleteMimeTypeForContact(rawContactId, contact, MIMETYPE_IM_BLOCKED));
	        	}
	        }
		}
		
		if (!ops.isEmpty()){
			// Do the actual database modifications
			try {
				ctx.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
			} catch (RemoteException e) {
				if (logger.isActivated()){
					logger.error("Something went wrong when updating the database with the contact info",e);
				}
			} catch (OperationApplicationException e) {
				if (logger.isActivated()){
					logger.error("Something went wrong when updating the database with the contact info",e);
				}
			}
		}
    }
    
    /**
     * Get whether the "IM" feature is enabled or not for the contact
     * 
     * @param contact
     * @return flag indicating if IM sessions with the contact are enabled or not
     */
    public boolean isImBlockedForContact(String contact){
		// May be called from outside the core, so be sure the number format is international before doing the queries
    	contact = PhoneUtils.extractNumberFromUri(contact);
    	
		String[] projection = {Data.DATA1, Data.MIMETYPE};

        String selection = Data.MIMETYPE + "=?" + " AND " + Data.DATA1+ "=?";
        String[] selectionArgs = { MIMETYPE_IM_BLOCKED , contact};
        Cursor c = ctx.getContentResolver().query(Data.CONTENT_URI, 
        		projection, 
        		selection, 
        		selectionArgs, 
        		null);
		if (c.getCount()>0){
			c.close();
			return true;
		}
		c.close();
    	return false;
    }
    
	/**
	 * Get the contacts that are "IM blocked"
	 * 
	 * @return list containing all contacts that are "IM blocked" 
	 */
	public List<String> getImBlockedContacts(){
		List<String> imBlockedNumbers = new ArrayList<String>();
        String[] projection = {Data.DATA1, Data.MIMETYPE};

        String selection = Data.MIMETYPE + "=?";
        String[] selectionArgs = { MIMETYPE_IM_BLOCKED };
        Cursor c = ctx.getContentResolver().query(Data.CONTENT_URI, 
        		projection, 
        		selection, 
        		selectionArgs, 
        		null);
		
		while (c.moveToNext()) {
			imBlockedNumbers.add(c.getString(0));
		}
		c.close();
		return imBlockedNumbers;
	}
    
    /**
     * Mark the contact as "blocked for FT"
     * 
     * @param contact
     * @param flag indicating if we enable or disable the FT sessions with the contact
     */
    public void setFtBlockedForContact(String contact, boolean flag){
		// May be called from outside the core, so be sure the number format is international before doing the queries
    	contact = PhoneUtils.extractNumberFromUri(contact);

		// Update the database		
		// Get all the Ids from raw contacts that have this phone number
		List<Long> rawContactIds = getRawContactIdsFromPhoneNumber(contact);

		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		// For each, prepare the modifications
		for (int i=0;i<rawContactIds.size();i++){
			long rawContactId = rawContactIds.get(i);
			
			// Get the associated RCS raw contact id
			long rcsRawContactId = getAssociatedRcsRawContact(rawContactId, contact);
			
			if (rcsRawContactId==INVALID_ID){
				// If no RCS raw contact id is associated to the raw contact, create a new one
				ContactInfo newInfo = new ContactInfo();
				newInfo.setContact(contact);
				rcsRawContactId = createRcsContact(newInfo, rawContactId);
			}
			// Get the row id of this capability for this raw contact
			long dataId = getDataIdForRawContact(rawContactId, MIMETYPE_FT_BLOCKED);
	        
	        if (dataId == INVALID_ID) {
	        	// The capability is not present for now
	        	if (flag){
	        		// We have to add it
	        		ops.add(insertMimeTypeForContact(rawContactId, contact, MIMETYPE_FT_BLOCKED));
	        	}
	        }else{
	        	// The capability is present
	        	if (!flag){
	        		// We have to remove it
	        		ops.add(deleteMimeTypeForContact(rawContactId, contact, MIMETYPE_FT_BLOCKED));
	        	}
	        }
		}
		
		if (!ops.isEmpty()){
			// Do the actual database modifications
			try {
				ctx.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
			} catch (RemoteException e) {
				if (logger.isActivated()){
					logger.error("Something went wrong when updating the database with the contact info",e);
				}
			} catch (OperationApplicationException e) {
				if (logger.isActivated()){
					logger.error("Something went wrong when updating the database with the contact info",e);
				}
			}
		}
    }
    
    /**
     * Get whether the "FT" feature is enabled or not for the contact
     * 
     * @param contact
     * @return flag indicating if FT sessions with the contact are enabled or not
     */
    public boolean isFtBlockedForContact(String contact){
		// May be called from outside the core, so be sure the number format is international before doing the queries
    	contact = PhoneUtils.extractNumberFromUri(contact);
    	
		String[] projection = {Data.DATA1, Data.MIMETYPE};

        String selection = Data.MIMETYPE + "=?" + " AND " + Data.DATA1+ "=?";
        String[] selectionArgs = { MIMETYPE_FT_BLOCKED , contact};
        Cursor c = ctx.getContentResolver().query(Data.CONTENT_URI, 
        		projection, 
        		selection, 
        		selectionArgs, 
        		null);
		if (c.getCount()>0){
			c.close();
			return true;
		}
		c.close();
    	return false;
    }
    
	/**
	 * Get the contacts that are "FT blocked"
	 * 
	 * @return list containing all contacts that are "FT blocked" 
	 */
	public List<String> getFtBlockedContacts(){
		List<String> imBlockedNumbers = new ArrayList<String>();
        String[] projection = {Data.DATA1, Data.MIMETYPE};

        String selection = Data.MIMETYPE + "=?";
        String[] selectionArgs = { MIMETYPE_FT_BLOCKED };
        Cursor c = ctx.getContentResolver().query(Data.CONTENT_URI, 
        		projection, 
        		selection, 
        		selectionArgs, 
        		null);
		
		while (c.moveToNext()) {
			imBlockedNumbers.add(c.getString(0));
		}
		c.close();
		return imBlockedNumbers;
	}
	
    /**
     * Utility to create a ContactInfo object from a cursor containing data
     * 
     * @param cursor
     * @return contactInfo
     */
    private ContactInfo getContactInfoFromCursor(Cursor cursor){
    	ContactInfo contactInfo = new ContactInfo();
    	PresenceInfo presenceInfo = new PresenceInfo();
    	Capabilities capabilities = new Capabilities();
    	byte[] photoContent = null;
    	String photoEtag = null;
    	
    	while(cursor.moveToNext()){
    		String mimeType = cursor.getString(cursor.getColumnIndex(Data.MIMETYPE));
    		if (mimeType.equalsIgnoreCase(MIMETYPE_WEBLINK)){
    			// Set weblink
    			int columnIndex = cursor.getColumnIndex(Website.URL);
    			if (columnIndex!=-1){
    				presenceInfo.setFavoriteLinkUrl(cursor.getString(columnIndex));
    			}
    		}else if (mimeType.equalsIgnoreCase(MIMETYPE_PHOTO)){
    			// Set photo
    			int columnIndex = cursor.getColumnIndex(Photo.PHOTO);
    			if (columnIndex!=-1){
    				photoContent = cursor.getBlob(columnIndex);
    			}
    		}else if (mimeType.equalsIgnoreCase(MIMETYPE_PHOTO_ETAG)){
    			// Set photo etag
    			int columnIndex = cursor.getColumnIndex(Data.DATA2);
    			if (columnIndex!=-1){
    				photoEtag = cursor.getString(columnIndex);
    			}    			
    		}else if (mimeType.equalsIgnoreCase(MIMETYPE_PRESENCE_TIMESTAMP)){
    			// Set presence timestamp
    			int columnIndex = cursor.getColumnIndex(Data.DATA2);
    			if (columnIndex!=-1){
    				presenceInfo.setTimestamp(cursor.getLong(columnIndex));
    			}    			
    		}else if (mimeType.equalsIgnoreCase(MIMETYPE_CAPABILITY_TIMESTAMP)){
    			// Set capability timestamp
    			int columnIndex = cursor.getColumnIndex(Data.DATA2);
    			if (columnIndex!=-1){
    				capabilities.setTimestamp(cursor.getLong(columnIndex));
    			}    			
    		}else if (mimeType.equalsIgnoreCase(MIMETYPE_CAPABILITY_CS_VIDEO)){
    			// Set capability cs_video
   				capabilities.setCsVideoSupport(true);
    		}else if (mimeType.equalsIgnoreCase(MIMETYPE_CAPABILITY_IMAGE_SHARING)){
    			// Set capability image sharing
   				capabilities.setImageSharingSupport(true);
    		}else if (mimeType.equalsIgnoreCase(MIMETYPE_CAPABILITY_VIDEO_SHARING)){
    			// Set capability video sharing
   				capabilities.setVideoSharingSupport(true);
    		}else if (mimeType.equalsIgnoreCase(MIMETYPE_CAPABILITY_IM_SESSION)){
    			// Set capability IM session
   				capabilities.setImSessionSupport(true);
    		}else if (mimeType.equalsIgnoreCase(MIMETYPE_CAPABILITY_FILE_TRANSFER)){
    			// Set capability file transfer
   				capabilities.setFileTransferSupport(true);
    		}else if (mimeType.equalsIgnoreCase(MIMETYPE_CAPABILITY_PRESENCE_DISCOVERY)){
    			// Set capability presence discovery
				capabilities.setPresenceDiscoverySupport(true);
    		}else if (mimeType.equalsIgnoreCase(MIMETYPE_CAPABILITY_SOCIAL_PRESENCE)){
    			// Set capability social presence
				capabilities.setSocialPresenceSupport(true);
    		}else if (mimeType.equalsIgnoreCase(MIMETYPE_CAPABILITY_GEOLOCATION_PUSH)){
    			// Set capability geoloc push
    			capabilities.setGeolocationPushSupport(true);
    		}else if (mimeType.equalsIgnoreCase(MIMETYPE_CAPABILITY_FILE_TRANSFER_THUMBNAIL)){
    			// Set capability file transfer thumbnail
				capabilities.setFileTransferThumbnailSupport(true);
    		}else if (mimeType.equalsIgnoreCase(MIMETYPE_CAPABILITY_FILE_TRANSFER_HTTP)){
    			// Set capability file transfer HTTP
   				capabilities.setFileTransferHttpSupport(true);
    		}else if (mimeType.equalsIgnoreCase(MIMETYPE_CAPABILITY_EXTENSIONS)){
    			// Set RCS extensions capability
    			int columnIndex = cursor.getColumnIndex(Data.DATA2);
    			if (columnIndex!=-1){
    				String extensions = cursor.getString(columnIndex);
    				String[] extensionList = extensions.split(";");
    				for (int i=0;i<extensionList.length;i++){
    					if (extensionList[i].trim().length()>0){
    						capabilities.addSupportedExtension(extensionList[i]);
    					}
    				}
    			}
    		}else if (mimeType.equalsIgnoreCase(MIMETYPE_FREE_TEXT)){
    			// Set free text
    			int columnIndex = cursor.getColumnIndex(Data.DATA2);
    			if (columnIndex!=-1){
    				presenceInfo.setFreetext(cursor.getString(columnIndex));
    			}
    		}else if (mimeType.equalsIgnoreCase(MIMETYPE_PRESENCE_STATUS)){
    			// Set presence status
    			int columnIndex = cursor.getColumnIndex(Data.DATA2);
    			if (columnIndex!=-1){
    				int presence = cursor.getInt(columnIndex);
    				if (presence == PRESENCE_STATUS_ONLINE){
    					presenceInfo.setPresenceStatus(PresenceInfo.ONLINE);
    				}else if (presence == PRESENCE_STATUS_OFFLINE){
    					presenceInfo.setPresenceStatus(PresenceInfo.OFFLINE);
    				}else{
    					presenceInfo.setPresenceStatus(PresenceInfo.UNKNOWN);
    				}
    			}
    		}else if (mimeType.equalsIgnoreCase(MIMETYPE_REGISTRATION_STATE)){
    			// Set registration state
    			int columnIndex = cursor.getColumnIndex(Data.DATA2);
    			if (columnIndex!=-1){
    				contactInfo.setRegistrationState(cursor.getInt(columnIndex));
    			}
    		}else if (mimeType.equalsIgnoreCase(MIMETYPE_RCS_STATUS)){
    			// Set RCS status
    			int columnIndex = cursor.getColumnIndex(Data.DATA2);
    			if (columnIndex!=-1){
    				contactInfo.setRcsStatus(cursor.getInt(columnIndex));
    			}
    		}else if (mimeType.equalsIgnoreCase(MIMETYPE_RCS_STATUS_TIMESTAMP)){
    			// Set RCS status timestamp
    			int columnIndex = cursor.getColumnIndex(Data.DATA2);
    			if (columnIndex!=-1){
    				contactInfo.setRcsStatusTimestamp(cursor.getLong(columnIndex));
    			}
    		}else if (mimeType.equalsIgnoreCase(MIMETYPE_NUMBER)){
    			// Set contact
    			int columnIndex = cursor.getColumnIndex(Data.DATA1);
    			if (columnIndex!=-1){
    				contactInfo.setContact(cursor.getString(columnIndex));
    			}
    		}
    	}
    	cursor.close();
    	
    	PhotoIcon photoIcon = null;
    	if (photoContent!=null){
    		Bitmap bmp = BitmapFactory.decodeByteArray(photoContent, 0, photoContent.length);
			if (bmp != null) {
				photoIcon = new PhotoIcon(photoContent, bmp.getWidth(), bmp.getHeight(), photoEtag);
			}
    	}
    	presenceInfo.setPhotoIcon(photoIcon);
		contactInfo.setPresenceInfo(presenceInfo);
    	contactInfo.setCapabilities(capabilities);
    	
    	return contactInfo;
    }
    
    /**
     * Utility to extract data from a raw contact.
     *
     * @param rawContactId the rawContactId
     * @return A cursor containing the requested data.
     */
    private Cursor getRawContactDataCursor(final long rawContactId) {
        String[] projection = {
                Data._ID, 
                Data.MIMETYPE, 
                Data.DATA1, 
                Data.DATA2, 
                Website.URL,
                Photo.PHOTO          
        };

        // Filter the mime types 
        String selection = "(" + Data.RAW_CONTACT_ID + " =?) AND (" 
                + Data.MIMETYPE + "=? OR " 
                + Data.MIMETYPE + "=? OR "
                + Data.MIMETYPE + "=? OR "
                + Data.MIMETYPE + "=? OR "
                + Data.MIMETYPE + "=? OR "
                + Data.MIMETYPE + "=? OR "
                + Data.MIMETYPE + "=? OR "
                + Data.MIMETYPE + "=? OR "
                + Data.MIMETYPE + "=? OR "
                + Data.MIMETYPE + "=? OR "
                + Data.MIMETYPE + "=? OR "
                + Data.MIMETYPE + "=? OR "
                + Data.MIMETYPE + "=? OR "
                + Data.MIMETYPE + "=? OR "
                + Data.MIMETYPE + "=? OR "
                + Data.MIMETYPE + "=? OR "
                + Data.MIMETYPE + "=? OR "
                + Data.MIMETYPE + "=? OR "
                + Data.MIMETYPE + "=?)";
        String[] selectionArgs = {
                Long.toString(rawContactId), 
                MIMETYPE_WEBLINK,
                MIMETYPE_PHOTO,
                MIMETYPE_PHOTO_ETAG,
                MIMETYPE_RCS_STATUS,
                MIMETYPE_RCS_STATUS_TIMESTAMP,
                MIMETYPE_REGISTRATION_STATE,
                MIMETYPE_PRESENCE_STATUS,
                MIMETYPE_PRESENCE_TIMESTAMP,
                MIMETYPE_FREE_TEXT,
                MIMETYPE_NUMBER,
                MIMETYPE_CAPABILITY_TIMESTAMP,
                MIMETYPE_CAPABILITY_CS_VIDEO,
                MIMETYPE_CAPABILITY_IMAGE_SHARING,
                MIMETYPE_CAPABILITY_VIDEO_SHARING,
                MIMETYPE_CAPABILITY_IM_SESSION,
                MIMETYPE_CAPABILITY_FILE_TRANSFER,
                MIMETYPE_CAPABILITY_PRESENCE_DISCOVERY,
                MIMETYPE_CAPABILITY_SOCIAL_PRESENCE,
                MIMETYPE_CAPABILITY_GEOLOCATION_PUSH,
                MIMETYPE_CAPABILITY_FILE_TRANSFER_THUMBNAIL,
                MIMETYPE_CAPABILITY_FILE_TRANSFER_HTTP,
                MIMETYPE_CAPABILITY_EXTENSIONS
        };

        Cursor cur = ctx.getContentResolver().query(Data.CONTENT_URI, 
        		projection, 
        		selection, 
        		selectionArgs, 
        		null);

        return cur;
    }
    
    /**
     * Update UI strings when device's locale has changed
     */
    public void updateStrings(){
    	
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
    	
    	// Update My profile display name
    	ContentValues values = new ContentValues();
    	values.put(StructuredName.DISPLAY_NAME, ctx.getString(R.string.rcs_core_my_profile));
    	
    	ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
    			.withSelection("(" + Data.RAW_CONTACT_ID + " =?) AND (" + Data.MIMETYPE + "=?)", new String[]{Long.toString(getRawContactIdForMe()), StructuredName.DISPLAY_NAME})
    			.withValues(values)
    			.build());
    	
    	// Update file transfer menu
    	values.clear();
    	values.put(Data.DATA2, getMimeTypeDescription(MIMETYPE_CAPABILITY_FILE_TRANSFER));
    	ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
    			.withSelection(Data.MIMETYPE + "=?", new String[]{MIMETYPE_CAPABILITY_FILE_TRANSFER})
    			.withValues(values)
    			.build());
    	
    	// Update chat menu 
    	values.clear();
    	values.put(Data.DATA2, getMimeTypeDescription(MIMETYPE_CAPABILITY_IM_SESSION));
    	ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
    			.withSelection(Data.MIMETYPE + "=?", new String[]{MIMETYPE_CAPABILITY_IM_SESSION})
    			.withValues(values)
    			.build());

    	// Update image sharing menu 
    	values.clear();
    	values.put(Data.DATA2, getMimeTypeDescription(MIMETYPE_CAPABILITY_IMAGE_SHARING));
    	ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
    			.withSelection(Data.MIMETYPE + "=?", new String[]{MIMETYPE_CAPABILITY_IMAGE_SHARING})
    			.withValues(values)
    			.build());

    	// Update video sharing menu 
    	values.clear();
    	values.put(Data.DATA2, getMimeTypeDescription(MIMETYPE_CAPABILITY_VIDEO_SHARING));
    	ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
    			.withSelection(Data.MIMETYPE + "=?", new String[]{MIMETYPE_CAPABILITY_VIDEO_SHARING})
    			.withValues(values)
    			.build());

    	// Update CS video menu 
    	values.clear();
    	values.put(Data.DATA2, getMimeTypeDescription(MIMETYPE_CAPABILITY_CS_VIDEO));
    	ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
    			.withSelection(Data.MIMETYPE + "=?", new String[]{MIMETYPE_CAPABILITY_CS_VIDEO})
    			.withValues(values)
    			.build());
    	
    	// Update extensions menu
    	values.clear();
    	values.put(Data.DATA2, getMimeTypeDescription(MIMETYPE_CAPABILITY_COMMON_EXTENSION));
    	ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
    			.withSelection(Data.MIMETYPE + "=?", new String[]{MIMETYPE_CAPABILITY_COMMON_EXTENSION})
    			.withValues(values)
    			.build());

    	if (!ops.isEmpty()){
			// Do the actual database modifications
			try {
				ctx.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
			} catch (RemoteException e) {
				if (logger.isActivated()){
					logger.error("Something went wrong when updating the database strings",e);
				}
			} catch (OperationApplicationException e) {
				if (logger.isActivated()){
					logger.error("Something went wrong when updating the database strings",e);
				}
			}
		}
    }

    /**
     * Clean the RCS entries
     *
     * <br>This removes the RCS entries that are associated to numbers not present in the address book anymore
     * <br>This also creates a RCS raw contact for numbers that are present, have RCS raw contact but not on all raw contacts 
     * (typical example: a RCS number is present in the address book and another contact is created using the same number)
     */
    public void cleanRCSEntries() {
        cleanRCSRawContactsInAB();
        cleanEntriesInRichAB();
    }

    /**
     * Clean AB
     */
    private void cleanRCSRawContactsInAB() {
        // Get all RCS raw contacts id
        String[] projection = {
                Data.RAW_CONTACT_ID,
                Data.DATA1
        };
        String selection = Data.MIMETYPE + "=?";
        String[] selectionArgs = {
                MIMETYPE_NUMBER
        };
        Cursor cursor = ctx.getContentResolver().query(Data.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null);

        // Delete RCS Entry where number is not in the address book anymore
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
    	while (cursor.moveToNext()) {
    		long rawContactId = cursor.getLong(0);
    		String phoneNumber = cursor.getString(1);
    		if (getRawContactIdsFromPhoneNumber(phoneNumber).isEmpty()) {
    			ops.add(ContentProviderOperation.newDelete(RawContacts.CONTENT_URI)
    					.withSelection(RawContacts._ID + "=?", new String[]{Long.toString(rawContactId)})
    					.build());
        		// Also delete the corresponding entries in the aggregation provider
    			ctx.getContentResolver().delete(AggregationData.CONTENT_URI,
    					AggregationData.KEY_RCS_RAW_CONTACT_ID + "=?", 
    					new String[]{Long.toString(rawContactId)});
    		}
    	}
    	cursor.close();
    	
		if (!ops.isEmpty()){
			// Do the actual database modifications
			try {
				ctx.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
			} catch (RemoteException e) {
				if (logger.isActivated()){
					logger.error("Something went wrong when updating the database strings",e);
				}
			} catch (OperationApplicationException e) {
				if (logger.isActivated()){
					logger.error("Something went wrong when updating the database strings",e);
				}
			}
		}
    }

    /**
     * Clean EAB
     */
    private void cleanEntriesInRichAB() {
        // Get All contact in EAB
        String[] projection = {
                RichAddressBookData.KEY_CONTACT_NUMBER
        };
        Cursor cursor = ctx.getContentResolver().query(RichAddressBookData.CONTENT_URI,
                projection,
                null,
                null,
                null);

        // Delete EAB Entry where number is not in the address book anymore
        while (cursor.moveToNext()) {
            String phoneNumber = cursor.getString(0);
            if (getRawContactIdsFromPhoneNumber(phoneNumber).isEmpty()) {
                String where = RichAddressBookData.KEY_CONTACT_NUMBER + "=?";
                String[] selectionArg = {phoneNumber};
                ctx.getContentResolver().delete(RichAddressBookData.CONTENT_URI,
                        where,
                        selectionArg);
            }
        }
    }

    /**
     * Get list of supported MIME types associated to RCS contacts
     * 
     * @return MIME types
     */
    public String[] getRcsMimeTypes(){
    	return new String[] {
    			MIMETYPE_NUMBER,
    			MIMETYPE_RCS_STATUS,
    			MIMETYPE_REGISTRATION_STATE,
    			MIMETYPE_RCS_STATUS_TIMESTAMP,
    			MIMETYPE_PRESENCE_STATUS,
    			MIMETYPE_FREE_TEXT,
    			MIMETYPE_WEBLINK,
    		    MIMETYPE_PHOTO_ETAG,
    		    MIMETYPE_PRESENCE_TIMESTAMP,
    		    MIMETYPE_CAPABILITY_TIMESTAMP,
    		    MIMETYPE_CAPABILITY_CS_VIDEO,
    		    MIMETYPE_CAPABILITY_IMAGE_SHARING,
    		    MIMETYPE_CAPABILITY_VIDEO_SHARING,
    		    MIMETYPE_CAPABILITY_IM_SESSION,
    		    MIMETYPE_CAPABILITY_FILE_TRANSFER,
    		    MIMETYPE_CAPABILITY_PRESENCE_DISCOVERY,
    		    MIMETYPE_CAPABILITY_SOCIAL_PRESENCE,
    		    MIMETYPE_CAPABILITY_GEOLOCATION_PUSH,
    		    MIMETYPE_CAPABILITY_FILE_TRANSFER_THUMBNAIL,
    		    MIMETYPE_CAPABILITY_FILE_TRANSFER_HTTP,
    		    MIMETYPE_CAPABILITY_EXTENSIONS,
    		    MIMETYPE_SEE_MY_PROFILE,
    		    MIMETYPE_RCS_CONTACT,
    		    MIMETYPE_RCS_CAPABLE_CONTACT,
    		    MIMETYPE_NOT_RCS_CONTACT,
    		    MIMETYPE_IM_BLOCKED
		    };
    }

    /**
     * Delete all RCS entries in databases
     */
    public void deleteRCSEntries() {
        // Delete Aggregation data
        ctx.getContentResolver().delete(AggregationData.CONTENT_URI, null, null);

        // Delete presence data
        ctx.getContentResolver().delete(RichAddressBookData.CONTENT_URI, null, null);
    }
    
    /**
     * Get the vCard file associated to a contact
     *
     * @param uri Contact URI in database
     * @return vCard filename
     */
    public String getVisitCard(Uri uri) {
    	String fileName = null;
    	
		Cursor cursor = ctx.getContentResolver().query(uri, null, null, null, null);   			
    	while(cursor.moveToNext()) {
    		String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
    		String lookupKey = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
    		Uri vCardUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey);
    		AssetFileDescriptor fd;
    		try {
    			fd = ctx.getContentResolver().openAssetFileDescriptor(vCardUri, "r");
    			FileInputStream fis = fd.createInputStream();
    			byte[] buf = new byte[(int) fd.getDeclaredLength()];
    			fis.read(buf);
    			String Vcard = new String(buf);

    			fileName = Environment.getExternalStorageDirectory().toString() + File.separator + name + ".vcf";
    			
    			File vCardFile = new File(fileName);

    			if (vCardFile.exists()) 
    				vCardFile.delete();

    			FileOutputStream mFileOutputStream = new FileOutputStream(vCardFile, true);
    			mFileOutputStream.write(Vcard.toString().getBytes());
    			mFileOutputStream.close();
    		} catch (Exception e) {
				if (logger.isActivated()){
					logger.error("Something went wrong when during creation of vcard",e);
				}				
    		}
    	}
    	cursor.close();
    	return fileName;
    }
    
    /**
     * Get boolean capability from database column
     * 
     * @param cursor Cursor
     * @param column Column name
     * @return Boolean capability
     */
    private boolean getCapabilityFronColumn(Cursor cursor, String column) {
    	if (cursor.getInt(cursor.getColumnIndex(column)) == CapabilitiesLog.SUPPORTED) {
    		return true;
    	} else {
    		return false;
    	}
    }

    /**
     * Set boolean capability to database column
     * 
     * @param capability Boolean capability
     * @return Integer
     */
    private int setCapabilityToColumn(boolean capability) {
    	if (capability) {
    		return CapabilitiesLog.SUPPORTED;
    	} else {
    		return CapabilitiesLog.NOT_SUPPORTED;
    	}
    }
}
