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

package com.orangelabs.rcs.core.ims.protocol.rtp.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Represent a RTP extension header 
 *
 * @author Deutsche Telekom
 */
public class RtpExtensionHeader implements Iterable<RtpExtensionHeader.ExtensionElement> {
    /**
     * Allowed extension header id for RCS video orientation
     */
    public static final int RTP_EXTENSION_HEADER_ID = ((0xbe << 8) | 0xde);

    /**
     * elements list
     */
    private List<RtpExtensionHeader.ExtensionElement> elements = new ArrayList<RtpExtensionHeader.ExtensionElement>(
            0);

    /**
     * Default constructor
     */
    public RtpExtensionHeader() {
    }

    /**
     * Add header element.
     *
     * @param id Element id
     * @param data Element data
     */
    public void addElement(int id, byte[] data) {
        elements.add(new ExtensionElement(id, data));
    }

    /**
     * Get ExtensionHeader element by id.
     *
     * @param id ID to search for
     * @return Element data
     */
    public ExtensionElement getElementById(int id) {
        for (ExtensionElement element : elements) {
            if (element.id == id) {
                return element;
            }
        }
        return null;
    }

    /**
     * Counts the number of elements in the header
     *
     * @return Number of elements
     */
    public int elementsCount() {
        return elements.size();
    }

    @Override
    public Iterator<ExtensionElement> iterator() {
        return this.elements.iterator();
    }

    /**
     * Extension Header Element
     */
    public static class ExtensionElement {
        public final int id;
        public final byte[] data;

        public ExtensionElement(int id, byte[] data) {
            this.id = id;
            this.data = data;
        }
    }
}
