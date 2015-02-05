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

package com.orangelabs.rcs.core.ims.protocol.sdp;

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
    public String name;

    @Override
    public String toString() {
        return "MediaDescription [name=" + name + ", port=" + port + ", protocol=" + protocol
                + ", payloadType=" + payloadType + ", payload=" + payload + ", mediaTitle="
                + mediaTitle + ", connectionInfo=" + connectionInfo + ", bandwidthInfo="
                + bandwidthInfo + ", senderBandwidthInfo=" + senderBandwidthInfo
                + ", receiverBandwidthInfo=" + receiverBandwidthInfo + ", encryptionKey="
                + encryptionKey + ", mediaAttributes=" + mediaAttributes + "]";
    }

    /**
     * Media port
     */
    public int port;

    /**
     * Media protocol
     */
    public String protocol;

    /**
     * Payload type
     */
    public int payloadType;

    /**
     * Payload
     */
    public String payload;

    /**
     * Media title
     */
    public String mediaTitle;

    /**
     * Connection info
     */
    public String connectionInfo;

    /**
     * Bandwidth info
     */
    public String bandwidthInfo;

    /**
     * Sender bandwidth info (RFC 3556)
     */
    public String senderBandwidthInfo;

    /**
     * Receiver bandwidth info (RFC 3556)
     */
    public String receiverBandwidthInfo;

    /**
     * Encryption key
     */
    public String encryptionKey;

    /**
     * Media attributes
     */
    public Vector<MediaAttribute> mediaAttributes = new Vector<MediaAttribute>();

    /**
     * Constructor
     * 
     * @param name Media name
     * @param port Media port
     * @param protocol Media protocol
     * @param payload Media payload
     */
    public MediaDescription(String name, int port, String protocol, String payload) {
        this.name = name;
        this.port = port;
        this.protocol = protocol;
        this.payload = payload;
        try {
            this.payloadType = Integer.parseInt(payload);
        } catch (Exception e) {
            this.payloadType = -1;
        }
    }

    public MediaAttribute getMediaAttribute(String name) {
        MediaAttribute attribute = null;
        if (mediaAttributes != null) {
            for (int i = 0; i < mediaAttributes.size(); i++) {
                MediaAttribute entry = (MediaAttribute) mediaAttributes.elementAt(i);
                if (entry.getName().equals(name)) {
                    attribute = entry;
                    break;
                }
            }
        }

        return attribute;
    }
}
