package com.orangelabs.rcs.provider.messaging;

import org.gsma.joyn.ft.FileTransferLog;

import android.net.Uri;

/**
 * File transfer data constants
 * 
 * @author Jean-Marc AUFFRET
 */
public class FileTransferData {
	/**
	 * Database URI
	 */
	static final Uri CONTENT_URI = Uri.parse("content://com.orangelabs.rcs.ft/ft");
	
	/**
	 * Column name
	 */
	static final String KEY_ID = FileTransferLog.ID;
	
	/**
	 * Column name
	 */
	static final String KEY_SESSION_ID = FileTransferLog.FT_ID;

	/**
	 * Column name
	 */
	static final String KEY_TIMESTAMP = FileTransferLog.TIMESTAMP;

	/**
	 * Column name
	 */
	static final String KEY_CONTACT = FileTransferLog.CONTACT_NUMBER;
	
	/**
	 * Column name
	 */
	static final String KEY_STATUS = FileTransferLog.STATE;

	/**
	 * Column name
	 */
	static final String KEY_MIME_TYPE = FileTransferLog.MIME_TYPE;
	
	/**
	 * Column name
	 */
	static final String KEY_NAME = FileTransferLog.FILENAME;
	
	/**
	 * Column name
	 */
	static final String KEY_SIZE = FileTransferLog.TRANSFERED_SIZE;
	
	/**
	 * Column name
	 */
	static final String KEY_TOTAL_SIZE = FileTransferLog.FILE_SIZE;	

	/**
	 * Column name
	 */
	static final String KEY_DIRECTION = FileTransferLog.DIRECTION;	
}
