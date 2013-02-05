/*
 * Copyright 2013, France Telecom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gsma.rcs.core.ims.protocol.rtp.util;

/**
 * Class Buffer.
 */
public class Buffer {
    /**
     * Constant FLAG_EOM.
     */
    public static final int FLAG_EOM = 1;

    /**
     * Constant FLAG_DISCARD.
     */
    public static final int FLAG_DISCARD = 2;

    /**
     * Constant FLAG_RTP_MARKER.
     */
    public static final int FLAG_RTP_MARKER = 2048;

    /**
     * Constant FLAG_RTP_TIME.
     */
    public static final int FLAG_RTP_TIME = 4096;

    /**
     * Constant TIME_UNKNOWN.
     */
    public static final long TIME_UNKNOWN = -1l;

    /**
     * Constant SEQUENCE_UNKNOWN.
     */
    public static final long SEQUENCE_UNKNOWN = 9223372036854775806l;

    /**
     * The time stamp.
     */
    protected long timeStamp;

    /**
     * The format.
     */
    protected org.gsma.rcs.core.ims.protocol.rtp.format.Format format;

    /**
     * The length.
     */
    protected int length;

    /**
     * The offset.
     */
    protected int offset;

    /**
     * The flags.
     */
    protected int flags;

    /**
     * The duration.
     */
    protected long duration;

    /**
     * The data.
     */
    protected Object data;

    /**
     * The sequence number.
     */
    protected long sequenceNumber;

    /**
     * The fragments array.
     */
    protected Buffer[] fragments;

    /**
     * The fragments size.
     */
    protected int fragmentsSize;

    /**
     * Creates a new instance of Buffer.
     */
    public Buffer() {

    }

    /**
     * Returns the length.
     *  
     * @return  The length.
     */
    public int getLength() {
        return 0;
    }

    /**
     * Sets the length.
     *  
     * @param arg1 The length.
     */
    public void setLength(int arg1) {

    }

    /**
     * Returns the offset.
     *  
     * @return  The offset.
     */
    public int getOffset() {
        return 0;
    }

    /**
     * Sets the offset.
     *  
     * @param arg1 The offset.
     */
    public void setOffset(int arg1) {

    }

    /**
     * Returns the data.
     *  
     * @return  The data.
     */
    public Object getData() {
        return (java.lang.Object) null;
    }

    /**
     * Sets the discard.
     *  
     * @param arg1 The discard.
     */
    public void setDiscard(boolean arg1) {

    }

    /**
     * Returns the format.
     *  
     * @return  The format.
     */
    public org.gsma.rcs.core.ims.protocol.rtp.format.Format getFormat() {
        return (org.gsma.rcs.core.ims.protocol.rtp.format.Format) null;
    }

    /**
     * Returns the duration.
     *  
     * @return  The duration.
     */
    public long getDuration() {
        return 0l;
    }

    /**
     * Sets the duration.
     *  
     * @param arg1 The duration.
     */
    public void setDuration(long arg1) {

    }

    /**
     * Returns the time stamp.
     *  
     * @return  The time stamp.
     */
    public long getTimeStamp() {
        return 0l;
    }

    /**
     * Returns the flags.
     *  
     * @return  The flags.
     */
    public int getFlags() {
        return 0;
    }

    /**
     * Sets the format.
     *  
     * @param arg1 The format.
     */
    public void setFormat(org.gsma.rcs.core.ims.protocol.rtp.format.Format arg1) {

    }

    /**
     * Sets the flags.
     *  
     * @param arg1 The flags.
     */
    public void setFlags(int arg1) {

    }

    /**
     * Returns the sequence number.
     *  
     * @return  The sequence number.
     */
    public long getSequenceNumber() {
        return 0l;
    }

    /**
     * Sets the data.
     *  
     * @param arg1 The data.
     */
    public void setData(Object arg1) {

    }

    /**
     * Sets the sequence number.
     *  
     * @param arg1 The sequence number.
     */
    public void setSequenceNumber(long arg1) {

    }

    /**
     * Sets the time stamp.
     *  
     * @param arg1 The time stamp.
     */
    public void setTimeStamp(long arg1) {

    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isEOM() {
        return false;
    }

    /**
     * Sets the e o m.
     *  
     * @param arg1 The e o m.
     */
    public void setEOM(boolean arg1) {

    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isDiscard() {
        return false;
    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isFragmented() {
        return false;
    }

    /**
     * Returns the fragments.
     *  
     * @return  The fragments array.
     */
    public Buffer[] getFragments() {
        return (Buffer []) null;
    }

    /**
     * Returns the fragments size.
     *  
     * @return  The fragments size.
     */
    public int getFragmentsSize() {
        return 0;
    }

    /**
     * Sets the fragments.
     *  
     * @param arg1 The fragments array.
     */
    public void setFragments(Buffer[] arg1) {

    }

    /**
     * Sets the fragments size.
     *  
     * @param arg1 The fragments size.
     */
    public void setFragmentsSize(int arg1) {

    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isRTPMarkerSet() {
        return false;
    }

    /**
     * Sets the r t p marker.
     *  
     * @param arg1 The r t p marker.
     */
    public void setRTPMarker(boolean arg1) {

    }

} // end Buffer
