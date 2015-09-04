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

package com.gsma.rcs.br;

import com.gsma.rcs.provider.BackupRestoreDb;
import com.gsma.rcs.utils.FileUtils;
import com.gsma.rcs.utils.logger.Logger;

import android.os.Environment;
import android.test.AndroidTestCase;
import android.text.TextUtils;

import java.io.File;

public class DbBackupRestoreTest extends AndroidTestCase {

    private static final int MAX_SAVED_ACCOUNT = 3;

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private static final String DB_PATH = new StringBuilder(Environment.getDataDirectory()
            .toString()).append(BackupRestoreDb.DATABASE_LOCATION).toString();

    private File srcdir = new File(DB_PATH);

    File[] savedAccounts;

    protected void setUp() throws Exception {
        super.setUp();
        if (!srcdir.exists()) {
            srcdir.mkdir();
        }
        // Clean up all saved configurations
        savedAccounts = BackupRestoreDb.listOfSavedAccounts(srcdir);
        if (savedAccounts == null) {
            return;
        }
        for (File savedAccount : savedAccounts) {
            FileUtils.deleteDirectory(savedAccount);
        }
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testBackupAccount() throws InterruptedException {
        try {
            BackupRestoreDb.backupAccount("1111");
            /*
             * A timer greater than 1 second is set because some emulator have only an accuracy of 1
             * second.
             */
            Thread.sleep(1010);
            BackupRestoreDb.backupAccount("2222");
            Thread.sleep(1010);
            BackupRestoreDb.backupAccount("3333");
            Thread.sleep(1010);
            BackupRestoreDb.backupAccount("4444");
        } catch (Exception e) {
            fail(e.getMessage());
        }
        try {
            savedAccounts = BackupRestoreDb.listOfSavedAccounts(srcdir);
            for (File file : savedAccounts) {
                logger.info(new StringBuilder("Account ").append(file.getName())
                        .append(" last modified=").append(file.lastModified()).toString());
            }
        } catch (IllegalArgumentException e) {
            fail(e.getMessage());
        }
        assertTrue("listOfSavedAccounts failed", savedAccounts != null && savedAccounts.length == 4);
        String oldestFile = FileUtils.getOldestFile(savedAccounts).getName();
        if (TextUtils.isEmpty(oldestFile)) {
            fail("testBackupAccount failed : oldestFile is null or empty");
        } else if (!oldestFile.equals("1111")) {
            fail("testBackupAccount failed: oldestFile is ".concat(oldestFile));
        }
    }

    public void testCleanBackups() throws InterruptedException {
        try {
            /* This cleanBackups removes the oldest directory (if MAX_SAVED_ACCOUNT is reached) */
            BackupRestoreDb.backupAccount("1111");
            /*
             * A timer greater than 1 second is set because some emulator have only an accuracy of 1
             * second.
             */
            Thread.sleep(1010);
            BackupRestoreDb.backupAccount("2222");
            Thread.sleep(1010);
            BackupRestoreDb.backupAccount("3333");
            Thread.sleep(1010);
            BackupRestoreDb.backupAccount("4444");
        } catch (Exception e) {
            fail(e.getMessage());
        }
        BackupRestoreDb.cleanBackups("3333");
        savedAccounts = null;
        try {
            savedAccounts = BackupRestoreDb.listOfSavedAccounts(srcdir);
            for (File file : savedAccounts) {
                logger.info(new StringBuilder("Account ").append(file.getName())
                        .append(" last modified=").append(file.lastModified()).toString());
            }
        } catch (IllegalArgumentException e) {
            fail(e.getMessage());
        }
        assertTrue("listOfSavedAccounts MAX_SAVED_ACCOUNT failed", savedAccounts != null
                && savedAccounts.length == MAX_SAVED_ACCOUNT);

        String oldestFile = FileUtils.getOldestFile(savedAccounts).getName();
        if (TextUtils.isEmpty(oldestFile)) {
            fail("testCleanBackups failed: oldestFile is null or empty");
        } else if (!oldestFile.equals("2222")) {
            fail("testCleanBackups failed: oldestFile is ".concat(oldestFile));
        }
    }

    public void testRestoreDb() {
        try {
            BackupRestoreDb.backupAccount("2222");
            BackupRestoreDb.restoreAccount("2222");
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

}
