package com.gsma.rcs.api;

import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.RcsServiceListener;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.RcsServiceNotRegisteredException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactUtil;
import com.gsma.services.rcs.extension.MultimediaMessagingSession;
import com.gsma.services.rcs.extension.MultimediaSessionService;

import android.test.AndroidTestCase;
import android.util.Log;

public class MultimediaSessionSampleTest extends AndroidTestCase {
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
    
    private MultimediaSessionService sessionApi;

    private String serviceId = "ext.sample"; 

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
        sessionApi = new MultimediaSessionService(mContext, apiServiceListener);
        assertNotNull(sessionApi);

        // Connect to the API
        sessionApi.connect();
    }
   
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Initiaites a MM session with a remote contact
     */
    public void testInitiate() {
        try {
            MultimediaMessagingSession session= sessionApi.initiateMessagingSession(serviceId, remote);
            // TODO
        } catch (RcsPermissionDeniedException e) {
            Log.e(TAG, "Permission denied");
        } catch (RcsServiceNotAvailableException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsServiceNotRegisteredException e) {
            Log.e(TAG, "RCS service not registered");
        } catch (RcsGenericException e) {
            Log.e(TAG, "Unexpected error", e);
        }       
    }
}
