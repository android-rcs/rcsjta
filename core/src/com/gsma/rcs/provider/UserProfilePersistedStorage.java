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

import com.gsma.rcs.utils.FileUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsServiceControl;

import android.os.Environment;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Backup and restore of user profile information
 * 
 * @author YPLO6403
 */
public class UserProfilePersistedStorage {

    /**
     * Pattern to check if rcs account have atleast 3 characters.
     */
    private static final Pattern RCS_ACCOUNT_MATCH_PATTERN = Pattern.compile("^\\d{3,}$");

    /**
     * The location of the database
     */
    public static final String DATABASE_LOCATION = new StringBuilder("/data/")
            .append(RcsServiceControl.RCS_STACK_PACKAGENAME).append("/databases/").toString();

    /**
     * The maximum number of saved accounts
     */
    private static final int MAX_SAVED_ACCOUNT = 3;

    private static final Logger sLogger = Logger.getLogger(UserProfilePersistedStorage.class
            .getSimpleName());

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
     */
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
            @Override
            public boolean accept(File file) {
                return file.isDirectory()
                        && RCS_ACCOUNT_MATCH_PATTERN.matcher(file.getName()).matches();
            }
        };
        return databasesDir.listFiles(directoryFilter);
    }

    /**
     * Returns true if valid account and database directory
     * 
     * @param databasesDir the database directory
     * @param account the account
     */
    private static boolean isValidAccountAndDatabaseDirectory(final File databasesDir,
            final String account) {
        return RCS_ACCOUNT_MATCH_PATTERN.matcher(account).matches() && databasesDir.isDirectory();
    }

    /**
     * Save user account profile
     * 
     * @param databasesDir the database directory
     * @param account the account
     */
    private static void saveUserProfile(final File databasesDir, final String account) {
        try {
            if (sLogger.isActivated()) {
                sLogger.info("saveAccountDatabases account=".concat(account));
            }
            if (isValidAccountAndDatabaseDirectory(databasesDir, account)) {
                String[] listOfDbFiles = databasesDir.list(sFilenameDbFilter);
                if (listOfDbFiles != null) {
                    File dstDir = new File(databasesDir, account);
                    for (String dbFile : listOfDbFiles) {
                        File srcFile = new File(databasesDir, dbFile);
                        FileUtils.copyFileToDirectory(srcFile, dstDir, true);
                        if (sLogger.isActivated()) {
                            sLogger.info(new StringBuilder("Save file '").append(srcFile)
                                    .append("' to '").append(dstDir).append("'").toString());
                        }
                    }
                    dstDir.setLastModified(System.currentTimeMillis());
                }
            }
        } catch (IOException e) {
            if (sLogger.isActivated()) {
                sLogger.debug(e.getMessage());
            }
        }
    }

    /**
     * Restore user account profile
     * 
     * @param databasesDir the database directory
     * @param account the account
     */
    private static void restoreUserProfile(final File databasesDir, final String account) {
        try {
            if (isValidAccountAndDatabaseDirectory(databasesDir, account)) {
                File srcDir = new File(databasesDir, account);
                String[] listOfDbFiles = srcDir.list(sFilenameDbFilter);
                if (listOfDbFiles != null) {
                    for (String dbFile : listOfDbFiles) {
                        File srcFile = new File(srcDir, dbFile);
                        FileUtils.copyFileToDirectory(srcFile, databasesDir, true);
                    }
                }
            }
        } catch (IOException e) {
            if (sLogger.isActivated()) {
                sLogger.debug(e.getMessage());
            }
        }
    }

    /**
     * Suppress oldest backup if more than MAX_SAVED_ACCOUNT
     * 
     * @param databasesDir the database directory
     * @param currentUserAccount the account
     */
    private static void cleanBackups(final File databasesDir, final String currentUserAccount) {
        File[] files = listOfSavedAccounts(databasesDir);
        if (files == null || files.length <= MAX_SAVED_ACCOUNT) {
            /* No need to suppress oldest backup */
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
        /* Do not clean current account */
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
     */
    public static void backupAccount(String account) {
        saveUserProfile(new File(new StringBuilder(Environment.getDataDirectory().toString())
                .append(DATABASE_LOCATION).toString()), account);
    }

    /**
     * Restore account
     * 
     * @param account Account
     */
    public static void restoreAccount(String account) {
        restoreUserProfile(new File(new StringBuilder(Environment.getDataDirectory().toString())
                .append(DATABASE_LOCATION).toString()), account);
    }
}
