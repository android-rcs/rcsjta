/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.provider;

import com.gsma.rcs.addressbook.RcsAccountException;
import com.gsma.rcs.utils.FileUtils;
import com.gsma.rcs.utils.logger.Logger;

import android.os.Environment;
import android.test.AndroidTestCase;
import android.text.TextUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.regex.Pattern;

public class UserProfilePersistedStorageUtilTest extends AndroidTestCase {

    private static final int MAX_SAVED_ACCOUNT = 3;

    private static final Logger sLogger = Logger
            .getLogger(UserProfilePersistedStorageUtilTest.class.getSimpleName());

    private static final String DB_PATH = Environment.getDataDirectory().toString()
            + UserProfilePersistedStorageUtil.DATABASE_LOCATION;

    private File mSrcdir = new File(DB_PATH);

    File[] mSavedAccounts;

    /**
     * Pattern to check if rcs account have atleast 3 characters.
     */
    private static final Pattern RCS_ACCOUNT_MATCH_PATTERN = Pattern.compile("^\\d{3,}$");

    private static File[] listOfSavedAccounts(final File databasesDir) {
        if (!databasesDir.exists() || !databasesDir.isDirectory()) {
            /*
             * As the database itself doesn't exist , So there won't be any accounts related to user
             * profile saved, So return from here. This is a special case where we don't throw
             * exception as backup and restore files and not always needed to be present and reading
             * information from persisted cache is always optional
             */
            return null;
        }
        FileFilter directoryFilter = new FileFilter() {
            public boolean accept(File file) {
                return file.isDirectory()
                        && RCS_ACCOUNT_MATCH_PATTERN.matcher(file.getName()).matches();
            }
        };
        return databasesDir.listFiles(directoryFilter);
    }

    protected void setUp() throws Exception {
        super.setUp();
        if (!mSrcdir.exists()) {
            if (!mSrcdir.mkdir()) {
                throw new RuntimeException("Impossible to create directory");

            }

        }
        // Clean up all saved configurations
        mSavedAccounts = listOfSavedAccounts(mSrcdir);
        if (mSavedAccounts == null) {
            return;
        }
        for (File savedAccount : mSavedAccounts) {
            FileUtils.deleteDirectory(savedAccount);
        }
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testBackupAccount() throws InterruptedException, IOException, RcsAccountException {
        UserProfilePersistedStorageUtil.tryToBackupAccount("1111");
        /*
         * A timer greater than 1 second is set because some emulator have only an accuracy of 1
         * second.
         */
        Thread.sleep(1010);
        UserProfilePersistedStorageUtil.tryToBackupAccount("2222");
        Thread.sleep(1010);
        UserProfilePersistedStorageUtil.tryToBackupAccount("3333");
        Thread.sleep(1010);
        UserProfilePersistedStorageUtil.tryToBackupAccount("4444");

        mSavedAccounts = listOfSavedAccounts(mSrcdir);
        if (mSavedAccounts == null) {
            throw new RcsAccountException("No Saved account found");
        }
        for (File file : mSavedAccounts) {
            sLogger.info("Account " + file.getName() + " last modified=" + file.lastModified());
        }
        assertTrue("listOfSavedAccounts failed", mSavedAccounts.length == 4);

        String oldestFile = FileUtils.getOldestFile(mSavedAccounts).getName();
        if (TextUtils.isEmpty(oldestFile)) {
            fail("testBackupAccount failed : oldestFile is null or empty");
        } else if (!oldestFile.equals("1111")) {
            fail("testBackupAccount failed: oldestFile is ".concat(oldestFile));
        }
    }

    public void testCleanBackups() throws InterruptedException, IOException, RcsAccountException {
        /* This cleanBackups removes the oldest directory (if MAX_SAVED_ACCOUNT is reached) */
        UserProfilePersistedStorageUtil.tryToBackupAccount("1111");
        /*
         * A timer greater than 1 second is set because some emulator have only an accuracy of 1
         * second.
         */
        Thread.sleep(1010);
        UserProfilePersistedStorageUtil.tryToBackupAccount("2222");
        Thread.sleep(1010);
        UserProfilePersistedStorageUtil.tryToBackupAccount("3333");
        Thread.sleep(1010);
        UserProfilePersistedStorageUtil.tryToBackupAccount("4444");
        UserProfilePersistedStorageUtil.normalizeFileBackup("3333");

        mSavedAccounts = listOfSavedAccounts(mSrcdir);
        if (mSavedAccounts == null) {
            throw new RcsAccountException("No Saved account found");

        }

        for (File file : mSavedAccounts) {
            sLogger.info("Account " + file.getName() + " last modified=" + file.lastModified());
        }

        assertTrue("listOfSavedAccounts MAX_SAVED_ACCOUNT failed",
                mSavedAccounts.length == MAX_SAVED_ACCOUNT);

        String oldestFile = FileUtils.getOldestFile(mSavedAccounts).getName();
        if (TextUtils.isEmpty(oldestFile)) {
            fail("testCleanBackups failed: oldestFile is null or empty");
        } else if (!oldestFile.equals("2222")) {
            fail("testCleanBackups failed: oldestFile is ".concat(oldestFile));
        }
    }

    public void testRestoreDb() throws IOException, RcsAccountException {
        UserProfilePersistedStorageUtil.tryToBackupAccount("2222");
        UserProfilePersistedStorageUtil.tryToRestoreAccount("2222");
    }

}
