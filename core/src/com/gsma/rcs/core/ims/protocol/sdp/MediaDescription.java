/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.core.ims.protocol.sdp;

import java.util.Vector;

/**
 * Media description
 * 
 * @author jexa7410
 */
public class MediaDescription {

    /**
     * Media name
     */
    public String mName;

    @Override
    public String toString() {
        return "MediaDescription [name=" + mName + ", port=" + mPort + ", protocol=" + mProtocol
                + ", payload=" + mPayload + ", mediaTitle=" + mMediaTitle + ", connectionInfo="
                + mConnectionInfo + ", bandwidthInfo=" + mBandwidthInfo + ", senderBandwidthInfo="
                + mSenderBandwidthInfo + ", receiverBandwidthInfo=" + mReceiverBandwidthInfo
                + ", encryptionKey=" + mEncryptionKey + ", mediaAttributes=" + mMediaAttributes
                + "]";
    }

    /**
     * Media port
     */
    public int mPort;

    /**
     * Media protocol
     */
    public String mProtocol;

    /**
     * Payload
     */
    public String mPayload;

    /**
     * Media title
     */
    public String mMediaTitle;

    /**
     * Connection info
     */
    public String mConnectionInfo;

    /**
     * Bandwidth info
     */
    public String mBandwidthInfo;

    /**
     * Sender bandwidth info (RFC 3556)
     */
    public String mSenderBandwidthInfo;

    /**
     * Receiver bandwidth info (RFC 3556)
     */
    public String mReceiverBandwidthInfo;

    /**
     * Encryption key
     */
    public String mEncryptionKey;

    /**
     * Media attributes
     */
    public Vector<MediaAttribute> mMediaAttributes = new Vector<>();

    /**
     * Constructor
     * 
     * @param name Media name
     * @param port Media port
     * @param protocol Media protocol
     * @param payload Media payload
     */
    public MediaDescription(String name, int port, String protocol, String payload) {
        mName = name;
        mPort = port;
        mProtocol = protocol;
        mPayload = payload;
    }

    public MediaAttribute getMediaAttribute(String name) {
        MediaAttribute attribute = null;
        if (mMediaAttributes != null) {
            for (int i = 0; i < mMediaAttributes.size(); i++) {
                MediaAttribute entry = mMediaAttributes.elementAt(i);
                if (entry.getName().equals(name)) {
                    attribute = entry;
                    break;
                }
            }
        }
        return attribute;
    }
}
