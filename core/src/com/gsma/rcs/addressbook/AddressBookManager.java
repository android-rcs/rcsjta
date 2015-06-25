/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.addressbook;

import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.contact.ContactManagerException;
import com.gsma.rcs.utils.logger.Logger;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract.CommonDataKinds.Phone;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private List<AddressBookEventListener> listeners = new ArrayList<AddressBookEventListener>();

    private ContentResolver mContentResolver;

    /**
     * Cursor used to observe ContactsContract
     */
    private Cursor mContactsContractCursor;

    private ContactsContractObserver mContactsContractObserver;

    private CheckHandler mCheckHandler = new CheckHandler();

    /**
     * Check message ID
     */
    private final static int CHECK_MESSAGE = 5765;

    /**
     * Minimum period awaited before we do the checking in milliseconds
     */
    private final static long MINIMUM_CHECK_PERIOD = 1 * 1000;

    /**
     * Content observer registered flag
     */
    private boolean mObserverIsRegistered = false;

    /**
     * Background service executor
     */
    private ExecutorService mCleanupExecutor;

    private static final Logger sLogger = Logger.getLogger(AddressBookManager.class.getName());

    private ContactManager mContactManager;

    /**
     * Constructor
     * 
     * @param contactManager Contact manager accessor
     */
    public AddressBookManager(ContactManager contactManager) {
        if (sLogger.isActivated()) {
            sLogger.info("Address book manager is created");
        }
        mContentResolver = AndroidFactory.getApplicationContext().getContentResolver();
        mContactManager = contactManager;
    }

    /**
     * Start address book monitoring
     */
    public void startAddressBookMonitoring() {
        if (sLogger.isActivated()) {
            sLogger.info("Start address book monitoring");
        }

        // Instanciate background executor
        mCleanupExecutor = Executors.newSingleThreadExecutor();

        if (!mObserverIsRegistered) {
            // Instanciate content observer
            mContactsContractObserver = new ContactsContractObserver(new Handler());

            // Query contactContracts phone database
            mContactsContractCursor = mContentResolver.query(Phone.CONTENT_URI, null, null, null,
                    null);
            /* TODO: Handle mContactsContractCursor when null. */

            // Register content observer
            mContactsContractCursor.registerContentObserver(mContactsContractObserver);
            mObserverIsRegistered = true;
        }
    }

    /**
     * Stop address book monitoring
     */
    public void stopAddressBookMonitoring() {
        if (sLogger.isActivated()) {
            sLogger.info("Stop address book monitoring");
        }

        // Remove the messages that may still be scheduled
        mCheckHandler.removeMessages(CHECK_MESSAGE);

        // Unregister content observer
        if (mObserverIsRegistered) {
            mContactsContractCursor.unregisterContentObserver(mContactsContractObserver);
            mObserverIsRegistered = false;
            // Close cursor
            mContactsContractCursor.close();
        }

        // Shutdown background executor
        mCleanupExecutor.shutdownNow();
    }

    /**
     * Add a listener
     * 
     * @param listener Listener
     */
    public void addAddressBookListener(AddressBookEventListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove a listener
     * 
     * @param listener Listener
     */
    public void removeAddressBookListener(AddressBookEventListener listener) {
        listeners.remove(listener);
    }

    /**
     * Remove all listeners
     */
    public void removeAllAddressBookListeners() {
        listeners.clear();
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
            if (!mCheckHandler.hasMessages(CHECK_MESSAGE)) {
                // If we do not have a check already scheduled, schedule a new one
                mCheckHandler.sendEmptyMessageDelayed(CHECK_MESSAGE, MINIMUM_CHECK_PERIOD);
                if (sLogger.isActivated()) {
                    sLogger.debug(new StringBuilder("New address book checking scheduled in ")
                            .append(MINIMUM_CHECK_PERIOD).append(" ms").toString());
                }
            }
        }
    };

    /**
     * Handler used to avoid too many checks
     */
    private class CheckHandler extends Handler {
        private boolean mCleanupNeeded;
        private boolean mCleanupRunning;
        private final Object mCheck = new Object();

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (msg.what == CHECK_MESSAGE) {
                /* Clean RCS entries associated to numbers that have been removed or modified */
                if (sLogger.isActivated()) {
                    sLogger.debug("Minimum check period elapsed, notify listeners that a change occurred in the address book");
                }

                /*
                 * We may receive multiple CHECK_MESSAGE messages while already processing one. We
                 * cannot stay in the handler for too long because the application will be killed as
                 * ANR. Thus, we will schedule the processing if is is not running or tell the
                 * running task that it will have to do it again once it is done.
                 */
                boolean scheduleCleanup = false;
                synchronized (mCheck) {
                    if (mCleanupRunning) {
                        /* We need to re-do it again */
                        mCleanupNeeded = true;
                    } else {
                        scheduleCleanup = true;
                    }
                }

                if (scheduleCleanup) {
                    mCleanupExecutor.execute(new Runnable() {
                        public void run() {
                            try {
                                mCleanupRunning = true;

                                while (true) {
                                    mCleanupNeeded = false;

                                    /*
                                     * Clean RCS entries associated to numbers that have been
                                     * removed or modified
                                     */
                                    mContactManager.cleanRCSEntries();

                                    for (AddressBookEventListener listener : listeners) {
                                        listener.handleAddressBookHasChanged();
                                    }
                                    synchronized (mCheck) {
                                        if (!mCleanupNeeded) {
                                            mCleanupRunning = false;
                                            break;
                                        }
                                    }
                                }
                            } catch (ContactManagerException e) {
                                // TODO CR037 exception handling
                                sLogger.error("Failed to check address book!", e);
                            } catch (RuntimeException e) {
                                /*
                                 * Intentionally catch runtime exceptions as else it will abruptly
                                 * end the thread and eventually bring the whole system down, which
                                 * is not intended.
                                 */
                                // TODO CR037 exception handling
                                sLogger.error("Failed to check address book!", e);
                            }
                        }
                    });
                }
            }
        }
    }
}
