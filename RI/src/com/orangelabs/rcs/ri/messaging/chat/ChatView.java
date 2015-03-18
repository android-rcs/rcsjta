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

package com.orangelabs.rcs.ri.messaging.chat;

import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.chat.ChatLog.Message;
import com.gsma.services.rcs.chat.ChatMessage;
import com.gsma.services.rcs.contact.ContactId;

import com.orangelabs.rcs.ri.ConnectionManager;
import com.orangelabs.rcs.ri.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.messaging.geoloc.EditGeoloc;
import com.orangelabs.rcs.ri.messaging.geoloc.ShowUsInMap;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsDisplayName;
import com.orangelabs.rcs.ri.utils.Utils;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Set;

/**
 * Chat view
 */
public abstract class ChatView extends FragmentActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, IChatView {
    /**
     * The loader's unique ID. Loader IDs are specific to the Activity in which they reside.
     */
    protected static final int LOADER_ID = 1;

    /**
     * Activity result constant
     */
    private final static int SELECT_GEOLOCATION = 0;

    /**
     * The adapter that binds data to the ListView
     */
    protected ChatCursorAdapter mAdapter;

    /**
     * Message composer
     */
    protected EditText composeText;

    /**
     * Utility class to manage the is-composing status
     */
    protected IsComposingManager composingManager;

    /**
     * A locker to exit only once
     */
    protected LockAccess mExitOnce = new LockAccess();

    /**
     * Activity displayed status
     */
    private static boolean sActivityDisplayed = false;

    /**
     * API connection manager
     */
    protected ConnectionManager mCnxManager;

    /**
     * UI handler
     */
    protected Handler handler = new Handler();

    /**
     * The log tag for this class
     */
    private static final String LOGTAG = LogUtils.getTag(ChatView.class.getSimpleName());

    // @formatter:off
    protected static final String[] PROJECTION = new String[] {
        Message.BASECOLUMN_ID, 
        Message.MESSAGE_ID, 
        Message.MIME_TYPE, 
        Message.CONTENT, 
        Message.TIMESTAMP,
        Message.STATUS, 
        Message.DIRECTION, 
        Message.CONTACT
    };
    // @formatter:on

