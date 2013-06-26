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
	 * Video encoding
	 */
	private String encoding;
	
	/**
	 * Payload
	 */
	private int payload;
	
	/**
	 * Clock rate
	 */
	private float clockRate;
	
	/**
	 * Frame rate
	 */
	private float frameRate;
	
	/**
	 * Bit rate
	 */
	private int bitRate;

	/**
	 * Screen width
	 */
	private int width;
	
	/**
	 * Screen height
	 */
	private int height;

	/**
	 * Video parameters
	 */
	private String parameters;
	
    /**
     * Constructor
     *
     * @param encoding Video encoding
     * @param payload Payload
     * @param clockRate Clock rate
     * @param framerate Frame rate
     * @param bitRate Bit rate
     * @param width Video width
     * @param height Video height
     * @param parameters Codec parameters
     */
    public VideoCodec(String encoding, int payload, float clockRate, float frameRate, int bitRate, int width, int height, String parameters) {
    	this.encoding = encoding;
    	this.payload = payload;
    	this.clockRate = clockRate;
    	this.frameRate = frameRate;
    	this.bitRate = bitRate;
    	this.width = width;
    	this.height = height;
    	this.parameters = parameters;
    }
    
    /**
	 * Constructor
	 * 
	 * @param source Parcelable source
	 */
	public VideoCodec(Parcel source) {
		this.encoding = source.readString();
    	this.payload = source.readInt();
    	this.clockRate = source.readFloat();
    	this.frameRate = source.readFloat();
    	this.bitRate = source.readInt();
    	this.width = source.readInt();
    	this.height = source.readInt();
		this.parameters = source.readString();
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
    	dest.writeString(encoding);
    	dest.writeInt(payload);
    	dest.writeFloat(clockRate);
    	dest.writeFloat(frameRate);
    	dest.writeInt(bitRate);
    	dest.writeInt(width);
    	dest.writeInt(height);
    	dest.writeString(parameters);
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
    	return encoding;
    }
    
    /**
     * Returns the codec payload (e.g. 96)
     * 
     * @return Payload number
     */
    public int getPayload() {
    	return payload;
    }
    
    /**
     * Returns the codec clock rate (e.g. 90000)
     * 
     * @return Clock rate
     */
    public float getClockRate() {
    	return clockRate;
    }
    
    /**
     * Returns the codec frame rate (e.g. 10)
     * 
     * @return Frame rate
     */
    public float getFrameRate() {
    	return frameRate;
    }
    
    /**
     * Returns the codec bit rate (e.g. 64000)
     * 
     * @return Bit rate
     */
    public int getBitRate() {
    	return bitRate;
    }
    
    /**
     * Returns the video width (e.g. 176)
     * 
     * @return Video width
     */
    public int getVideoWidth() {
    	return width;
    }
    
    /**
     * Returns the video height (e.g. 144)
     * 
     * @return Video height
     */
    public int getVideoHeight() {
    	return height;
    }
    
    /**
     * Returns the list of codec parameters (e.g. profile-level-id, packetization-mode).
     * Parameters are are semicolon separated.
     * 
     * @return Parameters
     */
    public String getParameters() {
    	return parameters;    	
    }
}
