package com.orangelabs.rcs.database;

import android.database.Cursor;
import android.net.Uri;
import android.test.AndroidTestCase;

import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.Geoloc;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocPush;
import com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage;
import com.orangelabs.rcs.provider.messaging.MessagingLog;

public class ChatMessageTest extends AndroidTestCase {
	protected void setUp() throws Exception {
		super.setUp();
		
		MessagingLog.createInstance(mContext);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testTextMessage() {
		String remote = "+339000000";
		String msgId = "" + System.currentTimeMillis();
		String txt = "Hello";
		InstantMessage msg = new InstantMessage(msgId, remote, txt, true, "display");
		
		// Add entry
		MessagingLog.getInstance().addChatMessage(msg, ChatLog.Message.Direction.OUTGOING);
		
		// Read entry
		Uri uri = Uri.withAppendedPath(ChatLog.Message.CONTENT_CHAT_URI, remote);		
    	Cursor cursor = mContext.getContentResolver().query(uri, 
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
    		assertEquals(contact, remote);
    		assertEquals(readTxt, txt);
    		assertEquals(contentType, com.gsma.services.rcs.chat.ChatMessage.MIME_TYPE);
    		assertEquals(type, ChatLog.Message.Type.CONTENT);
    		assertEquals(id, msgId);
    	}
	}

	public void testGeolocMessage() {
		String remote = "+339000000";
		String msgId = "" + System.currentTimeMillis();
		GeolocPush geoloc = new GeolocPush("test", 10.0, 11.0, 2000);
		GeolocMessage geolocMsg = new GeolocMessage(msgId, remote, geoloc, true,"display");
		
		// Add entry
		MessagingLog.getInstance().addChatMessage(geolocMsg, ChatLog.Message.Direction.OUTGOING);
		
		// Read entry
		Uri uri = Uri.withAppendedPath(ChatLog.Message.CONTENT_CHAT_URI, remote);		
    	Cursor cursor = mContext.getContentResolver().query(uri, 
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
    		assertEquals(contact, remote);
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
