package com.orangelabs.rcs.provider.ipcall;

import com.gsma.services.rcs.ipcall.IPCallLog;

import android.net.Uri;

/**
* IP call history provider data 
* 
* @author owom5460
*/
public class IPCallData {
	/**
	 * Database URI
	 */
	protected static final Uri CONTENT_URI = Uri.parse("content://com.orangelabs.rcs.ipcall/ipcall");
		
	/**
	 * Column name
	 */
	static final String KEY_ID = IPCallLog.ID;
	
	/**
	 * Column name
	 */
	static final String KEY_CONTACT = IPCallLog.CONTACT_NUMBER;

	/**
	 * Column name
	 */
	static final String KEY_DIRECTION = IPCallLog.DIRECTION;	

	/**
	 * Column name
	 */
	static final String KEY_TIMESTAMP = IPCallLog.TIMESTAMP;

	/**
	 * Column name
	 */
	static final String KEY_STATUS = IPCallLog.STATE;

	/**
	 * Column name
	 */
	static final String KEY_SESSION_ID = IPCallLog.CALL_ID;
}
