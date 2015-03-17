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

package com.gsma.rcs.core.ims.service.im.filetransfer.http;

import org.xml.sax.helpers.DefaultHandler;

import android.net.Uri;

/**
 * File transfer over HTTP info document
 * 
 * @author hhff3235
 */
public class FileTransferHttpResumeInfo extends DefaultHandler {
    /**
     * start-offset in bytes
     */
    private int mStart = 0;

    /**
     * end-offset in bytes
     */
    private int mEnd = 0;

    /**
     * HTTP upload Uri for the file
     */
    private Uri mFile;

    /**
     * @return the start
     */
    protected int getStart() {
        return mStart;
    }

    /**
     * @param start the start to set
     */
    protected void setStart(int start) {
        mStart = start;
    }

    /**
     * @return the end
     */
    protected int getEnd() {
        return mEnd;
    }

    /**
     * @param end the end to set
     */
    protected void setEnd(int end) {
        mEnd = end;
    }

    /**
     * @return the Uri
     */
    protected Uri getUri() {
        return mFile;
    }

    /**
     * @param file the Uri to set
     */
    protected void setUri(Uri file) {
        mFile = file;
    }

}
