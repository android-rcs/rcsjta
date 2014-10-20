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
package com.orangelabs.rcsrcs.br;

import java.io.File;

import javax2.sip.InvalidArgumentException;

import android.os.Environment;
import android.test.AndroidTestCase;

import com.orangelabs.rcs.provider.BackupRestoreDb;
import com.orangelabs.rcs.utils.FileUtils;
import com.orangelabs.rcs.utils.logger.Logger;

public class DbBackupRestoreTest extends AndroidTestCase {
	
	private static final int MAX_SAVED_ACCOUNT = 3;
	
	private Logger logger = Logger.getLogger(this.getClass().getName());
	File srcdir = new File(Environment.getDataDirectory() + "/data/com.orangelabs.rcs/databases");
	File[] savedAccounts = null;

	protected void setUp() throws Exception {
		super.setUp();
		// Clean up all saved configurations
		savedAccounts = BackupRestoreDb.listOfSavedAccounts(srcdir);
		if (savedAccounts != null) {
			for (File savedAccount : savedAccounts) {
				try {
					FileUtils.deleteDirectory(savedAccount);
				} catch (Exception e) {
					logger.info("Failed to delete account " + savedAccount.getName());
				}
			}
		}
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testBackupAccount() {
		assertTrue(BackupRestoreDb.backupAccount("1111"));
		assertTrue(BackupRestoreDb.backupAccount("2222"));
		assertTrue(BackupRestoreDb.backupAccount("3333"));
		assertTrue(BackupRestoreDb.backupAccount("4444"));
		try {
			savedAccounts = BackupRestoreDb.listOfSavedAccounts(srcdir);
			for (File file : savedAccounts) {
				logger.info("account " + file);
			}
		} catch (InvalidArgumentException e) {
			fail(e.getMessage());
		}
		assertTrue("listOfSavedAccounts failed", savedAccounts != null && savedAccounts.length == 4);
		assertTrue("getOldestFile failed", FileUtils.getOldestFile(savedAccounts).getName().equals("1111"));
	}

	public void testCleanBackups() {
		// This cleanBackups removes the oldest directory (if MAX_SAVED_ACCOUNT is reached)
		assertTrue(BackupRestoreDb.backupAccount("1111"));
		assertTrue(BackupRestoreDb.backupAccount("2222"));
		assertTrue(BackupRestoreDb.backupAccount("3333"));
		assertTrue(BackupRestoreDb.backupAccount("4444"));
		BackupRestoreDb.cleanBackups("3333");
		savedAccounts = null;
		String oldestConfig = "1111";
		try {
			savedAccounts = BackupRestoreDb.listOfSavedAccounts(srcdir);
			for (File file : savedAccounts) {
				logger.info("account " + file);
				if ("1111".equals(file.getName())) {
					oldestConfig = null;
				}
			}
		} catch (InvalidArgumentException e) {
			fail(e.getMessage());
		}
		assertTrue("listOfSavedAccounts MAX_SAVED_ACCOUNT failed", savedAccounts != null && savedAccounts.length == MAX_SAVED_ACCOUNT);
		assertTrue("listOfSavedAccounts Oldest configuration removed failed",  oldestConfig != null);
	}

	public void testRestoreDb() {
		assertTrue(BackupRestoreDb.backupAccount("2222"));
		assertTrue("restoreAccountProviders failed", BackupRestoreDb.restoreAccount("2222"));
	}

}
