/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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
 ******************************************************************************/

package com.gsma.services.rcs.sharing.video;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Video descriptor for the default video player
 * 
 * @author Jean-Marc AUFFRET
 */
public class VideoDescriptor implements Parcelable {
    /**
     * Screen width
     */
    private final int mWidth;

    /**
     * Screen height
     */
    private final int mHeight;

    /**
     * Constructor
     * 
     * @param width Video width
     * @param height Video height
     * @hide
     */
    public VideoDescriptor(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    /**
     * Constructor
     * 
     * @param source Parcelable source
     * @hide
     */
    public VideoDescriptor(Parcel source) {
        mWidth = source.readInt();
        mHeight = source.readInt();
    }

    /**
     * Describe the kinds of special objects contained in this Parcelable's marshalled
     * representation
     * 
     * @return Integer
     * @hide
     */
    public int describeContents() {
        return 0;
    }

    /**
     * Write parcelable object
     * 
     * @param dest The Parcel in which the object should be written
     * @param flags Additional flags about how the object should be written
     * @hide
     */
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mWidth);
        dest.writeInt(mHeight);
    }

    /**
     * Parcelable creator
     * 
     * @hide
     */
    public static final Parcelable.Creator<VideoDescriptor> CREATOR = new Parcelable.Creator<VideoDescriptor>() {
        public VideoDescriptor createFromParcel(Parcel source) {
            return new VideoDescriptor(source);
        }

        public VideoDescriptor[] newArray(int size) {
            return new VideoDescriptor[size];
        }
    };

    /**
     * Returns the video width (e.g. 176)
     * 
     * @return Video width
     */
    public int getWidth() {
        return mWidth;
    }

    /**
     * Returns the video height (e.g. 144)
     * 
     * @return Video height
     */
    public int getHeight() {
        return mHeight;
    }
}
