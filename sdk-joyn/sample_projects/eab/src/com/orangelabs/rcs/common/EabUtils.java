/*******************************************************************************
 * Software Name : RCS
 *
 * Copyright © 2010 France Telecom S.A.
 ******************************************************************************/

package com.orangelabs.rcs.common;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.text.TextUtils;

/**
 * Some utility functions related to the address book
 */
public class EabUtils {
	
	/**
     * Got from android.provider.Contacts
     * 
     * This looks up the provider name defined in
     * {@link android.provider.Im.ProviderNames} from the predefined IM protocol id.
     * This is used for interacting with the IM application.
     *
     * @param protocol the protocol ID
     * @return the provider name the IM app uses for the given protocol, or null if no
     * provider is defined for the given protocol
     * @hide
     */
    public static String lookupProviderNameFromId(int protocol) {
        switch (protocol) {
            case PROTOCOL_GOOGLE_TALK:
                //return Im.ProviderNames.GTALK;
                return "GTalk";
            case PROTOCOL_AIM:
                //return Im.ProviderNames.AIM;
                return ProviderNames.AIM;
            case PROTOCOL_MSN:
                //return Im.ProviderNames.MSN;
                return "MSN";
            case PROTOCOL_YAHOO:
                //return Im.ProviderNames.YAHOO;
                return "Yahoo";
            case PROTOCOL_ICQ:
                //return Im.ProviderNames.ICQ;
                return "ICQ";
            case PROTOCOL_JABBER:
                //return Im.ProviderNames.JABBER;
                return "JABBER";
            case PROTOCOL_SKYPE:
                //return Im.ProviderNames.SKYPE;
                return "SKYPE";
            case PROTOCOL_QQ:
                //return Im.ProviderNames.QQ;
                return "QQ";
        }
        return null;
    }

    /**
     * The predefined IM protocol types. The protocol can either be non-present, one
     * of these types, or a free-form string. These cases are encoded in the AUX_DATA
     * column as:
     *  - null
     *  - pre:<an integer, one of the protocols below>
     *  - custom:<a string>
     */
    public static final int PROTOCOL_AIM = 0;
    public static final int PROTOCOL_MSN = 1;
    public static final int PROTOCOL_YAHOO = 2;
    public static final int PROTOCOL_SKYPE = 3;
    public static final int PROTOCOL_QQ = 4;
    public static final int PROTOCOL_GOOGLE_TALK = 5;
    public static final int PROTOCOL_ICQ = 6;
    public static final int PROTOCOL_JABBER = 7;
	
    /**
     * Known names corresponding to the {@link ProviderColumns#NAME} column
     */
    public static interface ProviderNames {
        //
        //NOTE: update Contacts.java with new providers when they're added.
        //
        String YAHOO = "Yahoo";
        String GTALK = "GTalk";
        String MSN = "MSN";
        String ICQ = "ICQ";
        String AIM = "AIM";
        String XMPP = "XMPP";
        String JABBER = "JABBER";
        String SKYPE = "SKYPE";
        String QQ = "QQ";
    }
    
    /** Build {@link Intent} to launch an action for the given {@link Im} or 
     * {@link Email} row. Returns null when missing protocol or data. */ 
    public static Intent buildImIntent(ContentValues values) {
    	final boolean isEmail = Email.CONTENT_ITEM_TYPE.equals(values.getAsString(Data.MIMETYPE)); 
    	if (!isEmail && !isProtocolValid(values)) { 
    		return null;
    	}
    	final int protocol = isEmail ? Im.PROTOCOL_GOOGLE_TALK : values.getAsInteger(Im.PROTOCOL); 
    	String host = values.getAsString(Im.CUSTOM_PROTOCOL); 
    	String data = values.getAsString(isEmail ? Email.DATA : Im.DATA); 
    	if (protocol != Im.PROTOCOL_CUSTOM) { 
    		// Try bringing in a well-known host for specific protocols
    		host = lookupProviderNameFromId(protocol); 
    	} 
    	if (!TextUtils.isEmpty(host) && !TextUtils.isEmpty(data)) { 
    		final String authority = host.toLowerCase(); 
    		final Uri imUri = new Uri.Builder().scheme("imto").authority( 
    				authority).appendPath(data).build(); 
    		return new Intent(Intent.ACTION_SENDTO, imUri); 
    	} else {
    		return null;
    	}
    }
    
    
    /**
     * Check if given IM protocol is valid 
     * @param values
     * @return
     */
    private static boolean isProtocolValid(ContentValues values) {
    	String protocolString = values.getAsString(Im.PROTOCOL); 
    	if (protocolString == null) {
    		return false; 
    	}
    	try {
    		Integer.valueOf(protocolString); 
    	} catch (NumberFormatException e) { 
    		return false; 
    	}
    	return true; 
    }
    
    /**
     * Kick off an intent to initiate a call.
     */
    public static void initiateCall(Context context, CharSequence phoneNumber) {
    	Intent intent = new Intent(Intent.ACTION_CALL, Uri.fromParts("tel", phoneNumber.toString(), null));
        context.startActivity(intent);
    }

    /**
     * Kick off an intent to initiate an Sms/Mms message.
     */
    public static void initiateSms(Context context, CharSequence phoneNumber) {
		Intent intent = new Intent(Intent.ACTION_SENDTO);
		intent.setData(Uri.parse("sms:"+phoneNumber.toString()));
		context.startActivity(intent);
    }

    /** 
     * Test if the given {@link CharSequence} contains any graphic characters, 
     * first checking {@link TextUtils#isEmpty(CharSequence)} to handle null.
     */
    public static boolean isGraphic(CharSequence str) {
    	return !TextUtils.isEmpty(str) && TextUtils.isGraphic(str); 
    }
    
    /**
     * Get the photo pick intent
     * 
     * @return intent
     */
    public static Intent getPhotoPickIntent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
        intent.setType("image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 96);
        intent.putExtra("outputY", 96);
        intent.putExtra("return-data", true);
        return intent;
    }
    
}