    protected final static String QUERY_SORT_ORDER = new StringBuilder(Message.TIMESTAMP).append(
            " ASC").toString();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.chat_view);

        // Set message composer callbacks
        composeText = (EditText) findViewById(R.id.userText);
        composeText.setOnKeyListener(new OnKeyListener() {

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (KeyEvent.ACTION_DOWN != event.getAction()) {
                    return false;

                }
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_CENTER:
                    case KeyEvent.KEYCODE_ENTER:
                        sendText();
                        return true;
                }
                return false;
            }
        });

        composeText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Check if the text is not null.
                // we do not wish to consider putting the edit text back to null
                // (like when sending message), is having activity
                if (!TextUtils.isEmpty(s)) {
                    // Warn the composing manager that we have some activity
                    if (composingManager != null) {
                        composingManager.hasActivity();
                    }
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Set send button listener
        Button sendBtn = (Button) findViewById(R.id.send_button);
        sendBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                sendText();
            }
        });

        // Initialize the adapter.
        mAdapter = new ChatCursorAdapter(this, null, 0, isSingleChat());

        // Associate the list adapter with the ListView.
        ListView listView = (ListView) findViewById(android.R.id.list);
        listView.setAdapter(mAdapter);
        registerForContextMenu(listView);

        // Register to API connection manager
        mCnxManager = ConnectionManager.getInstance(this);

        if (mCnxManager == null
                || !mCnxManager.isServiceConnected(RcsServiceName.CHAT, RcsServiceName.CONTACT)) {
            Utils.showMessageAndExit(this, getString(R.string.label_service_not_available),
                    mExitOnce);
            return;

        }
        mCnxManager.startMonitorServices(this, mExitOnce, RcsServiceName.CHAT,
                RcsServiceName.CONTACT);
        processIntent();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCnxManager == null) {
            return;

        }
        mCnxManager.stopMonitorServices(this);
        if (mCnxManager.isServiceConnected(RcsServiceName.CHAT)) {
            try {
                removeChatEventListener(mCnxManager.getChatApi());
            } catch (RcsServiceException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sActivityDisplayed = true;
    }

    @Override
    protected void onPause() {
        super.onStart();
        sActivityDisplayed = false;
    }

    /**
     * Return true if the activity is currently displayed or not
     * 
     * @return Boolean
     */
    public static boolean isDisplayed() {
        return sActivityDisplayed;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onNewIntent");
        }
        super.onNewIntent(intent);
        // Replace the value of intent
        setIntent(intent);

        if (mCnxManager.isServiceConnected(RcsServiceName.CHAT, RcsServiceName.CONTACT)) {
            processIntent();
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // A switch-case is useful when dealing with multiple Loaders/IDs
        switch (loader.getId()) {
            case LOADER_ID:
                // The asynchronous load is complete and the data
                // is now available for use. Only now can we associate
                // the queried Cursor with the CursorAdapter.
                mAdapter.swapCursor(cursor);
                break;
        }
        // The listview now displays the queried data.
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // For whatever reason, the Loader's data is now unavailable.
        // Remove any references to the old data by replacing it with a null
        // Cursor.
        mAdapter.swapCursor(null);
    }

    /**
     * Send a text and display it
     */
    private void sendText() {
        String text = composeText.getText().toString();
        if (TextUtils.isEmpty(text)) {
            return;

        }
        // Send text message
        ChatMessage message = sendMessage(text);
        if (message == null) {
            Utils.showMessage(ChatView.this, getString(R.string.label_send_im_failed));
            return;

        }
        // Warn the composing manager that the message was sent
        composingManager.messageWasSent();
        composeText.setText(null);
    }

    /**
     * Send a geolocation
     * 
     * @param geoloc
     */
    private void sendGeoloc(Geoloc geoloc) {
        // Send text message
        ChatMessage message = sendMessage(geoloc);
        if (message == null) {
            Utils.showMessage(ChatView.this, getString(R.string.label_send_im_failed));
        }
    }

    /**
     * Add quick text
     */
    protected void addQuickText() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.label_select_quicktext);
        builder.setCancelable(true);
        builder.setItems(R.array.select_quicktext, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String[] items = getResources().getStringArray(R.array.select_quicktext);
                composeText.append(items[which]);
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Get a geoloc
     */
    protected void getGeoLoc() {
        // Start a new activity to send a geolocation
        startActivityForResult(new Intent(this, EditGeoloc.class), SELECT_GEOLOCATION);
    }

    /**
     * On activity result
     * 
     * @param requestCode Request code
     * @param resultCode Result code
     * @param data Data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;

        }
        switch (requestCode) {
            case SELECT_GEOLOCATION:
                // Get selected geoloc
                Geoloc geoloc = data.getParcelableExtra(EditGeoloc.EXTRA_GEOLOC);
                // Send geoloc
                sendGeoloc(geoloc);
                break;
        }
    }

    /**
     * Show us in a map
     * 
     * @param participants Set of participants
     */
    protected void showUsInMap(Set<String> participants) {
        ShowUsInMap.startShowUsInMap(this, new ArrayList<String>(participants));
    }

    /**
     * Display composing event for contact
     * 
     * @param contact
     * @param status True if contact is composing
     */
    protected void displayComposingEvent(final ContactId contact, final boolean status) {
        final String from = RcsDisplayName.getInstance(this).getDisplayName(contact);
        // Execute on UI handler since callback is executed from service
        handler.post(new Runnable() {
            public void run() {
                TextView view = (TextView) findViewById(R.id.isComposingText);
                if (status) {
                    // Display is-composing notification
                    view.setText(getString(R.string.label_contact_is_composing, from));
                    view.setVisibility(View.VISIBLE);
                } else {
                    // Hide is-composing notification
                    view.setVisibility(View.GONE);
                }
            }
        });
    }
}
