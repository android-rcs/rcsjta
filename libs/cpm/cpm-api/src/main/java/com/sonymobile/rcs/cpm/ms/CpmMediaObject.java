/*
 * Copyright (C) 2015 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.sonymobile.rcs.cpm.ms;

/**
 * The standalone Media Object is realized by the message concept of IMAP4, whereby the Media Object
 * is wrapped into a MIME formatted object in order to fit into an IMAP4 message. Other than the
 * formatting of the contents, the standalone Media Object is the same as the message object defined
 * in section 5.2.1. (CPM) RCS:
 */
public interface CpmMediaObject extends CpmMessage {

    /**
     * Returns the content type, i.e "image/png"
     * 
     * @return the mime type
     */
    // public String getContentType();

    /**
     * Returns the data
     * 
     * @return the data as bytes
     */
    public byte[] getContentAsBytes();

}
