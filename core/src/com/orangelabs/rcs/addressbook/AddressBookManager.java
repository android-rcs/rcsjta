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

package com.orangelabs.rcs.addressbook;

import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract.CommonDataKinds.Phone;

import com.orangelabs.rcs.core.CoreException;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.provider.eab.ContactsManager;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Address book manager:<br>
 * <br>
 * This manager is responsible of the synchronization between the native address book and the RCS
 * contacts. <br>
 * It observes the modifications done to the ContactsContract provider and revokes the missing
 * contacts. <br>
 * It also is responsible for creating the contacts if missing at first launch of the service. <br>
 * For more information, see the corresponding chapter in specifications.
 */
public class AddressBookManager {
    /**
     * Address book changed event listeners
     */
    private Vector<AddressBookEventListener> listeners = new Vector<AddressBookEventListener>();

    /**
     * Content resolver
     */
    private ContentResolver contentResolver;

    /**
     * Cursor used to observe ContactsContract
     */
    private Cursor contactsContractCursor;

    /**
     * Content observer
     */
    private ContactsContractObserver contactsContractObserver;

    /**
     * Check handler
     */
    private CheckHandler checkHandler = new CheckHandler();

    /**
     * Check message ID
     */
    private final static int CHECK_MESSAGE = 5765;

    /**
     * Minimum period awaited before we do the checking
     */
    private final static int MINIMUM_CHECK_PERIOD = 1 * 1000;

    /**
     * Content observer registered flag
     */
    private boolean observerIsRegistered = false;

    /**
     * Background service executor
     */
    private ExecutorService cleanupExecutor;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     */
    public AddressBookManager() throws CoreException {
        if (logger.isActivated()) {
            logger.info("Address book manager is created");
        }
        this.contentResolver = AndroidFactory.getApplicationContext().getContentResolver();
    }

    /**
     * Start address book monitoring
     */
    public void startAddressBookMonitoring() {
        if (logger.isActivated()) {
            logger.info("Start address book monitoring");
        }

        // Instanciate background executor
        cleanupExecutor = Executors.newSingleThreadExecutor();

        if (!observerIsRegistered) {
            // Instanciate content observer
            contactsContractObserver = new ContactsContractObserver(new Handler());

            // Query contactContracts phone database
            contactsContractCursor = contentResolver.query(Phone.CONTENT_URI, null, null, null,
                    null);

            // Register content observer
            contactsContractCursor.registerContentObserver(contactsContractObserver);
            observerIsRegistered = true;
        }
    }

    /**
     * Stop address book monitoring
     */
    public void stopAddressBookMonitoring() {
        if (logger.isActivated()) {
            logger.info("Stop address book monitoring");
        }

        // Remove the messages that may still be scheduled
        checkHandler.removeMessages(CHECK_MESSAGE);

        // Unregister content observer
        if (observerIsRegistered) {
            contactsContractCursor.unregisterContentObserver(contactsContractObserver);
            observerIsRegistered = false;
            // Close cursor
            contactsContractCursor.close();
        }

        // Shutdown background executor
        cleanupExecutor.shutdown();
    }

    /**
     * Add a listener
     * 
     * @param listener Listener
     */
    public void addAddressBookListener(AddressBookEventListener listener) {
        listeners.addElement(listener);
    }

    /**
     * Remove a listener
     * 
     * @param listener Listener
     */
    public void removeAddressBookListener(AddressBookEventListener listener) {
        listeners.removeElement(listener);
    }

    /**
     * Remove all listeners
     */
    public void removeAllAddressBookListeners() {
        listeners.removeAllElements();
    }

    /**
     * ContactsContract observer
     */
    private class ContactsContractObserver extends ContentObserver {

        public ContactsContractObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            // Something changed in the address book
            if (!checkHandler.hasMessages(CHECK_MESSAGE)) {
                // If we do not have a check already scheduled, schedule a new one
                checkHandler.sendEmptyMessageDelayed(CHECK_MESSAGE, MINIMUM_CHECK_PERIOD);
                if (logger.isActivated()) {
                    logger.debug("New address book checking scheduled in " + MINIMUM_CHECK_PERIOD
                            + " ms");
                }
            }
        }
    };

    /**
     * Handler used to avoid too many checks
     */
    private class CheckHandler extends Handler {
        private boolean isCleanupNeeded;
        private boolean isCleanupRunning;
        private final Object check = new Object();

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (msg.what == CHECK_MESSAGE) {
                // Clean RCS entries associated to numbers that have been removed or modified
                if (logger.isActivated()) {
                    logger.debug("Minimum check period elapsed, notify the listeners that a change occured in the address book");
                }

                // We may receive multiple CHECK_MESSAGE messages while already processing one. We
                // cannot
                // stay in the handler for too long because the application will be killed as ANR.
                // Thus,
                // we will schedule the processing if is is not running or tell the running task
                // that it will have to do it again once it is done.
                boolean scheduleCleanup = false;
                synchronized (check) {
                    if (isCleanupRunning) {
                        // We need to redo it again
                        isCleanupNeeded = true;
                    } else {
                        scheduleCleanup = true;
                    }
                }

                if (scheduleCleanup) {
                    cleanupExecutor.execute(new Runnable() {
                        public void run() {
                            isCleanupRunning = true;

                            while (true) {
                                isCleanupNeeded = false;

                                // Clean RCS entries associated to numbers that have been removed or
                                // modified
                                ContactsManager.getInstance().cleanRCSEntries();

                                // Notify listeners
                                for (int i = 0; i < listeners.size(); i++) {
                                    AddressBookEventListener listener = (AddressBookEventListener) listeners
                                            .elementAt(i);
                                    listener.handleAddressBookHasChanged();
                                }
                                synchronized (check) {
                                    if (!isCleanupNeeded) {
                                        isCleanupRunning = false;
                                        break;
                                    }
                                }
                            }
                        }
                    });
                }
            }
        }
    }
}
