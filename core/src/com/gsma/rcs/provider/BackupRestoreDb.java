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

package com.gsma.rcs.provider;

import com.gsma.rcs.addressbook.RcsAccountException;
import com.gsma.rcs.utils.FileUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsServiceControl;

import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;

/**
 * Backup and restore databases
 * 
 * @author YPLO6403
 */
public class BackupRestoreDb {

    /**
     * The location of the database
     */
    public static final String DATABASE_LOCATION = new StringBuilder("/data/")
            .append(RcsServiceControl.RCS_STACK_PACKAGENAME).append("/databases/").toString();

    /**
     * The maximum number of saved accounts
     */
    private static final int MAX_SAVED_ACCOUNT = 3;

    private static final Logger sLogger = Logger.getLogger(BackupRestoreDb.class.getSimpleName());

    /**
     * Filter to get database files
     */
    private static final FilenameFilter sFilenameDbFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
            return (filename.endsWith(".db"));
        }
    };

    /**
     * Get list of RCS accounts saved under the database directory
     * 
     * @param databasesDir the database directory
     * @return the array of RCS saved accounts (may be empty) or null
     * @throws InvalidArgumentException
     */
    public static File[] listOfSavedAccounts(final File databasesDir)
            throws IllegalArgumentException {
        if (!databasesDir.exists()) {
            throw new IllegalArgumentException(new StringBuilder("Argument '").append(databasesDir)
                    .append("' directory does not exist").toString());
        }
        if (!databasesDir.isDirectory()) {
            throw new IllegalArgumentException(new StringBuilder("Argument '").append(databasesDir)
                    .append("' is not a directory").toString());
        }
        FileFilter directoryFilter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                if (file.isDirectory()) {
                    // There must be at least 3 digits
                    return file.getName().matches("^\\d{3,}$");
                }
                return false;
            }
        };
        return databasesDir.listFiles(directoryFilter);
    }

    /**
     * Check the arguments of backup or restore methods
     * 
     * @param databasesDir the database directory
     * @param account the account
     * @return true is arguments are OK
     */
    private static boolean checkBackupRestoreArguments(final File databasesDir, final String account) {
        if (TextUtils.isEmpty(account)) {
            return false;
        }
        // There must be at least 3 digits
        if (account.matches("^\\d{3,}$") == false) {
            return false;
        }
        if (databasesDir == null || databasesDir.isDirectory() == false) {
            return false;
        }
        return true;
    }

    /**
     * Save databases under account directory
     * 
     * @param databasesDir the database directory
     * @param account the account
     * @throws IOException
     * @throws RcsAccountException
     */
    private static void saveAccountDatabases(final File databasesDir, final String account)
            throws IOException, RcsAccountException {
        if (sLogger.isActivated()) {
            sLogger.info("saveAccountDatabases account=".concat(account));
        }
        if (checkBackupRestoreArguments(databasesDir, account) == false) {
            throw new RcsAccountException("Cannot save account ".concat(account));
        }
        String[] listOfDbFiles = databasesDir.list(sFilenameDbFilter);
        if (listOfDbFiles == null || listOfDbFiles.length <= 0) {
            throw new RcsAccountException("No DB files to save for ".concat(account));
        }
        File dstDir = new File(databasesDir, account);
        for (String dbFile : listOfDbFiles) {
            File srcFile = new File(databasesDir, dbFile);
            FileUtils.copyFileToDirectory(srcFile, dstDir, true);
            if (sLogger.isActivated()) {
                sLogger.info(new StringBuilder("Save file '").append(srcFile).append("' to '")
                        .append(dstDir).append("'").toString());
            }
        }
        dstDir.setLastModified(System.currentTimeMillis());
    }

    /**
     * Restore databases from account directory
     * 
     * @param databasesDir the database directory
     * @param account the account
     * @throws IOException
     * @throws RcsAccountException
     */
    private static void restoreAccountDatabases(final File databasesDir, final String account)
            throws IOException, RcsAccountException {
        if (checkBackupRestoreArguments(databasesDir, account) == false) {
            throw new RcsAccountException("Cannot save account ".concat(account));

        }
        File srcDir = new File(databasesDir, account);
        String[] listOfDbFiles = srcDir.list(sFilenameDbFilter);
        if (listOfDbFiles == null || listOfDbFiles.length <= 0) {
            throw new RcsAccountException("No DB files to restore for ".concat(account));
        }
        for (String dbFile : listOfDbFiles) {
            File srcFile = new File(srcDir, dbFile);
            FileUtils.copyFileToDirectory(srcFile, databasesDir, true);
        }
    }

    /**
     * Suppress oldest backup if more than MAX_SAVED_ACCOUNT
     * 
     * @param databasesDir the database directory
     * @param currentUserAccount the account
     * @throws IllegalArgumentException
     */
    private static void cleanBackups(final File databasesDir, final String currentUserAccount)
            throws IllegalArgumentException {
        File[] files = listOfSavedAccounts(databasesDir);
        if (files == null || files.length <= MAX_SAVED_ACCOUNT) {
            // No need to suppress oldest backup
            return;
        }

        File file = FileUtils.getOldestFile(files);
        if (!file.getName().equals(currentUserAccount)) {
            FileUtils.deleteDirectory(file);
            if (sLogger.isActivated()) {
                sLogger.debug("Clean oldest Backup : account=".concat(file.getName()));
            }
            return;

        }
        // Do not clean current account
        File[] filesWithoutCurrentAccount = new File[files.length - 1];
        int i = 0;
        for (File file2 : files) {
            if (file2.getName().equals(currentUserAccount) == false) {
                filesWithoutCurrentAccount[i++] = file2;
            }
        }
        file = FileUtils.getOldestFile(filesWithoutCurrentAccount);
        FileUtils.deleteDirectory(file);
        if (sLogger.isActivated()) {
            sLogger.debug("Clean oldest Backup : account=".concat(file.getName()));
        }
    }

    /**
     * Suppress oldest backup
     * 
     * @param currentUserAccount the current account must not be cleaned
     */
    public static void cleanBackups(String currentUserAccount) {
        cleanBackups(
                new File(new StringBuilder(Environment.getDataDirectory().toString()).append(
                        DATABASE_LOCATION).toString()), currentUserAccount);
    }

    /**
     * Backup account
     * 
     * @param account the Account to backup
     * @throws IOException
     * @throws RcsAccountException
     */
    public static void backupAccount(String account) throws IOException, RcsAccountException {
        saveAccountDatabases(new File(new StringBuilder(Environment.getDataDirectory().toString())
                .append(DATABASE_LOCATION).toString()), account);
    }

    /**
     * Restore account
     * 
     * @param account Account
     * @throws IOException
     * @throws RcsAccountException
     */
    public static void restoreAccount(String account) throws IOException, RcsAccountException {
        restoreAccountDatabases(new File(new StringBuilder(Environment.getDataDirectory()
                .toString()).append(DATABASE_LOCATION).toString()), account);
    }
}
