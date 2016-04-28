/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Backup and restore of user profile information
 * 
 * @author YPLO6403
 */
public class UserProfilePersistedStorageUtil {

    private static final String DB_FILE_EXTENSION = ".db";

    /**
     * Pattern to check if rcs account have atleast 3 characters.
     */
    private static final Pattern RCS_ACCOUNT_MATCH_PATTERN = Pattern.compile("^\\d{3,}$");

    /**
     * The location of the database
     */
    public static final String DATABASE_LOCATION = "/data/"
            + RcsServiceControl.RCS_STACK_PACKAGENAME + "/databases/";

    /**
     * The maximum number of saved accounts
     */
    private static final int MAX_SAVED_ACCOUNT = 3;

    private static final Logger sLogger = Logger.getLogger(UserProfilePersistedStorageUtil.class
            .getSimpleName());

    /**
     * Filter to get database files
     */
    private static final FilenameFilter sFilenameDbFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
            return (filename.endsWith(DB_FILE_EXTENSION));
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
     * Save user account profile
     * 
     * @param databasesDir the database directory
     * @param account the account
     * @throws IOException
     */
    private static void saveUserProfile(final File databasesDir, final String account)
            throws IOException {
        if (sLogger.isActivated()) {
            sLogger.info("saveAccountDatabases account=".concat(account));
        }
        String[] listOfDbFiles = databasesDir.list(sFilenameDbFilter);
        if (listOfDbFiles == null) {
            throw new FileNotFoundException("Failed to find " + DB_FILE_EXTENSION + " files at : "
                    + databasesDir.getPath());
        }
        File dstDir = new File(databasesDir, account);
        for (String dbFile : listOfDbFiles) {
            File srcFile = new File(databasesDir, dbFile);
            FileUtils.copyFileToDirectory(srcFile, dstDir, true);
            if (sLogger.isActivated()) {
                sLogger.info("Save file '" + srcFile + "' to '" + dstDir + "'");
            }
        }
        // noinspection ResultOfMethodCallIgnored
        dstDir.setLastModified(System.currentTimeMillis());
    }

    /**
     * Restore user account profile
     * 
     * @param databasesDir the database directory
     * @param account the account
     * @throws IOException
     */
    private static void restoreUserProfile(final File databasesDir, final String account)
            throws IOException {
        File srcDir = new File(databasesDir, account);
        String[] listOfDbFiles = srcDir.list(sFilenameDbFilter);
        if (listOfDbFiles == null) {
            throw new FileNotFoundException("Failed to find " + DB_FILE_EXTENSION + " files at : "
                    + databasesDir.getPath());
        }
        for (String dbFile : listOfDbFiles) {
            File srcFile = new File(srcDir, dbFile);
            FileUtils.copyFileToDirectory(srcFile, databasesDir, true);
        }
    }

    /**
     * Normalize file backup and manages old user files if not more then MAX_SAVED_ACCOUNT
     * 
     * @param databasesDir the database directory
     * @param currentUserAccount the account
     * @throws IOException
     */
    private static void normalizeFileBackup(final File databasesDir, final String currentUserAccount)
            throws IOException {
        File[] files = listOfSavedAccounts(databasesDir);
        if (files == null || files.length <= MAX_SAVED_ACCOUNT) {
            /* No need to formalize backup files as we havn't reach max saved account limit yet */
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
            if (!file2.getName().equals(currentUserAccount)) {
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
     * Normalize file backup and manages old user files if not more then MAX_SAVED_ACCOUNT
     * 
     * @param currentUserAccount the account
     * @throws IOException
     */
    public static void normalizeFileBackup(String currentUserAccount) throws IOException {
        normalizeFileBackup(
                new File(Environment.getDataDirectory().toString() + DATABASE_LOCATION),
                currentUserAccount);
    }

    /**
     * Try to backup user account.
     * <p>
     * As this method makes an attempt to backup user account any failure that may occur while
     * backing up will not be propagated to the caller function.
     * </p>
     * 
     * @param account the Account to backup
     */
    public static void tryToBackupAccount(String account) {
        File userProfile = new File(Environment.getDataDirectory().toString() + DATABASE_LOCATION);
        try {
            saveUserProfile(userProfile, account);
        } catch (FileNotFoundException e) {
            /*
             * If the file that needs to be backed up does not exist then we can't proceed with this
             * operation, so we log this incident and continue with the rest of the flow as backing
             * up account is not a mandatory feature that needs full success.
             */
            if (sLogger.isActivated()) {
                sLogger.debug(e.getMessage());
            }

        } catch (IOException e) {
            if (sLogger.isActivated()) {
                sLogger.debug(e.getMessage());
            }
            /*
             * Since we have failed to do full backup and there is a chance that we might have
             * partial copy of original files, Hence we should remove those files and also the files
             * under DATABASE_LOCATION , as anyway we don't have a chance to back those after sim
             * swap happens.
             */
            /* Remove all directories and files those are restored so far */
            try {
                FileUtils.deleteDirectory(new File(userProfile, account));
            } catch (IOException ex) {
                if (sLogger.isActivated()) {
                    sLogger.debug(ex.getMessage());
                }
            }
            /* Remove directories and files under DATABASE_LOCATION */
            try {
                FileUtils.deleteDirectory(userProfile);
            } catch (IOException ex) {
                if (sLogger.isActivated()) {
                    sLogger.debug(ex.getMessage());
                }
            }
        }
    }

    /**
     * Try to restore user account
     * <p>
     * As this method makes an attempt to restore user account any failure that may occur while
     * restoring will not be propagated to the caller function.
     * </p>
     * 
     * @param account Account
     */
    public static void tryToRestoreAccount(String account) {
        File userProfile = new File(Environment.getDataDirectory().toString() + DATABASE_LOCATION);
        try {
            restoreUserProfile(userProfile, account);
        } catch (FileNotFoundException e) {
            /*
             * If the file that needs to be backed up does not exist then we can't proceed with this
             * operation, so we log this incident and continue with the rest of the flow as backing
             * up account is not a mandatory feature that needs full success.
             */
            if (sLogger.isActivated()) {
                sLogger.debug(e.getMessage());
            }

        } catch (IOException e) {
            if (sLogger.isActivated()) {
                sLogger.debug(e.getMessage());
            }
            /*
             * Since we have failed to do full restore and there is a chance that we might have
             * partial copy of original files, Hence we should remove those files and also the files
             * under DATABASE_LOCATION , as anyway we don't have a chance to back those after sim
             * swap happens.
             */
            /* Remove all directories and files those are restored so far */
            try {
                FileUtils.deleteDirectory(new File(userProfile, account));
            } catch (IOException ex) {
                if (sLogger.isActivated()) {
                    sLogger.debug(ex.getMessage());
                }
            }
            /* Remove directories and files under DATABASE_LOCATION */
            try {
                FileUtils.deleteDirectory(userProfile);
            } catch (IOException ex) {
                if (sLogger.isActivated()) {
                    sLogger.debug(ex.getMessage());
                }
            }
        }
    }
}
