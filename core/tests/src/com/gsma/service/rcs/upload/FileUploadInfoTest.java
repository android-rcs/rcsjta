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

package com.gsma.service.rcs.upload;

import java.io.File;
import java.util.Random;

import android.net.Uri;
import android.os.Parcel;
import android.test.AndroidTestCase;

import com.gsma.services.rcs.upload.FileUploadInfo;

/**
 * @author Danielle Rouquier
 */
public class FileUploadInfoTest extends AndroidTestCase {
    File tFilef = new File("tFileName");

    File tIconFileName = new File("tIconFileName");

    Uri tFile;

    long tValidity;

    long tSize;

    String tFileName;

    String tMimeType;

    Uri tFileIcon;

    String tIconMimeType;

    long tFileIconVal;

    long tFileIconSize;

    protected void setUp() throws Exception {
        Random random = new Random();
        tValidity = random.nextLong();
        tFileIconVal = random.nextLong();
        tSize = random.nextLong();
        tFileName = String.valueOf(random.nextInt(96) + 32);
        tMimeType = String.valueOf(random.nextInt(96) + 32);
        tIconMimeType = String.valueOf(random.nextInt(96) + 32);
        tFileIconSize = random.nextLong();
        tFile = Uri.fromFile(tFilef);
        tFileIcon = Uri.fromFile(tIconFileName);
    }

    public void testFileUploadInfoNull() {
        Exception ex = null;
        FileUploadInfo tfileUploadInfo = new FileUploadInfo(null, tValidity, tFileName, tSize,
                tMimeType, tFileIcon, tFileIconVal, tFileIconSize, tIconMimeType);
        // the constructor here issues no exception
        try {
            Parcel parcel = Parcel.obtain();
            tfileUploadInfo.writeToParcel(parcel, 0);
            // done writing, now reset parcel for reading
            parcel.setDataPosition(0);
            // finish round trip
            FileUploadInfo.CREATOR.createFromParcel(parcel);
        } catch (Exception e) {
            ex = e;
        }
        assertTrue("File to upload URI should be not null", (ex != null));
    }

    public void testFileUploadInfo() {
        FileUploadInfo tfileUploadInfo = new FileUploadInfo(tFile, tValidity, tFileName, tSize,
                tMimeType, tFileIcon, tFileIconVal, tFileIconSize, tIconMimeType);
        Parcel parcel = Parcel.obtain();
        tfileUploadInfo.writeToParcel(parcel, 0);
        // done writing, now reset parcel for reading
        parcel.setDataPosition(0);
        // finish round trip
        FileUploadInfo createFromParcel = FileUploadInfo.CREATOR.createFromParcel(parcel);
        assertEquals(createFromParcel.getFile(), tfileUploadInfo.getFile());
        assertEquals(createFromParcel.getExpiration(), tfileUploadInfo.getExpiration());
        assertEquals(createFromParcel.getFileName(), tfileUploadInfo.getFileName());
        assertEquals(createFromParcel.getSize(), tfileUploadInfo.getSize());
        assertEquals(createFromParcel.getMimeType(), tfileUploadInfo.getMimeType());
        assertEquals(createFromParcel.getFileIcon(), tfileUploadInfo.getFileIcon());
        assertEquals(createFromParcel.getFileIconExpiration(),
                tfileUploadInfo.getFileIconExpiration());
        assertEquals(createFromParcel.getFileIconMimeType(), tfileUploadInfo.getFileIconMimeType());
        assertEquals(createFromParcel.getFileIconSize(), tfileUploadInfo.getFileIconSize());
    }

    /**
     * @throws java.lang.Exception
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

}
