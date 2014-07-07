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
package com.orangelabs.rcs.database;

import android.database.Cursor;
import android.net.Uri;
import android.test.AndroidTestCase;

import com.gsma.services.rcs.JoynContactFormatException;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.contacts.ContactUtils;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.Geoloc;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocPush;
import com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage;
import com.orangelabs.rcs.provider.messaging.MessagingLog;

public class ChatMessageTest extends AndroidTestCase {
	private ContactId remote = null;
	
	protected void setUp() throws Exception {
		super.setUp();
		
		MessagingLog.createInstance(getContext());
		ContactUtils contactUtils = ContactUtils.getInstance(getContext());
		try {
			remote = contactUtils.formatContactId("+339000000");
		} catch (JoynContactFormatException e) {
			fail( "Cannot create contactID");
		}
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testTextMessage() {
		String msgId = "" + System.currentTimeMillis();
		String txt = "Hello";
		InstantMessage msg = new InstantMessage(msgId, remote, txt, true, "display");
		
		// Add entry
		MessagingLog.getInstance().addChatMessage(msg, ChatLog.Message.Direction.OUTGOING);
		
		// Read entry
		Uri uri = Uri.withAppendedPath(ChatLog.Message.CONTENT_CHAT_URI, remote.toString());		
    	Cursor cursor = getContext().getContentResolver().query(uri, 
    			new String[] {
    				ChatLog.Message.DIRECTION,
    				ChatLog.Message.CONTACT_NUMBER,
    				ChatLog.Message.BODY,
    				ChatLog.Message.MIME_TYPE,
    				ChatLog.Message.MESSAGE_TYPE,
    				ChatLog.Message.MESSAGE_ID
    				},
    				"(" + ChatLog.Message.MESSAGE_ID + "='" + msgId + "')", 
    			null, 
    			ChatLog.Message.TIMESTAMP + " ASC");
    	assertEquals(cursor.getCount(), 1);
    	while(cursor.moveToNext()) {
    		int direction = cursor.getInt(0);
    		String contact = cursor.getString(1);
    		String content = cursor.getString(2);
    		assertNotNull(content);
    		String readTxt = new String(content);
    		String contentType = cursor.getString(3);
    		int type = cursor.getInt(4);
    		String id = cursor.getString(5);
    		
    		assertEquals(direction, ChatLog.Message.Direction.OUTGOING);
    		assertEquals(contact, remote.toString());
    		assertEquals(readTxt, txt);
    		assertEquals(contentType, com.gsma.services.rcs.chat.ChatMessage.MIME_TYPE);
    		assertEquals(type, ChatLog.Message.Type.CONTENT);
    		assertEquals(id, msgId);
    	}
	}

	public void testGeolocMessage() {
		String msgId = "" + System.currentTimeMillis();
		GeolocPush geoloc = new GeolocPush("test", 10.0, 11.0, 2000);
		GeolocMessage geolocMsg = new GeolocMessage(msgId, remote, geoloc, true,"display");
		
		// Add entry
		MessagingLog.getInstance().addChatMessage(geolocMsg, ChatLog.Message.Direction.OUTGOING);
		
		// Read entry
		Uri uri = Uri.withAppendedPath(ChatLog.Message.CONTENT_CHAT_URI, remote.toString());		
    	Cursor cursor = getContext().getContentResolver().query(uri, 
    			new String[] {
    				ChatLog.Message.DIRECTION,
    				ChatLog.Message.CONTACT_NUMBER,
    				ChatLog.Message.BODY,
    				ChatLog.Message.MIME_TYPE,
    				ChatLog.Message.MESSAGE_TYPE,
    				ChatLog.Message.MESSAGE_ID
    				},
    				"(" + ChatLog.Message.MESSAGE_ID + "='" + msgId + "')", 
    			null, 
    			ChatLog.Message.TIMESTAMP + " ASC");
    	assertEquals(cursor.getCount(), 1);
    	while(cursor.moveToNext()) {
    		int direction = cursor.getInt(0);
    		String contact = cursor.getString(1);
    		String content = cursor.getString(2);
    		assertNotNull(content);
			Geoloc readGeoloc = ChatLog.getGeoloc(content);
    		assertNotNull(readGeoloc);
			
    		String contentType = cursor.getString(3);
    		int type = cursor.getInt(4);
    		String id = cursor.getString(5);
    		
    		assertEquals(direction, ChatLog.Message.Direction.OUTGOING);
    		assertEquals(contact, remote.toString());
    		assertEquals(readGeoloc.getLabel(), geoloc.getLabel());
    		assertEquals(readGeoloc.getLatitude(), geoloc.getLatitude());
    		assertEquals(readGeoloc.getLongitude(), geoloc.getLongitude());
    		assertEquals(readGeoloc.getExpiration(), geoloc.getExpiration());
    		assertEquals(readGeoloc.getAccuracy(), geoloc.getAccuracy());
    		assertEquals(contentType, com.gsma.services.rcs.chat.GeolocMessage.MIME_TYPE);
    		assertEquals(type, ChatLog.Message.Type.CONTENT);
    		assertEquals(id, msgId);
    	}
	}
}
