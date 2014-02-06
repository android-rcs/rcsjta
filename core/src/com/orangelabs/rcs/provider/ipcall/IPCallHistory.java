package com.orangelabs.rcs.provider.ipcall;

import java.util.Calendar;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * IP call history
 * 
 * @author owom5460
 */
public class IPCallHistory {
	/**
	 * Current instance
	 */
	private static IPCallHistory instance = null;

	/**
	 * Content resolver
	 */
	private ContentResolver cr;
	
	/**
	 * Database URI
	 */
	private Uri databaseUri = IPCallData.CONTENT_URI;

	/**
	 * The logger
	 */
	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	/**
	 * Create instance
	 * 
	 * @param ctx Context
	 */
	public static synchronized void createInstance(Context ctx) {
		if (instance == null) {
			instance = new IPCallHistory(ctx);
		}
	}
	
	/**
	 * Returns instance
	 * 
	 * @return Instance
	 */
	public static IPCallHistory getInstance() {
		return instance;
	}
	
	/**
     * Constructor
     * 
     * @param ctx Application context
     */
	private IPCallHistory(Context ctx) {
		super();
		
        this.cr = ctx.getContentResolver();
    }
	
	/**
	 * Add a new entry in the call history 
	 * 
	 * @param contact Remote contact
	 * @param sessionId Session ID
	 * @param direction Direction 
	 * @param audiocontent Audio content
	 * @param videocontent Video content
	 * @param status Call status
	 */
	public Uri addCall(String contact, String sessionId, int direction, MmContent audiocontent, MmContent videocontent, int status) {
		if(logger.isActivated()){
			logger.debug("Add new call entry for contact " + contact + ": session=" + sessionId + ", status=" + status);
		}

		contact = PhoneUtils.extractNumberFromUri(contact);
		ContentValues values = new ContentValues();
		values.put(IPCallData.KEY_SESSION_ID, sessionId);
		values.put(IPCallData.KEY_CONTACT, contact);
		values.put(IPCallData.KEY_DIRECTION, direction);
		values.put(IPCallData.KEY_TIMESTAMP, Calendar.getInstance().getTimeInMillis());
		values.put(IPCallData.KEY_STATUS, status);
		
		return cr.insert(databaseUri, values);
	}

	/**
	 * Update the call status
	 * 
	 * @param sessionId Session ID
	 * @param status New status
	 */
	public void setCallStatus(String sessionId, int status) {
		if (logger.isActivated()) {
			logger.debug("Update call status of session " + sessionId + " to " + status);
		}
		
		ContentValues values = new ContentValues();
		values.put(IPCallData.KEY_STATUS, status);
		cr.update(databaseUri, values, IPCallData.KEY_SESSION_ID + " = " + sessionId, null);
	}
}
