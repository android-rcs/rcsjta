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

package com.gsma.rcs.core.ims.protocol.sdp;

import java.io.ByteArrayInputStream;
import java.util.Vector;

/**
 * SDP parser
 * 
 * @author jexa7410
 */
public class SdpParser extends Parser {
    /**
     * Session description
     */
    public SessionDescription sessionDescription = new SessionDescription();

    /**
     * Media description
     */
    public Vector<MediaDescription> mediaDescriptions = new Vector<MediaDescription>();

    /**
     * Input stream
     */
    private ByteArrayInputStream bin = null;

    /**
     * Constructor
     * 
     * @param data Data
     */
    public SdpParser(byte data[]) {
        bin = new ByteArrayInputStream(data);
        if (getToken(bin, "v=")) {
            parseSessionDescription();
            parseMediaDescriptions();
        }
    }

    /**
     * Parse session description
     */
    private void parseSessionDescription() {
        // Protocol version
        sessionDescription.version = getLine(bin);

        // Origin
        if (getToken(bin, "o=")) {
            sessionDescription.origin = getLine(bin);
        }

        // Session name
        if (getToken(bin, "s=")) {
            sessionDescription.sessionName = getLine(bin);
        }

        // Session and media Information
        if (getToken(bin, "i=")) {
            sessionDescription.sessionInfo = getLine(bin);
        }

        // URI
        if (getToken(bin, "u=")) {
            sessionDescription.uri = getLine(bin);
        }

        // E-Mail
        if (getToken(bin, "e=")) {
            sessionDescription.email = getLine(bin);
        }

        // Phone number
        if (getToken(bin, "p=")) {
            sessionDescription.phone = getLine(bin);
        }

        // Connection information
        if (getToken(bin, "c=")) {
            sessionDescription.connectionInfo = getLine(bin);
        }

        // Bandwidth information
        if (getToken(bin, "b=")) {
            sessionDescription.bandwidthInfo = getLine(bin);
        }

        // Time description
        sessionDescription.timeDescriptions = new Vector<TimeDescription>();
        while (getToken(bin, "t=")) {
            TimeDescription timeDescription = parseTimeDescription();
            this.sessionDescription.timeDescriptions.addElement(timeDescription);
        }

        // Time zone adjustments
        if (getToken(bin, "z=")) {
            sessionDescription.timezoneAdjustment = getLine(bin);
        }

        // Encryption key
        if (getToken(bin, "k=")) {
            sessionDescription.encryptionKey = getLine(bin);
        }

        // Session attributes
        sessionDescription.sessionAttributes = new Vector<MediaAttribute>();
        while (getToken(bin, "a=")) {
            String sessionAttribute = getLine(bin);
            int index = sessionAttribute.indexOf(':');
            if (index > 0) {
                String name = sessionAttribute.substring(0, index);
                String value = sessionAttribute.substring(index + 1);
                MediaAttribute attribute = new MediaAttribute(name, value);
                sessionDescription.sessionAttributes.addElement(attribute);
            }
        }
    }

    /**
     * Parse time description
     * 
     * @return Time description
     */
    private TimeDescription parseTimeDescription() {
        TimeDescription td = new TimeDescription();

        // Time the session is active
        td.timeActive = getLine(bin);

        // Repeat times
        td.repeatTimes = new Vector<String>();
        while (getToken(bin, "r=")) {
            String repeatTime = getLine(bin);
            td.repeatTimes.addElement(repeatTime);
        }

        return td;
    }

