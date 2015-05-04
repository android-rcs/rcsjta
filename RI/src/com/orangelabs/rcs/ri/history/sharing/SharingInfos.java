package com.orangelabs.rcs.ri.history.sharing;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author yplo6403
 *
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
     * @param contact
     * @param filename
     * @param filesize
     * @param sate
     * @param direction
     * @param timestamp
     * @param duration
     */
    public SharingInfos(String contact, String filename, String filesize, String sate,
            String direction, String timestamp, String duration) {
        super();
        this.mContact = contact;
        this.mFilename = filename;
        this.mFilesize = filesize;
        this.mState = sate;
        this.mDirection = direction;
        this.mTimestamp = timestamp;
        this.mDuration = duration;
    }

    /**
     * Constructor
     * @param in
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
