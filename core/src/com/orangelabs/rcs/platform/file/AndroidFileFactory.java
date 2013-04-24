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

package com.orangelabs.rcs.platform.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;

import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.utils.logger.Logger;

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
	 * Open a file input stream
	 * 
	 * @param url URL
	 * @return Input stream
	 * @throws IOException
	 */
	public InputStream openFileInputStream(String url) throws IOException {
		File file = new File(url);
		return new FileInputStream(file);
	}

	/**
	 * Open a file output stream
	 * 
	 * @param url URL
	 * @return Output stream
	 * @throws IOException
	 */
	public OutputStream openFileOutputStream(String url) throws IOException {
		File file = new File(url);
		return new FileOutputStream(file);
	}
	
	/**
	 * Returns the description of a file
	 * 
	 * @param url URL of the file
	 * @return File description
	 * @throws IOException
	 */
	public FileDescription getFileDescription(String url) throws IOException {
		File file = new File(url);
		if (file.isDirectory()) {
			return new FileDescription(url, -1L, true);
		} else {
			return new FileDescription(url, file.length(), false);
		}
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
