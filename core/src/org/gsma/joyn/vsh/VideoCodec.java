package org.gsma.joyn.vsh;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Video codec maintains all the information related to a video codec.
 * 
 * @author Jean-Marc AUFFRET
 */
public class VideoCodec implements Parcelable {
	/**
	 * Constructor
	 * 
	 * @param source Parcelable source
	 */
	public VideoCodec(Parcel source) {
		// TODO
    }

	/**
	 * Describe the kinds of special objects contained in this Parcelable's
	 * marshalled representation
	 * 
	 * @return Integer
	 */
	public int describeContents() {
        return 0;
    }

	/**
	 * Write parcelable object
	 * 
	 * @param dest The Parcel in which the object should be written
	 * @param flags Additional flags about how the object should be written
	 */
    public void writeToParcel(Parcel dest, int flags) {
    	// TODO
    }

    /**
     * Parcelable creator
     */
    public static final Parcelable.Creator<VideoCodec> CREATOR
            = new Parcelable.Creator<VideoCodec>() {
        public VideoCodec createFromParcel(Parcel source) {
            return new VideoCodec(source);
        }

        public VideoCodec[] newArray(int size) {
            return new VideoCodec[size];
        }
    };	

    /**
    * Returns the encoding name (e.g. H264)
    * 
    * @return Encoding name
    */
    public String getEncoding() {
    	// TODO
    	return null;
    }
    
    /**
     * Returns the codec payload (e.g. 96)
     * 
     * @return Payload number
     */
    public int getPayload() {
    	return -1; // TODO
    }
    
    /**
     * Returns the codec clock rate (e.g. 90000)
     * 
     * @return Clock rate
     */
    public int getClockRate() {
    	return -1; // TODO
    }
    
    /**
     * Returns the codec frame rate (e.g. 10)
     * 
     * @return Frame rate
     */
    public int getFrameRate() {
    	return -1; // TODO
    }
    
    /**
     * Returns the codec bit rate (e.g. 64000)
     * 
     * @return Bit rate
     */
    public int getBitRate() {
    	return -1; // TODO
    }
    
    /**
     * Returns the video width (e.g. 176)
     * 
     * @return Video width
     */
    public int getVideoWidth() {
    	return -1; // TODO: replace by a format WxH
    }
    
    /**
     * Returns the video height (e.g. 144)
     * 
     * @return Video height
     */
    public int getVideoHeight() {
    	return -1; // TODO
    }
    
    /**
     * Returns a codec parameter from its key name (e.g. profile-level-id, packetization-mode).
     * 
     * @param key Key name of the parameter
     * @return Parameter value
     */
    public String getParameter(String key) {
    	return null; // TODO    	
    }
}
