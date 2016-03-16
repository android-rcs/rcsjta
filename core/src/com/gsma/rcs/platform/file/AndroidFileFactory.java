/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2014 Sony Mobile Communications AB.
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
 * NOTE: This file has been modified by Sony Mobile Communications AB.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.platform.file;

import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.utils.FileUtils;
import com.gsma.rcs.utils.logger.Logger;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;

import java.io.File;

/**
 * Android file factory
 * 
 * @author jexa7410
 */
public class AndroidFileFactory extends FileFactory {

    private static final Logger sLogger = Logger
            .getLogger(AndroidFileFactory.class.getSimpleName());

    /**
     * Returns the description of a file
     * 
     * @param file URI of the file
     * @return File description
     */
    public FileDescription getFileDescription(Uri file) {
        Context context = AndroidFactory.getApplicationContext();
        String fileName = FileUtils.getFileName(context, file);
        long fileSize = FileUtils.getFileSize(context, file);
        return new FileDescription(fileName, fileSize);
    }

    /**
     * Returns whether a file exists or not
     * 
     * @param url Url of the file to check
     * @return File existence
     */
    public boolean fileExists(String url) {
        File file = new File(url);
        return file.exists();
    }

    /**
     * Update the media storage
     * 
     * @param url New URL to be added
     */
    public void updateMediaStorage(String url) {
        if (sLogger.isActivated()) {
            sLogger.debug("Updating media storage with URL " + url);
        }
        MyMediaScannerClient scanner = new MyMediaScannerClient(url);
        scanner.scan();
    }

    /**
     * Media scanner
     */
    private class MyMediaScannerClient implements MediaScannerConnectionClient {
        private String filename;

        private MediaScannerConnection scanner;

        public MyMediaScannerClient(String filename) {
            this.filename = filename;
            this.scanner = new MediaScannerConnection(AndroidFactory.getApplicationContext(), this);
        }

        public void onMediaScannerConnected() {
            if (sLogger.isActivated()) {
                sLogger.debug("Scanning file " + filename);
            }
            scanner.scanFile(filename, null);
        }

        public void onScanCompleted(String path, Uri uri) {
            if (sLogger.isActivated()) {
                sLogger.debug("Scan completed for uri " + uri + " with path " + path);
            }
            if (path.equals(filename)) {
                scanner.disconnect();
            }
        }

        public void scan() {
            scanner.connect();
        }
    }
}
