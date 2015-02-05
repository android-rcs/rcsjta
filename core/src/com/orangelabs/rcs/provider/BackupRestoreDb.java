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

package com.orangelabs.rcs.provider;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.Date;

import javax2.sip.InvalidArgumentException;
import android.os.Environment;
import android.text.TextUtils;

import com.orangelabs.rcs.utils.FileUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Backup and restore databases
 * 
 * @author YPLO6403
 */
public class BackupRestoreDb {

    /**
     * The location of the database
     */
    public static final String DATABASE_LOCATION = "/data/com.orangelabs.rcs/databases/";;

    /**
     * The maximum number of saved accounts
     */
    private static final int MAX_SAVED_ACCOUNT = 3;

    /**
     * The logger
     */
    private static final Logger logger = Logger.getLogger(BackupRestoreDb.class.getSimpleName());

    /**
     * Filter to get database files
     */
    private static final FilenameFilter filenameDbFilter = new FilenameFilter() {
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
            throws InvalidArgumentException {
        if (databasesDir == null) {
            throw new InvalidArgumentException();
        }
        if (databasesDir.isDirectory() == false) {
            throw new InvalidArgumentException("Argument '" + databasesDir + "' is not a directory");
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
     * @return true if save succeeded
     */
    private static boolean saveAccountDatabases(final File databasesDir, final String account) {
        if (logger.isActivated()) {
            logger.info("saveAccountDatabases account=" + account);
        }
        if (checkBackupRestoreArguments(databasesDir, account) == false) {
            if (logger.isActivated()) {
                logger.error("Cannot save account " + account);
            }
            return false;
        }
        // Put the names of all files ending with .db in a String array
        String[] listOfDbFiles = databasesDir.list(filenameDbFilter);
        if (listOfDbFiles != null && listOfDbFiles.length > 0) {
            File dstDir = new File(databasesDir, account);
            // Iterate over the array of database file names
            for (String dbFile : listOfDbFiles) {
                // Create file to be saved
                File srcFile = new File(databasesDir, dbFile);
                try {
                    // Copy database file under account directory
                    FileUtils.copyFileToDirectory(srcFile, dstDir, true);
                    if (logger.isActivated()) {
                        logger.info("Save file" + srcFile + " to " + dstDir);
                    }
                } catch (Exception e) {
                    if (logger.isActivated()) {
                        logger.error(e.getMessage(), e);
                    }
                    return false;
                }
            }
            // update the saved account db directory date with the backup date
            dstDir.setLastModified((new Date()).getTime());
            return true;
        }
        return false;
    }

    /**
     * Restore databases from account directory
     * 
     * @param databasesDir the database directory
     * @param account the account
     * @return true if restore succeeded
     */
    private static boolean restoreAccountDatabases(final File databasesDir, final String account) {
        if (checkBackupRestoreArguments(databasesDir, account) == false) {
            return false;
        }
        File srcDir = new File(databasesDir, account);
        // Put the names of all files ending with .db in a String array
        String[] listOfDbFiles = srcDir.list(filenameDbFilter);
        if (listOfDbFiles != null && listOfDbFiles.length > 0) {
            // Iterate over the array of database file names
            for (String dbFile : listOfDbFiles) {
                // Create file to be restored
                File srcFile = new File(srcDir, dbFile);
                try {
                    // Copy database file under database directory
                    FileUtils.copyFileToDirectory(srcFile, databasesDir, true);
                } catch (Exception e) {
                    if (logger.isActivated()) {
                        logger.error(e.getMessage(), e);
                    }
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Suppress oldest backup if more than MAX_SAVED_ACCOUNT
     * 
     * @param databasesDir the database directory
     * @param currentUserAccount the account
     * @throws InvalidArgumentException
     */
    private static void cleanBackups(final File databasesDir, final String currentUserAccount)
            throws InvalidArgumentException {
        File[] files = listOfSavedAccounts(databasesDir);
        if (files != null && files.length > MAX_SAVED_ACCOUNT) {

            File file = FileUtils.getOldestFile(files);
            if (file.getName().equals(currentUserAccount) == false) {
                FileUtils.deleteDirectory(file);
                if (logger.isActivated()) {
                    logger.debug("Clean oldest Backup : account=" + file.getName());
                }
            } else {
                // Do not clean current account
                File[] _files = new File[files.length - 1];
                int i = 0;
                for (File file2 : files) {
                    if (file2.getName().equals(currentUserAccount) == false) {
                        _files[i++] = file2;
                    }
                }
                file = FileUtils.getOldestFile(_files);
                FileUtils.deleteDirectory(file);
                if (logger.isActivated()) {
                    logger.debug("Clean oldest Backup : account=" + file.getName());
                }
            }
        }
    }

    /**
     * Suppress oldest backup
     * 
     * @param currentUserAccount the current account must not be cleaned
     */
    public static void cleanBackups(String currentUserAccount) {
        String accountDBs = Environment.getDataDirectory() + DATABASE_LOCATION;
        try {
            File srcdir = new File(accountDBs);
            cleanBackups(srcdir, currentUserAccount);
        } catch (Exception e) {
            if (logger.isActivated())
                logger.error(e.getMessage(), e);
        }
    }

    /**
     * Backup account
     * 
     * @param account the Account to backup
     * @return true if backup is successful
     */
    public static boolean backupAccount(String account) {
        String accountDBs = Environment.getDataDirectory() + DATABASE_LOCATION;
        try {
            File fdir = new File(accountDBs);
            return saveAccountDatabases(fdir, account);
        } catch (Exception e) {
            if (logger.isActivated())
                logger.error(e.getMessage(), e);
        }
        return false;
    }

    /**
     * Restore account
     * 
     * @param account Account
     * @return true if restore is successful
     */
    public static boolean restoreAccount(String account) {
        String accountDBs = Environment.getDataDirectory() + DATABASE_LOCATION;
        try {
            File fdir = new File(accountDBs);
            return restoreAccountDatabases(fdir, account);
        } catch (Exception e) {
            if (logger.isActivated())
                logger.error(e.getMessage(), e);
        }
        return false;
    }
}
