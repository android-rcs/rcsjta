/*
 * Copyright (C) 2015 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.gsma.services.rcs.upload;

import com.gsma.services.rcs.upload.FileUpload.State;

import android.os.RemoteException;
import android.util.Log;

/**
 * File Upload Listener Implementation
 * 
 * @hide
 */
public class FileUploadListenerImpl extends IFileUploadListener.Stub {

    private final FileUploadListener mListener;

    private final static String LOG_TAG = FileUploadListenerImpl.class.getName();

    FileUploadListenerImpl(FileUploadListener listener) {
        mListener = listener;
    }

    public void onStateChanged(String uploadId, int state) {
        State rcsState;
        try {
            rcsState = State.valueOf(state);
        } catch (IllegalArgumentException e) {
            /*
             * Detected unknown state or reasonCode not part of standard coming from stack which a
             * client application can not handle since it is built only to handle the possible enum
             * values documented and specified in the api standard.
             */
            Log.e(LOG_TAG, e.getMessage());
            return;
        }
        mListener.onStateChanged(uploadId, rcsState);
    }

    public void onProgressUpdate(String uploadId, long currentSize, long totalSize)
            throws RemoteException {
        mListener.onProgressUpdate(uploadId, currentSize, totalSize);
    }

    public void onUploaded(String uploadId, FileUploadInfo info) throws RemoteException {
        mListener.onUploaded(uploadId, info);
    }
}
