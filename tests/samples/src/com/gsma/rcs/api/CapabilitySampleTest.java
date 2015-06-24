package com.gsma.rcs.api;

import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.RcsPersistentStorageException;
import com.gsma.services.rcs.RcsServiceListener;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.RcsServiceNotRegisteredException;
import com.gsma.services.rcs.capability.Capabilities;
import com.gsma.services.rcs.capability.CapabilitiesListener;
import com.gsma.services.rcs.capability.CapabilitiesLog;
import com.gsma.services.rcs.capability.CapabilityService;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactUtil;

import android.database.Cursor;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.util.Log;

public class CapabilitySampleTest extends AndroidTestCase {
    private static final String TAG = "RCSAPI";

    private ContactId remote; 
    
    private CapabilityService capabilityApi;
    
    private Synchronizer synchro = new Synchronizer();    

    protected void setUp() throws Exception {
        super.setUp();
        
        // Format a remote phone number for testing
        try {
            remote =  ContactUtil.getInstance(mContext).formatContact("+33681639059"); 
        } catch (RcsPermissionDeniedException e) {
            Log.e(TAG, "Permission denied");
        }
        assertNotNull(remote);
    }
   
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test API methods
     */
    public void testApiMethods() {
        Log.i(TAG, "testApiMethods");

        // Instanciate the API
        capabilityApi = new CapabilityService(mContext, new RcsServiceListener() {
            @Override
            public void onServiceDisconnected(ReasonCode error) {
                Log.i(TAG, "Disconnected from the RCS service");
            }

            @Override
            public void onServiceConnected() {
                Log.i(TAG, "Connected to the RCS service");
                
                // Test any API method which requires a binding to the API
                getMyCapabilities();
                getContactCapabilities();
                requestContactCapabilities();
                receiveContactCapabilities();
                
                synchro.doNotify();
            }   
        });

        // Connect to the API
        try {
            capabilityApi.connect();
        } catch (RcsPermissionDeniedException e) {
            Log.e(TAG, "Permission denied");
        }
        
        synchro.doWait();
        
        // Disconnect from the API
        capabilityApi.disconnect();       
    }    
    
    /**
     * Gets my capabilities
     */
    public void getMyCapabilities() {
        Log.i(TAG, "testGetMyCapabilities");

        try {
            Capabilities capa = capabilityApi.getMyCapabilities();
            if (capa != null) {
                Log.i(TAG, "Capabilities:");
                Log.i(TAG, "- chat support: " + capa.isImSessionSupported());
                Log.i(TAG, "- FT support: " + capa.isFileTransferSupported());
                Log.i(TAG, "- Video share support: " + capa.isVideoSharingSupported());
                Log.i(TAG, "- Image share support: " + capa.isImageSharingSupported());
                Log.i(TAG, "- Geoloc share support: " + capa.isGeolocPushSupported());
                Log.i(TAG, "- Extensions: " + capa.getSupportedExtensions().size());
            } else {
                Log.i(TAG, "Capabilities not found");
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
     * Gets capabilities of a remote contact
     */
    public void getContactCapabilities() {
        Log.i(TAG, "testGetContactCapabilities");

        try {
            Capabilities capa = capabilityApi.getContactCapabilities(remote);
            if (capa != null) {
                Log.i(TAG, "Capabilities:");
                Log.i(TAG, "- chat support: " + capa.isImSessionSupported());
                Log.i(TAG, "- FT support: " + capa.isFileTransferSupported());
                Log.i(TAG, "- Extensions: " + capa.getSupportedExtensions().size());
            } else {
                Log.i(TAG, "Capabilities not found");
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
     * Requests a refresh of the capabilities for a remote contact
     */
    public void requestContactCapabilities() {
        Log.i(TAG, "testRequestContactCapabilities");
        
        try {
            capabilityApi.requestContactCapabilities(remote);
        } catch (RcsServiceNotAvailableException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsServiceNotRegisteredException e) {
            Log.e(TAG, "RCS service not registered");
        } catch (RcsGenericException e) {
            Log.e(TAG, "Unexpected error", e);
        }
    }

    /**
     * Receives capabilities updates from remote contacts
     */
    public void receiveContactCapabilities() {
        Log.i(TAG, "testReceiveContactCapabilities");

        CapabilitiesListener listener = new CapabilitiesListener() {
            public void onCapabilitiesReceived(ContactId contact, Capabilities capabilities) {
                Log.i(TAG, "Capabilities of " + contact.toString() + ":");
                Log.i(TAG, "- chat support: " + capabilities.isImSessionSupported());
                Log.i(TAG, "- FT support: " + capabilities.isFileTransferSupported());
                Log.i(TAG, "- Video share support: " + capabilities.isVideoSharingSupported());
                Log.i(TAG, "- Image share support: " + capabilities.isImageSharingSupported());
                Log.i(TAG, "- Geoloc share support: " + capabilities.isGeolocPushSupported());
                Log.i(TAG, "- Extensions: " + capabilities.getSupportedExtensions().size());
            }
        };
        try {
            capabilityApi.addCapabilitiesListener(listener);
        } catch (RcsServiceNotAvailableException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsGenericException e) {
            Log.e(TAG, "Unexpected error", e);
        }
    }

    /**
     * Reads capabilities of a remote contact
     */
    public void testReadContactCapabilities() {
        Log.i(TAG, "testReadContactCapabilities");

        String contactNumber = remote.toString();
        Uri uri = Uri.withAppendedPath(CapabilitiesLog.CONTENT_URI, contactNumber);
        Cursor cursor = mContext.getContentResolver().query(uri, null, null, null, null);
        assertNotNull(cursor);
        if (cursor.moveToFirst()) {
            Log.i(TAG, "Capabilities:");
            Log.i(TAG, "- chat support: " + cursor.getInt(
                    cursor.getColumnIndex(CapabilitiesLog.CAPABILITY_IM_SESSION)));
            Log.i(TAG, "- FT support: " + cursor.getInt(
                    cursor.getColumnIndex(CapabilitiesLog.CAPABILITY_FILE_TRANSFER)));
            Log.i(TAG, "- Extensions: " + cursor.getString(
                    cursor.getColumnIndex(CapabilitiesLog.CAPABILITY_EXTENSIONS)));
        } else {
            Log.i(TAG, "Capabilities not found");
        }
        cursor.close();
    }

    // TODO: create an extension
    // TODO: global listener
    // TODO: group of contact
    // TODO: database directly
    // TODO: is a RCS contact
}
