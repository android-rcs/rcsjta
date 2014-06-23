/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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

package com.orangelabs.rcs.platform.file;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;

import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.utils.logger.Logger;
import com.orangelabs.rcs.platform.file.FileDescription;
import com.orangelabs.rcs.utils.FileUtils;

/**
 * Android file factory
 * 
 * @author jexa7410
 */
public class AndroidFileFactory extends FileFactory {
	/**
	 * The logger
	 */
	private Logger logger = Logger.getLogger(this.getClass().getName());

	/**
	 * Returns the description of a file
	 *
	 * @param file URI of the file
	 * @return File description
	 * @throws IOException
	 */
	public FileDescription getFileDescription(Uri file) throws IOException {
		Context context = AndroidFactory.getApplicationContext();
		String fileName = FileUtils.getFileName(context, file);
		long fileSize = FileUtils.getFileSize(context, file);
		return new FileDescription(file, fileName, fileSize);
	}
	
	/**
	 * Returns whether a file exists or not
	 * 
	 * @param url Url of the file to check
	 * @return File existence
	 */
	public boolean fileExists(String url){
		File file = new File(url);
		return file.exists();
	}

	/**
	 * Update the media storage
	 * 
	 * @param url New URL to be added
	 */
	public void updateMediaStorage(String url) {
		if (logger.isActivated()) {
			logger.debug("Updating media storage with URL " + url);
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
			if (logger.isActivated()) {
				logger.debug("Scanning file " + filename);
			}
			scanner.scanFile(filename, null);
		}
		
		public void onScanCompleted(String path, Uri uri) {
			if (logger.isActivated()) {
				logger.debug("Scan completed for uri " + uri + " with path " + path);
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
