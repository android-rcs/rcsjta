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
	// with Eclipse test methods are run in alphabetic order
	// with setUp() and tearDown being run between each method
	// to ensure garbage collector and isolate each method.
	// But here eachtest methods depends depends on the previous one

	private Logger logger = Logger.getLogger(this.getClass().getName());
	File srcdir = new File(Environment.getDataDirectory() + "/data/com.orangelabs.rcs/databases");
	File[] list = null;

	protected void setUp() throws Exception {
		logger.info("Set up start");
		super.setUp();
		logger.info("Set up end");
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testBackupAccount() {
		logger.info("testBackupRestoreDb start");
		assertTrue(BackupRestoreDb.backupAccount("1111"));
		assertTrue(BackupRestoreDb.backupAccount("2222"));
		assertTrue(BackupRestoreDb.backupAccount("3333"));
		assertTrue(BackupRestoreDb.backupAccount("4444"));
		try {
			list = BackupRestoreDb.listOfSavedAccounts(srcdir);
			for (File file : list) {
				logger.info("account " + file);
			}
		} catch (InvalidArgumentException e) {
			e.printStackTrace();
		}
		assertTrue("listOfSavedAccounts failed", list != null && list.length == 4);
		assertTrue("getOldestFile failed", FileUtils.getOldestFile(list).getName().equals("1111"));
	}

	public void testCleanBackups() {
		// this is to test that the backup this saved account directory has to be kept and
		// that the total number of saved accounts does not exceed 3
		BackupRestoreDb.cleanBackups("3333");
		list = null;
		try {
			list = BackupRestoreDb.listOfSavedAccounts(srcdir);
			for (File file : list) {
				logger.info("account " + file);
			}
		} catch (InvalidArgumentException e) {
			e.printStackTrace();
		}
		assertTrue("listOfSavedAccounts failed", list != null && list.length == 3);
		assertTrue("getOldestFile failed", FileUtils.getOldestFile(list).getName().equals("2222"));
	}

	public void testRestoreDb() {
		assertTrue("restoreAccountProviders failed", BackupRestoreDb.restoreAccount("2222"));
		try {
			FileUtils.deleteDirectory(new File(srcdir, "2222"));
			FileUtils.deleteDirectory(new File(srcdir, "3333"));
			FileUtils.deleteDirectory(new File(srcdir, "4444"));
		} catch (InvalidArgumentException e) {
			e.printStackTrace();
		}
		try {
			list = BackupRestoreDb.listOfSavedAccounts(srcdir);
			assertTrue("listOfSavedAccounts failed", list == null || list.length == 0);
		} catch (InvalidArgumentException e) {
			e.printStackTrace();
		}
		list = null;
		logger.info("testBackupRestoreDb end");
	}

}
