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

package com.gsma.services.rcs.contacts;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.os.IInterface;
import android.provider.ContactsContract;

import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceListener;
import com.gsma.services.rcs.RcsServiceNotAvailableException;

/**
 * Contacts service offers additional methods to manage RCS info in the
 * local address book.
 *  
 * The parameter contact in the API supports the following formats:
 * MSISDN in national or international format, SIP address, SIP-URI
 * or Tel-URI.
 * 
 * @author Jean-Marc AUFFRET 
 */
public class ContactsService extends RcsService {
	/**
	 * API
	 */
	private IContactsService mApi;
	
	private static final String ERROR_CNX = "Contacts service not connected";
	
    /**
     * Constructor
     * 
     * @param ctx Application context
     * @param listener Service listener
     */
    public ContactsService(Context ctx, RcsServiceListener listener) {
    	super(ctx, listener);
    }

    /**
     * Connects to the API
     */
    public void connect() {
    	mCtx.bindService(new Intent(IContactsService.class.getName()), apiConnection, 0);
    }
    
    /**
     * Disconnects from the API
     */
    public void disconnect() {
    	try {
    		mCtx.unbindService(apiConnection);
        } catch(IllegalArgumentException e) {
        	// Nothing to do
        }
    }

	/**
	 * Set API interface
	 * 
	 * @param api API interface
	 */
    protected void setApi(IInterface api) {
    	super.setApi(api);
        mApi = (IContactsService)api;
    }
    
    /**
	 * Service connection
	 */
	private ServiceConnection apiConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
        	setApi(IContactsService.Stub.asInterface(service));
        	if (mListener != null) {
        		mListener.onServiceConnected();
        	}
        }

        public void onServiceDisconnected(ComponentName className) {
        	setApi(null);
        	if (mListener != null) {
        		mListener.onServiceDisconnected(Error.CONNECTION_LOST);
        	}
        }
    };
    
    /**
     * Returns the rcs contact infos from its contact ID (i.e. MSISDN)
     * 
     * @param contact Contact ID
     * @return RcsContact
     * @throws RcsServiceException
     * @see RcsContact
     */
	public RcsContact getRcsContact(ContactId contact) throws RcsServiceException {
		if (mApi != null) {
			try {
				return mApi.getRcsContact(contact);
			} catch(Exception e) {
				throw new RcsServiceException(e);
			}
		} else {
			throw new RcsServiceNotAvailableException(ERROR_CNX);
		}
    }
    
    /**
     * Returns the list of rcs contacts
     * 
     * @return List of contacts
     * @throws RcsServiceException
     * @see RcsContact
     */
    public Set<RcsContact> getRcsContacts() throws RcsServiceException {
		if (mApi != null) {
			try {
	    		Set<RcsContact> result = new HashSet<RcsContact>();
	    		List<RcsContact> contacts = mApi.getRcsContacts();
	    		for(int i=0; i < contacts.size(); i++) {
	    			RcsContact contact = contacts.get(i);
	    			result.add(contact);
	    		}
				return result;
			} catch(Exception e) {
				throw new RcsServiceException(e);
			}
		} else {
			throw new RcsServiceNotAvailableException(ERROR_CNX);
		}
    }

    /**
     * Returns the list of online contacts (i.e. registered)
     * 
     * @return List of contacts
     * @throws RcsServiceException
     * @see RcsContact
     */
    public Set<RcsContact> getRcsContactsOnline() throws RcsServiceException {
		if (mApi != null) {
			try {
	    		Set<RcsContact> result = new HashSet<RcsContact>();
	    		result.addAll(mApi.getRcsContactsOnline());
				return result;
			} catch(Exception e) {
				throw new RcsServiceException(e);
			}
		} else {
			throw new RcsServiceNotAvailableException(ERROR_CNX);
		}
    }

    /**
     * Returns the list of contacts supporting a given extension or service ID
     * 
     * @param serviceId Service ID
     * @return List of contacts
     * @throws RcsServiceException
     * @see RcsContact
     */
    public Set<RcsContact> getRcsContactsSupporting(String serviceId) throws RcsServiceException {
		if (mApi != null) {
			try {
	    		Set<RcsContact> result = new HashSet<RcsContact>();
	    		result.addAll(mApi.getRcsContactsSupporting(serviceId));
				return result;
			} catch(Exception e) {
				throw new RcsServiceException(e);
			}
		} else {
			throw new RcsServiceNotAvailableException(ERROR_CNX);
		}
    }
    
    /**
     * Returns the vCard of a contact. The method returns the complete filename including the path of the visit
     * card. The filename has the file extension ".vcf" and is generated from the native address book
     * vCard URI (see Android SDK attribute ContactsContract.Contacts.CONTENT_VCARD_URI which returns
     * the referenced contact formatted as a vCard when opened through openAssetFileDescriptor(Uri, String)).
     * 
     * @param ctx Application context
     * @param contactUri Contact URI of the contact in the native address book
     * @return Filename of vCard
     * @throws RcsServiceException
     */
    public static String getVCard(Context ctx, Uri contactUri) throws RcsServiceException {
    	String fileName = null;
		Cursor cursor = ctx.getContentResolver().query(contactUri, null, null, null, null);   			
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
    		} catch(Exception e) {
				throw new RcsServiceException(e);
    		}
    	}
    	cursor.close();
    	return fileName;    	
    }
}
