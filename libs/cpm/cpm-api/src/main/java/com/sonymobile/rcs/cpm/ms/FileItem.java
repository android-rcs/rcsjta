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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This is a file representation composing the file transfer.
 * 
 * @see CpmFileTransfer
 */
public class FileItem {

    private final String mContentId;

    private final Map<Character, String[]> mSdp;

    private String mContent;

    final public static String DIRECTION_SENDONLY = "sendonly";
    final public static String DIRECTION_RECVONLY = "recvonly";
    final public static String DIRECTION_SENDRECV = "sendrecv";

    /**
     * @param contentId the unique content id
     * @param sdp the sdp map
     */
    public FileItem(String contentId, Map<Character, String[]> sdp) {
        super();
        mContentId = contentId;
        mSdp = sdp;
    }

    /**
     * @param contentId the unique content id
     * @param sdpString the SDP as string
     */
    public FileItem(String contentId, String sdpString) {
        this(contentId, getSDPMap(sdpString));
    }

    /**
     * Returns the Session Description Protocol values as a Map
     * 
     * @param string
     * @return
     */
    private static Map<Character, String[]> getSDPMap(String string) {
        // assuming format of k=XXXX
        Map<Character, String[]> map = new HashMap<Character, String[]>();
        String[] lines = string.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.length() < 3)
                continue;

            char key = line.charAt(0);
            char eq = line.charAt(1);
            String value = line.substring(2);

            if (eq == '=') {
                String[] values = map.get(key);
                if (values == null) {
                    map.put(key, new String[] {
                        value
                    });
                } else {
                    String[] values2 = Arrays.copyOf(values, values.length + 1);
                    values2[values.length] = value;
                    map.put(key, values2);
                }

            }

        }
        return map;
    }

    /**
     * Returns the usique content id
     * 
     * @return the content id
     */
    public String getContentId() {
        return mContentId;
    }

    /**
     * @return
     */
    // public Map<Character, String> getSdpMap() {
    // return sdp;
    // }

    /**
     * @return
     */
    public String getSdpMapAsString() {
        String s = "";
        Set<Character> keys = mSdp.keySet();
        for (char k : keys) {
            String[] values = mSdp.get(k);
            for (String v : values) {
                s += k + "=" + v + "\n";
            }
        }
        return s.trim();
    }

    public boolean hasContent() {
        return mContent != null || mContent.length() > 0;
    }

    public void setContent(String content) {
        this.mContent = content;
    }

    public String getContent() {
        return mContent;
    }

    public String getContentType() {
        // TODO Auto-generated method stub
        return null;
    }

    public int getSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    public String getFileName() {
        // TODO where to get the file name
        return null;
    }

    public String getSdpValue(char c) {
        String[] v = mSdp.get(c);
        if (v == null || v.length == 0) {
            return null;
        }
        return v[0];
    }

    public String[] getSdpValues(char c) {
        return mSdp.get(c);
    }

    public String getFileUri() {
        return getSdpValue('u');
    }

    public String[] getAttributes() {
        return mSdp.get('a');
    }

}
