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
package com.gsma.service.rcs.database;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.test.AndroidTestCase;

import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.RcsCommon;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.contacts.ContactUtils;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.Geoloc;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocPush;
import com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage;
import com.orangelabs.rcs.provider.LocalContentResolver;
import com.orangelabs.rcs.provider.messaging.MessagingLog;

public class ChatMessageTest extends AndroidTestCase {
	private ContactId mContact;
	private Context mContext;
	private ContentResolver mContentResolver;
	
	protected void setUp() throws Exception {
		super.setUp();
		mContext = getContext();
		mContentResolver = mContext.getContentResolver();
		LocalContentResolver localContentResolver = new LocalContentResolver(mContentResolver);
		MessagingLog.createInstance(mContext, localContentResolver);
		ContactUtils contactUtils = ContactUtils.getInstance(mContext);
		try {
			mContact = contactUtils.formatContact("+339000000");
		} catch (RcsContactFormatException e) {
			fail( "Cannot create contactID");
		}
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testTextMessage() {
		String msgId = "" + System.currentTimeMillis();
		String txt = "Hello";
		InstantMessage msg = new InstantMessage(msgId, mContact, txt, true, "display");
		
		// Add entry
		MessagingLog.getInstance().addOutgoingOneToOneChatMessage(msg, ChatLog.Message.Status.Content.SENT, ChatLog.Message.ReasonCode.UNSPECIFIED);
		
		// Read entry
		Uri uri = Uri.withAppendedPath(ChatLog.Message.CONTENT_URI, msgId);		
    	Cursor cursor = mContentResolver.query(uri, 
    			new String[] {
    				ChatLog.Message.DIRECTION,
    				ChatLog.Message.CONTACT,
    				ChatLog.Message.CONTENT,
    				ChatLog.Message.MIME_TYPE,
    				ChatLog.Message.MESSAGE_ID
    				},
    				"(" + ChatLog.Message.MESSAGE_ID + "='" + msgId + "')", 
    			null, 
    			ChatLog.Message.TIMESTAMP + " ASC");
    	assertEquals(cursor.getCount(), 1);
    	while(cursor.moveToNext()) {
    		int direction = cursor.getInt(cursor.getColumnIndex(ChatLog.Message.DIRECTION));
    		String contact = cursor.getString(cursor.getColumnIndex(ChatLog.Message.CONTACT));
    		String content = cursor.getString(cursor.getColumnIndex(ChatLog.Message.CONTENT));
    		assertNotNull(content);
    		String readTxt = new String(content);
    		String mimeType = cursor.getString(cursor.getColumnIndex(ChatLog.Message.MIME_TYPE));
    		String id = cursor.getString(cursor.getColumnIndex(ChatLog.Message.MESSAGE_ID));
    		
    		assertEquals(direction, RcsCommon.Direction.OUTGOING);
    		assertEquals(contact, mContact.toString());
    		assertEquals(readTxt, txt);
    		assertEquals(mimeType, com.gsma.services.rcs.chat.ChatLog.Message.MimeType.TEXT_MESSAGE);
    		assertEquals(id, msgId);
    	}
	}

	public void testGeolocMessage() {
		String msgId = "" + System.currentTimeMillis();
		GeolocPush geoloc = new GeolocPush("test", 10.0, 11.0, 2000);
		GeolocMessage geolocMsg = new GeolocMessage(msgId, mContact, geoloc, true,"display");
		
		// Add entry
		MessagingLog.getInstance().addOutgoingOneToOneChatMessage(geolocMsg, ChatLog.Message.Status.Content.SENT, ChatLog.Message.ReasonCode.UNSPECIFIED);
		
		// Read entry
		Uri uri = Uri.withAppendedPath(ChatLog.Message.CONTENT_URI, msgId);		
    	Cursor cursor = mContentResolver.query(uri, 
    			new String[] {
    				ChatLog.Message.DIRECTION,
    				ChatLog.Message.CONTACT,
    				ChatLog.Message.CONTENT,
    				ChatLog.Message.MIME_TYPE,
    				ChatLog.Message.MESSAGE_ID
    				},
    				"(" + ChatLog.Message.MESSAGE_ID + "='" + msgId + "')", 
    			null, 
    			ChatLog.Message.TIMESTAMP + " ASC");
    	assertEquals(cursor.getCount(), 1);
    	while(cursor.moveToNext()) {
    		int direction = cursor.getInt(cursor.getColumnIndex(ChatLog.Message.DIRECTION));
    		String contact = cursor.getString(cursor.getColumnIndex(ChatLog.Message.CONTACT));
    		String content = cursor.getString(cursor.getColumnIndex(ChatLog.Message.CONTENT));
    		assertNotNull(content);
			Geoloc readGeoloc = new Geoloc(content);
    		assertNotNull(readGeoloc);
			
    		String contentType = cursor.getString(cursor.getColumnIndex(ChatLog.Message.MIME_TYPE));
    		String id = cursor.getString(cursor.getColumnIndex(ChatLog.Message.MESSAGE_ID));
    		
    		assertEquals(direction, RcsCommon.Direction.OUTGOING);
    		assertEquals(contact, mContact.toString());
    		assertEquals(readGeoloc.getLabel(), geoloc.getLabel());
    		assertEquals(readGeoloc.getLatitude(), geoloc.getLatitude());
    		assertEquals(readGeoloc.getLongitude(), geoloc.getLongitude());
    		assertEquals(readGeoloc.getExpiration(), geoloc.getExpiration());
    		assertEquals(readGeoloc.getAccuracy(), geoloc.getAccuracy());
    		assertEquals(contentType, com.gsma.services.rcs.chat.ChatLog.Message.MimeType.GEOLOC_MESSAGE);
    		assertEquals(id, msgId);
    	}
	}
}