    /**
     * Parse media descriptions
     */
    private void parseMediaDescriptions() {
        while (getToken(bin, "m=")) {
            Vector<MediaDescription> descs = new Vector<MediaDescription>();

            // Media name and transport address
            String line = getLine(bin);
            int end = line.indexOf(' ');
            String name = line.substring(0, end);

            int start = end + 1;
            end = line.indexOf(' ', start);
            int port = Integer.parseInt(line.substring(start, end));

            start = end + 1;
            end = line.indexOf(' ', start);
            String protocol = line.substring(start, end);

            String payload;
            start = end + 1;
            end = line.indexOf(' ', start);
            while (end != -1) {
                payload = line.substring(start, end);
                descs.addElement(new MediaDescription(name, port, protocol, payload));
                start = end + 1;
                end = line.indexOf(' ', start);
            }
            payload = line.substring(start);
            descs.addElement(new MediaDescription(name, port, protocol, payload));

            // Session and media information
            if (getToken(bin, "i=")) {
                String mediaTitle = getLine(bin);
                for (int i = 0; i < descs.size(); i++) {
                    descs.elementAt(i).mediaTitle = mediaTitle;
                }
            }

            // Connection information
            if (getToken(bin, "c=")) {
                String connectionInfo = getLine(bin);
                for (int i = 0; i < descs.size(); i++) {
                    descs.elementAt(i).connectionInfo = connectionInfo;
                }
            }

            // Bandwidth information
            while (getToken(bin, "b=")) {
                line = getLine(bin);
                int index = line.indexOf(':');
                if (index > 0) {
                    String valueAttribute = line.substring(index + 1);
                    if (line.contains("AS")) {
                        for (int i = 0; i < descs.size(); i++) {
                            descs.elementAt(i).bandwidthInfo = valueAttribute;
                        }
                    } else if (line.contains("RS")) {
                        for (int i = 0; i < descs.size(); i++) {
                            descs.elementAt(i).senderBandwidthInfo = valueAttribute;
                        }
                    } else if (line.contains("RR")) {
                        for (int i = 0; i < descs.size(); i++) {
                            descs.elementAt(i).receiverBandwidthInfo = valueAttribute;
                        }
                    }
                }
            }

            // Encryption key
            if (getToken(bin, "k=")) {
                String encryptionKey = getLine(bin);
                for (int i = 0; i < descs.size(); i++) {
                    descs.elementAt(i).encryptionKey = encryptionKey;
                }
            }

            // Media attributes
            while (getToken(bin, "a=")) {
                line = getLine(bin);
                int index = line.indexOf(':');
                if (index > 0) {
                    String nameAttribute = line.substring(0, index);
                    String valueAttribute = line.substring(index + 1);
                    MediaAttribute attribute = new MediaAttribute(nameAttribute, valueAttribute);

                    // Dispatch for specific payload
                    if (valueAttribute.indexOf(' ') != -1) {
                        // Add the attribute only for same payload
                        boolean payloadFound = false;
                        for (int i = 0; i < descs.size(); i++) {
                            // Check if first element is a payload
                            if (valueAttribute.startsWith(descs.elementAt(i).payload)) {
                                descs.elementAt(i).mediaAttributes.addElement(attribute);
                                payloadFound = true;
                            }
                        }
                        // Add for all if first element is not a payload
                        if (!payloadFound) {
                            for (int i = 0; i < descs.size(); i++) {
                                descs.elementAt(i).mediaAttributes.addElement(attribute);
                            }
                        }
                    } else {
                        // Add for all
                        for (int i = 0; i < descs.size(); i++) {
                            descs.elementAt(i).mediaAttributes.addElement(attribute);
                        }
                    }
                }
            }

            // Copy in media descriptions
            for (int i = 0; i < descs.size(); i++) {
                mediaDescriptions.addElement((MediaDescription) descs.elementAt(i));
            }
        }
    }

    /**
     * Returns session attribute
     * 
     * @param name Attribute name
     * @return Attribute
     */
    public MediaAttribute getSessionAttribute(String name) {
        if (sessionDescription != null) {
            return sessionDescription.getSessionAttribute(name);
        } else {
            return null;
        }
    }

    /**
     * Returns a media description
     * 
     * @param name Media name
     * @return Media
     */
    public MediaDescription getMediaDescription(String name) {
        MediaDescription description = null;
        if (mediaDescriptions != null) {
            for (int i = 0; i < mediaDescriptions.size(); i++) {
                MediaDescription entry = (MediaDescription) mediaDescriptions.elementAt(i);
                if (entry.name.equals(name)) {
                    description = entry;
                    break;
                }
            }
        }
        return description;
    }

    /**
     * Returns media descriptions
     * 
     * @param name Media name
     * @return Medias
     */
    public Vector<MediaDescription> getMediaDescriptions(String name) {
        Vector<MediaDescription> result = new Vector<MediaDescription>();
        if (mediaDescriptions != null) {
            for (int i = 0; i < mediaDescriptions.size(); i++) {
                MediaDescription entry = (MediaDescription) mediaDescriptions.elementAt(i);
                if (entry.name.equals(name)) {
                    result.add(entry);
                }
            }
        }
        return result;
    }

    /**
     * Returns all media descriptions
     * 
     * @return Medias
     */
    public Vector<MediaDescription> getMediaDescriptions() {
        return mediaDescriptions;
    }
}
