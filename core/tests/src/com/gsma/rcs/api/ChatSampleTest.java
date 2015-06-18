package com.gsma.rcs.api;

import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.RcsPersistentStorageException;
import com.gsma.services.rcs.RcsServiceListener;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.chat.ChatService;
import com.gsma.services.rcs.chat.OneToOneChat;
import com.gsma.services.rcs.chat.OneToOneChatIntent;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactUtil;

import android.content.Intent;
import android.content.IntentFilter;
import android.test.AndroidTestCase;
import android.util.Log;

public class ChatSampleTest extends AndroidTestCase {
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
    
    private ChatService chatApi;

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
        chatApi = new ChatService(mContext, apiServiceListener);
        assertNotNull(chatApi);

        // Connect to the API
        chatApi.connect();
    }
   
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Sends a 1-1 chat message to a remote contact
     */
    public void testSendOneToOneChat() {
        try {
            OneToOneChat chat = chatApi.getOneToOneChat(remote);
            chat.sendMessage("Hello world!");
        } catch (RcsServiceNotAvailableException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsPersistentStorageException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsGenericException e) {
            Log.e(TAG, "Unexpected error", e);
        }
    }

    /**
     * Receives 1-1 chat messages from an Intent receiver
     */
    public void testReceiveOneToOneChat() {
        // Catch chat messages
/*        IntentFilter filter = new IntentFilter(OneToOneChatIntent.ACTION_NEW_ONE_TO_ONE_CHAT_MESSAGE);
        Intent intent = mContext.registerReceiver(null, filter);        
        ContactId remote = intent.getParcelableExtra(OneToOneChatIntent.EXTRA_CONTACT);
        String msgId = intent.getStringExtra(OneToOneChatIntent.EXTRA_MESSAGE_ID);
        try {
            chatApi.markMessageAsRead(msgId);
        } catch (RcsServiceNotAvailableException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsPersistentStorageException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsGenericException e) {
            Log.e(TAG, "Unexpected error", e);
        }       
        Log.i(TAG, "Receive message " + msgId  + " from " + remote.toString());
        
        // Open a conversation with a remote contact who has sent a chat message
        try {
            OneToOneChat chat = chatApi.getOneToOneChat(remote);
            chat.openChat();
        } catch (RcsServiceNotAvailableException e) {
            Log.e(TAG, "RCS service not available");
        } catch (RcsGenericException e) {
            Log.e(TAG, "Unexpected error", e);
        }*/       
    }
        
    // TODO: delivery report
    // TODO: other Intents
    // TODO: database directly (ie. Antivirus)
}
