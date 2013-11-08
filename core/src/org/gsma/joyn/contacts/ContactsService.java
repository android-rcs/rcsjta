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

package org.gsma.joyn.contacts;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.gsma.joyn.JoynService;
import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.JoynServiceListener;
import org.gsma.joyn.JoynServiceNotAvailableException;
import org.gsma.joyn.capability.ICapabilityService;

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
public class ContactsService extends JoynService {
	/**
	 * API
	 */
	private IContactsService api = null;
	
    /**
     * Constructor
     * 
     * @param ctx Application context
     * @param listener Service listener
     */
    public ContactsService(Context ctx, JoynServiceListener listener) {
    	super(ctx, listener);
    }

    /**
     * Connects to the API
     */
    public void connect() {
    	ctx.bindService(new Intent(IContactsService.class.getName()), apiConnection, 0);
    }
    
    /**
     * Disconnects from the API
     */
    public void disconnect() {
    	try {
    		ctx.unbindService(apiConnection);
        } catch(IllegalArgumentException e) {
        	// Nothing to do
        }
    }
    

    /**
	 * Service connection
	 */
	private ServiceConnection apiConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
        	setApi(ICapabilityService.Stub.asInterface(service));
        	if (serviceListener != null) {
        		serviceListener.onServiceConnected();
        	}
        }

        public void onServiceDisconnected(ComponentName className) {
        	api = null;
        	if (serviceListener != null) {
        		serviceListener.onServiceDisconnected(Error.CONNECTION_LOST);
        	}
        }
    };
      
    
    /**
     * Returns the joyn contact infos from its contact ID (i.e. MSISDN)
     * 
     * @param contactId Contact ID
     * @return Contact
     * @throws JoynServiceException
     */
	public JoynContact getJoynContact(String contactId) throws JoynServiceException {
		if (api != null) {
			try {
				return api.getJoynContact(contactId);
			} catch(Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
    }
    
    /**
     * Returns the list of joyn contacts
     * 
     * @return List of contacts
     * @throws JoynServiceException
     */
    public Set<JoynContact> getJoynContacts() throws JoynServiceException {
		if (api != null) {
			try {
	    		Set<JoynContact> result = new HashSet<JoynContact>();
	    		List<JoynContact> contacts = api.getJoynContacts();
	    		for(int i=0; i < contacts.size(); i++) {
	    			JoynContact contact = contacts.get(i);
	    			result.add(contact);
	    		}
				return result;
			} catch(Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
    }

    /**
     * Returns the list of online contacts (i.e. registered)
     * 
     * @return List of contacts
     * @throws JoynServiceException
     */
    public Set<JoynContact> getJoynContactsOnline() throws JoynServiceException {
		if (api != null) {
			try {
	    		Set<JoynContact> result = new HashSet<JoynContact>();
	    		result.addAll(api.getJoynContactsOnline());
				return result;
			} catch(Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
    }

    /**
     * Returns the list of contacts supporting a given extension (i.e. feature tag)
     * 
     * @param tag Supported extension tag. The format of the tag may be the complete
     *  IARI tag (i.e. +g.3gpp.iari-ref="xxxxxx") or just the right part of the tag
     *   without quotes (xxxxxx).
     * @return List of contacts
     * @throws JoynServiceException
     */
    public Set<JoynContact> getJoynContactsSupporting(String tag) throws JoynServiceException {
		if (api != null) {
			try {
	    		Set<JoynContact> result = new HashSet<JoynContact>();
	    		result.addAll(api.getJoynContactsSupporting(tag));
				return result;
			} catch(Exception e) {
				throw new JoynServiceException(e.getMessage());
			}
		} else {
			throw new JoynServiceNotAvailableException();
		}
    }
    
    /**
     * Returns the vCard of a contact. The method returns the complete filename including the path of the visit
     * card. The filename has the file extension “.vcf” and is generated from the native address book
     * vCard URI (see Android SDK attribute ContactsContract.Contacts.CONTENT_VCARD_URI which returns
     * the referenced contact formatted as a vCard when opened through openAssetFileDescriptor(Uri, String)).
     * 
     * @param ctx Application context
     * @param contactUri Contact URI of the contact in the native address book
     * @return Filename of vCard
     * @throws JoynServiceException
     */
    public static String getVCard(Context ctx, Uri contactUri) throws JoynServiceException {
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
				throw new JoynServiceException(e.getMessage());
    		}
    	}
    	cursor.close();
    	return fileName;    	
    }

	@Override
	protected void setApi(IInterface api) {
			this.api = (IContactsService) api;
			setGenericApi(api);
	}    
}
