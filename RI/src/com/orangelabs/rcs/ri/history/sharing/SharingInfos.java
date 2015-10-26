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

package com.orangelabs.rcs.ri.history.sharing;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author yplo6403
 */
public class SharingInfos implements Parcelable {

    private final String mContact;
    private final String mFilename;
    private final String mFilesize;
    private final String mState;
    private final String mDirection;
    private final String mTimestamp;
    private final String mDuration;

    /**
     * Constructor
     * 
     * @param contact contact ID
     * @param filename file name
     * @param filesize file size
     * @param state session state
     * @param direction direction
     * @param timestamp timestamp
     * @param duration duration
     */
    public SharingInfos(String contact, String filename, String filesize, String state,
            String direction, String timestamp, String duration) {
        super();
        mContact = contact;
        mFilename = filename;
        mFilesize = filesize;
        mState = state;
        mDirection = direction;
        mTimestamp = timestamp;
        mDuration = duration;
    }

    /**
     * Constructor
     * 
     * @param in parcel
     */
    public SharingInfos(Parcel in) {
        String[] values = new String[7];
        in.readStringArray(values);
        mContact = values[0];
        mFilename = values[1];
        mFilesize = values[2];
        mState = values[3];
        mDirection = values[4];
        mTimestamp = values[5];
        mDuration = values[6];
    }

    protected String getContact() {
        return mContact;
    }

    protected String getFilename() {
        return mFilename;
    }

    protected String getFilesize() {
        return mFilesize;
    }

    protected String getSate() {
        return mState;
    }

    protected String getDirection() {
        return mDirection;
    }

    protected String getTimestamp() {
        return mTimestamp;
    }

    protected String getDuration() {
        return mDuration;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[] {
                mContact, mFilename, mFilesize, mState, mDirection, mTimestamp, mDuration
        });
    }

    /**
     * 
     */
    public static final Parcelable.Creator<SharingInfos> CREATOR = new Parcelable.Creator<SharingInfos>() {
        @Override
        public SharingInfos createFromParcel(Parcel in) {
            return new SharingInfos(in);
        }

        @Override
        public SharingInfos[] newArray(int size) {
            return new SharingInfos[size];
        }
    };

}
