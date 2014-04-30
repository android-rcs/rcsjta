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
package com.orangelabs.rcs.popup.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.widget.Toast;

import com.orangelabs.rcs.popup.R;

/**
 * Utility functions
 * 
 * @author Jean-Marc AUFFRET
 */
public class Utils {
	
	/**
	 * CRLF constant
	 */
	public final static String CRLF = "\r\n";
	
	/**
	 * RCS-e feature tag
	 */
	public final static String FEATURE_RCSE = "+g.3gpp.iari-ref";
	
	/**
	 * Construct an NTP time from a date in milliseconds
	 *
	 * @param date Date in milliseconds
	 * @return NTP time in string format
	 */
	public static String constructNTPtime(long date) {
		long ntpTime = 2208988800L;
		long startTime = (date / 1000) + ntpTime;
		return String.valueOf(startTime);
	}

	/**
	 * Returns the local IP address
	 *
	 * @return IP address
	 */
	public static InetAddress getLocalIpAddress() {
		try {
	        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
	            NetworkInterface intf = (NetworkInterface)en.nextElement();
	            for (Enumeration<InetAddress> addr = intf.getInetAddresses(); addr.hasMoreElements();) {
	                InetAddress inetAddress = (InetAddress)addr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress()) {
                        return inetAddress;
                    }
	            }
	        }
	        return null;
		} catch(Exception e) {
			return null;
		}
	}

	/**
	 * Display a toast
	 * 
	 * @param activity Activity
	 * @param message Message to be displayed
	 */
    public static void displayToast(Activity activity, String message) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }
    
	/**
	 * Show an message
	 * 
	 * @param activity Activity
	 * @param msg Message to be displayed
	 * @return Dialog
	 */
    public static AlertDialog showMessage(Activity activity, String msg) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(activity);
    	builder.setMessage(msg);
    	builder.setTitle(R.string.title_info);
    	builder.setCancelable(false);
    	builder.setPositiveButton(activity.getString(R.string.label_ok), null);
    	AlertDialog alert = builder.create();
    	alert.show();
		return alert;
    }

    /**
	 * Show a message and exit activity
	 * 
	 * @param activity Activity
	 * @param msg Message to be displayed
	 */
    public static void showMessageAndExit(final Activity activity, String msg) {
        if (activity.isFinishing()) {
        	return;
        }

		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setMessage(msg);
		builder.setTitle(R.string.title_info);
		builder.setCancelable(false);
		builder.setPositiveButton(activity.getString(R.string.label_ok),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						activity.finish();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
    }

	/**
	 * Returns the contact id associated to a contact number in the Address Book
	 * 
	 * @parma context
	 * @param number Contact number
	 * @return Id or -1 if the contact number does not exist
	 */
	public static int getContactId(Context context, String number){
		int id = -1;

		// Query the Phone API
        Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        		new String[]{ContactsContract.CommonDataKinds.Phone.CONTACT_ID,  ContactsContract.CommonDataKinds.Phone.NUMBER},
        		null, 
     		    null, 
     		    null);
        while(cursor.moveToNext()){
    		String databaseNumber = cursor.getString(1);
    		if (databaseNumber.equals(number)) {
    			id = cursor.getInt(0);
    			break;
    		}
        }
        cursor.close();
	       	
        return id;
	}
}
