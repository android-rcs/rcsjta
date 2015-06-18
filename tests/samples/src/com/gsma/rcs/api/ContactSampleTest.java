package com.gsma.rcs.api;

import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.RcsPersistentStorageException;
import com.gsma.services.rcs.RcsServiceListener;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactService;
import com.gsma.services.rcs.contact.ContactUtil;
import com.gsma.services.rcs.contact.RcsContact;

import android.test.AndroidTestCase;
import android.util.Log;

import java.util.Set;

public class ContactSampleTest extends AndroidTestCase {
    private static final String TAG = "RCSAPI";

    private RcsServiceListener apiServiceListener = new RcsServiceListener() {
        @Override
        public void onServiceDisconnected(ReasonCode error) {
            Log.i(TAG, "Disconnected from the RCS service");
        }

        @Override
        public void onServiceConnected() {
            Log.i(TAG, "Connected to the RCS service");
        }   
    };
    
    private ContactId remote; 
    
    private ContactService contactApi;

    protected void setUp() throws Exception {
        super.setUp();
        
        // Format a remote phone number for testing
        try {
            remote =  ContactUtil.getInstance(mContext).formatContact("+33681639059"); 
        } catch (RcsPermissionDeniedException e) {
            Log.e(TAG, "Permission denied");
        }
        assertNotNull(remote);

        // Instanciate the API
        contactApi = new ContactService(mContext, apiServiceListener);
        assertNotNull(contactApi);        

        // Connect to the API
        contactApi.connect();
    }
   
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Format a contact
     */
    public void testFormatContact() {
        try {
            ContactId contact =  ContactUtil.getInstance(mContext).formatContact("06 81 63 90 59");
            Log.i(TAG, "Formatted contact: " + contact.toString());
        } catch (RcsPermissionDeniedException e) {
            Log.e(TAG, "Permission denied");
        }
        assertNotNull(remote);
    }
        
    /**
     * Blocks a remote contact
     */
    public void testBlockContact() {
        try {
            contactApi.blockContact(remote);
        } catch (RcsServiceNotAvailableException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsPersistentStorageException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsGenericException e) {
            Log.e(TAG, "Unexpected error", e);
        }
    }

    /**
     * Unblocks a remote contact
     */
    public void testUnblockContact() {
        try {
            contactApi.unblockContact(remote);
        } catch (RcsServiceNotAvailableException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsPersistentStorageException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsGenericException e) {
            Log.e(TAG, "Unexpected error", e);
        }
    }
    
    /**
     * Gets RCS info of a remote contact
     */
    public void testGetRcsContactInfo() {
        try {
            RcsContact contact = contactApi.getRcsContact(remote);
            if (contact != null) {
                Log.i(TAG, "Contact info:");
                Log.i(TAG, "- Display name: " + contact.getDisplayName());
                Log.i(TAG, "- Online: " + contact.isOnline());
                Log.i(TAG, "- Blocked: " + contact.isBlocked());
            } else {
                Log.i(TAG, "Contact not found");
            }
        } catch (RcsServiceNotAvailableException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsPersistentStorageException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsGenericException e) {
            Log.e(TAG, "Unexpected error", e);
        }
    }

    /**
     * Gets online RCS contacts
     */
    public void testGetRcsContactOnline() {
        try {
            Set<RcsContact> contacts = contactApi.getRcsContactsOnline();
            Log.i(TAG, "Number of contacts online: " + contacts.size());
        } catch (RcsServiceNotAvailableException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsPersistentStorageException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsGenericException e) {
            Log.e(TAG, "Unexpected error", e);
        }
    }

    /**
     * Gets all RCS contacts
     */
    public void testGetRcsContacts() {
        try {
            Set<RcsContact> contacts = contactApi.getRcsContacts();
            Log.i(TAG, "Number of RCS contacts: " + contacts.size());
        } catch (RcsServiceNotAvailableException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsPersistentStorageException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsGenericException e) {
            Log.e(TAG, "Unexpected error", e);
        }
    }
    
    /**
     * Gets RCS contacts supporting a given extension
     */
    public void testGetRcsContactExt() {
        try {
            Set<RcsContact> contacts = contactApi.getRcsContactsSupporting("ext.game");
            Log.i(TAG, "Number of contacts supporting the extension 'ext.game': " + contacts.size());
        } catch (RcsServiceNotAvailableException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsPersistentStorageException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsGenericException e) {
            Log.e(TAG, "Unexpected error", e);
        }
    }
}
