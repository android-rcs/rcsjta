package com.orangelabs.rcs.provider.messaging;

import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.ft.FileTransferLog;

import android.net.Uri;

/**
 * Message data constants
 * 
 * @author Jean-Marc AUFFRET
 */
public class MessageData {
	/**
	 * Database URI
	 */
	static final Uri CONTENT_URI = Uri.parse("content://com.orangelabs.rcs.chat/message");
	
	/**
	 * Column name
	 */
	static final String KEY_ID = ChatLog.Message.ID;
	
	/**
	 * Column name
	 */
	static final String KEY_CHAT_ID = ChatLog.Message.CHAT_ID;

	/**
	 * Column name
	 */
	static final String KEY_CONTACT = ChatLog.Message.CONTACT_NUMBER;

	/**
	 * Column name
	 */
	static final String KEY_MSG_ID = ChatLog.Message.MESSAGE_ID;

	/**
	 * Column name
	 */
	static final String KEY_TYPE = ChatLog.Message.MESSAGE_TYPE;

	/**
	 * Column name
	 */
	static final String KEY_CONTENT = ChatLog.Message.BODY;

	/**
	 * Column name
	 */
	static final String KEY_CONTENT_TYPE = ChatLog.Message.MIME_TYPE;

	/**
	 * Column name
	 */
	static final String KEY_DIRECTION = ChatLog.Message.DIRECTION;	

	/**
	 * Column name
	 */
	static final String KEY_STATUS = ChatLog.Message.MESSAGE_STATUS;

	/**
	 * Column name
	 */
	static final String KEY_TIMESTAMP = ChatLog.Message.TIMESTAMP;
	
	/**
	 * Column name
	 */
    static final String KEY_TIMESTAMP_SENT = ChatLog.Message.TIMESTAMP_SENT;
    
	/**
	 * Column name
	 */
    static final String KEY_TIMESTAMP_DELIVERED = ChatLog.Message.TIMESTAMP_DELIVERED;
    
	/**
	 * Column name
	 */
    static final String KEY_TIMESTAMP_DISPLAYED = ChatLog.Message.TIMESTAMP_DISPLAYED;

    /**
	 * Column name KEY_FT_ID : the reference to the File Transfer ID
	 */
	public static final String KEY_FT_ID = ChatLog.Message.FT_ID;
}
