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

package com.sonymobile.rcs.imap;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ImapMessageMetadata {

    private final int mUid;

    private final Set<Flag> mFlags = new HashSet<Flag>();

    private final String mEnvelope;

    private final String mBodyInfo;

    private final long mInternalDate;

    private final int mSize;

    public ImapMessageMetadata(int id) {
        mUid = id;
        mEnvelope = null;
        mBodyInfo = null;
        mInternalDate = 0;
        mSize = 0;
    }

    protected ImapMessageMetadata(int uid, String envelope, String bodyInfo, long internalDate,
            int size) {
        super();
        mUid = uid;
        mEnvelope = envelope;
        mBodyInfo = bodyInfo;
        mInternalDate = internalDate;
        mSize = size;
    }

    public static ImapMessageMetadata parseMetadata(int id, String imapResponse)
            throws ParseException {
        Map<String, String> parsed = parseMetadata(imapResponse);
        // 09-Jul-2014 14:18:11 +0000
        //
        String sdate = parsed.get("INTERNALDATE");
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss Z");
        long _internalDate = sdf.parse(sdate).getTime();
        String _envelope = parsed.get("ENVELOPE");
        int _size = Integer.parseInt(parsed.get("RFC822.SIZE"));
        String _bodyInfo = parsed.get("BODY");
        ImapMessageMetadata md = new ImapMessageMetadata(id, _envelope, _bodyInfo, _internalDate,
                _size);
        String[] sflags = parsed.get("FLAGS").split(" ");
        for (String f : sflags) {
            if (f.startsWith("\\")) {
                f = f.substring(1);
                md.mFlags.add(Flag.valueOf(f));
            }
        }
        return md;
    }

    public String getBodyInfo() {
        return mBodyInfo;
    }

    public String getEnvelope() {
        return mEnvelope;
    }

    public Set<Flag> getFlags() {
        return mFlags;
    }

    public long getInternalDate() {
        return mInternalDate;
    }

    public int getSize() {
        return mSize;
    }

    public int getUid() {
        return mUid;
    }

    // Example:
    // * 1 FETCH (FLAGS (\Seen) INTERNALDATE "21-Jul-2014 13:20:08 +0000" RFC822.SIZE 66 ENVELOPE
    // (NIL NIL (("Joe" NIL "joe" "sony.com")) (("Joe" NIL "joe" "sony.com")) (("Joe" NIL "joe"
    // "sony.com")) NIL NIL NIL NIL NIL) BODY ("TEXT" "PLAIN" ("charset" "us-ascii") NIL NIL "7BIT"
    // 39 1))
    private static Map<String, String> parseMetadata(String l) {
        Map<String, String> map = new HashMap<String, String>();

        l = l.substring(l.indexOf('(') + 1, l.lastIndexOf(')')) + " ";

        char[] arr = l.toCharArray();

        String k = "";
        String v = "";

        int mode = 0; // 0 reading key, 1 reading value, 2 reading plain value, 3 reading quoted
                      // value, 4 reading parenthesis value
        int par = 0;
        for (char c : arr) {
            switch (mode) {
                case 0:
                    if (c == ' ' && k.length() > 0) {
                        // done reading key
                        mode = 1;
                    } else if (c != ' ') {
                        k += c;
                    }
                    break;
                case 1:
                    if (c == '"') {
                        mode = 3;
                    } else if (c == '(') {
                        mode = 4;
                        par = -1;
                    } else {
                        v += c;
                        mode = 2;
                    }
                    break;
                case 2:
                    if (c == ' ') {
                        mode = 0;
                        map.put(k, v);
                        k = "";
                        v = "";
                    } else {
                        v += c;
                    }
                    break;
                case 3:
                    if (c == '"') {
                        map.put(k, v);
                        k = "";
                        v = "";
                        mode = 0;
                    } else {
                        v += c;
                    }
                    break;
                case 4:
                    if (c == '(') {
                        v += c;
                        par--;
                    } else if (c == ')') {
                        par++;
                        if (par == 0) {
                            map.put(k, v);
                            k = "";
                            v = "";
                            mode = 0;
                        } else {
                            v += c;
                        }
                    } else {
                        v += c;
                    }
                    break;
            }
        }
        return map;
    }

    public ImapMessageStatus asStatus() {
        return new ImapMessageStatus(mFlags.contains(Flag.Seen), mFlags.contains(Flag.Draft),
                mFlags.contains(Flag.Recent), mFlags.contains(Flag.Deleted),
                mFlags.contains(Flag.Answered), mFlags.contains(Flag.Flagged), false);
    }

}
