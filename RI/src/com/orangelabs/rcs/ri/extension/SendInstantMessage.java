package com.orangelabs.rcs.ri.extension;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contact.ContactId;
import com.orangelabs.rcs.api.connection.ConnectionManager;
import com.orangelabs.rcs.api.connection.utils.RcsActivity;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.extension.messaging.MessagingSessionUtils;
import com.orangelabs.rcs.ri.utils.ContactListAdapter;
import com.orangelabs.rcs.ri.utils.ContactUtil;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Send an instant multimedia message
 *
 * @author Jean-Marc AUFFRET
 */
public class SendInstantMessage extends RcsActivity {

    private String mServiceId = MessagingSessionUtils.SERVICE_ID;

    /**
     * Spinner for contact selection
     */
    private Spinner mSpinner;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.extension_send_instant_message);

        // Set contact selector
        mSpinner = (Spinner) findViewById(R.id.contact);
        mSpinner.setAdapter(ContactListAdapter.createContactListAdapter(this));

        // Set buttons callback
        Button sendBtn = (Button) findViewById(R.id.send_btn);
        sendBtn.setOnClickListener(btnSendListener);

        /* Register to API connection manager */
        if (!isServiceConnected(ConnectionManager.RcsServiceName.MULTIMEDIA, ConnectionManager.RcsServiceName.CONTACT)) {
            showMessageThenExit(R.string.label_service_not_available);
            return;
        }
        startMonitorServices(ConnectionManager.RcsServiceName.MULTIMEDIA, ConnectionManager.RcsServiceName.CONTACT);

        // Disable button if no contact available
        if (mSpinner.getAdapter().getCount() == 0) {
            sendBtn.setEnabled(false);
        }
    }

    /**
     * Send button callback
     */
    private View.OnClickListener btnSendListener = new View.OnClickListener() {
        public void onClick(View v) {
            // get selected phone number
            ContactListAdapter adapter = (ContactListAdapter) mSpinner.getAdapter();
            String phoneNumber = adapter.getSelectedNumber(mSpinner.getSelectedView());
            ContactId contact = ContactUtil.formatContact(phoneNumber);
            // Initiate session
            sendMessage(contact);
            // Exit activity
            finish();
        }
    };

    /**
     * Send message to a remote contact
     *
     * @param contact Remote contact
     */
    public void sendMessage(ContactId contact) {
        try {
            String content = "Hello world";
            String contentType = MessagingSessionUtils.SERVICE_CONTENT_TYPE;
            getMultimediaSessionApi().sendInstantMultimediaMessage(mServiceId, contact, content.getBytes(), contentType);
            Utils.displayToast(this, getString(R.string.label_instant_message_sent));
        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }
}
