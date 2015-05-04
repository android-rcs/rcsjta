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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ImapFolderStatus {

    private String mFolderName;

    private Set<String> mFlags = null;
    private Set<String> mPermanentFlags = null;

    private int mExists;
    private int mRecent;

    private int mUidValidity;
    private int mUnseen;

    private int mNextUid;

    public ImapFolderStatus(String name) {
        mFolderName = name;
    }

    public String getFolderName() {
        return mFolderName;
    }

    public ImapFolderStatus(String name, List<String> imapInfos) {
        mFolderName = name;
        /*
         * FLAGS (\Answered \Deleted \Draft \Flagged \Seen) 2 EXISTS 1 RECENT OK [UIDVALIDITY
         * 229669678] UIDs valid OK [UNSEEN 2] Message 2 is first unseen OK [PERMANENTFLAGS
         * (\Answered \Deleted \Draft \Flagged \Seen \*)] Limited OK [HIGHESTMODSEQ 2] Highest OK
         * [UIDNEXT 3] Predicted next UID
         */
        for (String spec : imapInfos) {
            spec = spec.substring(2).trim(); // remove tag
            if (spec.startsWith("FLAGS")) {
                int i = spec.indexOf('(');
                int j = spec.indexOf(')');
                if (i < j) {
                    String[] fls = spec.substring(i + 1, j).split(" ");
                    mFlags = new HashSet<String>();
                    for (String f : fls) {
                        mFlags.add(f.substring(1));
                    }
                }
            } else if (spec.endsWith("EXISTS")) {
                int i = spec.indexOf(' ');
                mExists = Integer.parseInt(spec.substring(0, i));
            } else if (spec.endsWith("RECENT")) {
                int i = spec.indexOf(' ');
                mRecent = Integer.parseInt(spec.substring(0, i));
            } else if (spec.startsWith("OK [UIDVALIDITY")) {
                mUidValidity = Integer.parseInt(spec.substring(16, spec.indexOf(']')));
            } else if (spec.startsWith("OK [UNSEEN")) {
                mUnseen = Integer.parseInt(spec.substring(11, spec.indexOf(']')));
            } else if (spec.startsWith("OK [PERMANENTFLAGS")) {
                spec = spec.substring(19, spec.indexOf(']'));
                int i = spec.indexOf('(');
                int j = spec.indexOf(')');

                if (0 <= i && i < j) {
                    String[] fls = spec.substring(i + 1, j).split(" ");
                    mPermanentFlags = new HashSet<String>();
                    for (String f : fls) {
                        mPermanentFlags.add(f.substring(1));
                    }
                }

            } else if (spec.startsWith("OK [UIDNEXT")) {
                mNextUid = Integer.parseInt(spec.substring(12, spec.indexOf(']')));
            }
        }
    }

    public int getExists() {
        return mExists;
    }

    public Set<String> getFlags() {
        return mFlags;
    }

    public int getNextUid() {
        return mNextUid;
    }

    public Set<String> getPermanentFlags() {
        return mPermanentFlags;
    }

    public int getRecent() {
        return mRecent;
    }

    public int getUidValidity() {
        return mUidValidity;
    }

    public int getUnseen() {
        return mUnseen;
    }

    public void setExists(int exists) {
        this.mExists = exists;
    }

    public void setNextUid(int nextUid) {
        this.mNextUid = nextUid;
    }

    public void setRecent(int recent) {
        this.mRecent = recent;
    }

    public void setUnseen(int unseen) {
        this.mUnseen = unseen;
    }

    public void setUidValidity(int uidValidity) {
        this.mUidValidity = uidValidity;
    }

    @Override
    public String toString() {
        String s = "FOLDERSTATUS[";
        s += "exists=" + mExists;
        s += ", recent=" + mRecent;
        s += ", unseen=" + mUnseen;
        s += ", UIDvalidity=" + mUidValidity;
        s += ", nextUID=" + mNextUid;
        s += ", flags=" + mFlags;
        s += ", permanentFlags=" + mPermanentFlags;
        s += "]";
        return s;
    }

    /*
     * public static void main(String[] args) { String [] arr = new String [] {
     * "* FLAGS (\\Answered \\Deleted \\Draft \\Flagged \\Seen)", "* 2 EXISTS", "* 0 RECENT",
     * "* OK [UIDVALIDITY 229669678] UIDs valid", "* OK [UNSEEN 2] Message 2 is first unseen",
     * "* OK [PERMANENTFLAGS (\\Answered \\Deleted \\Draft \\Flagged \\Seen \\*)] Limited",
     * "* OK [HIGHESTMODSEQ 2] Highest", "* OK [UIDNEXT 3] Predicted next UID"}; IMAPFolderStatus st
     * = new IMAPFolderStatus(Arrays.asList(arr)); System.out.println(st); }
     */

    public void setFieldValue(StatusField sf, int v) {
        switch (sf) {
            case UIDNEXT:
                mNextUid = v;
                break;
            case UIDVALIDITY:
                mUidValidity = v;
                break;
            case MESSAGES:
                mExists = v;
                break;
            case RECENT:
                mRecent = v;
                break;
            case UNSEEN:
                mUnseen = v;
                break;
            default:
                break;
        }
    }

    public void merge(ImapFolderStatus status2) {
        mExists += status2.mExists;
        mUnseen += status2.mUnseen;
        mRecent += status2.mRecent;
        mUidValidity = Math.max(mUidValidity, status2.mUidValidity);
        mNextUid = Math.max(mNextUid, status2.mNextUid);
    }

    public static enum StatusField {

        MESSAGES,
        // The number of messages in the mailbox.
        RECENT,
        // The number of messages with the \Recent flag set.
        UIDNEXT,
        // The next unique identifier value of the mailbox. Refer to
        // section 2.3.1.1 for more information.
        UIDVALIDITY,
        // The unique identifier validity value of the mailbox. Refer to
        // section 2.3.1.1 for more information.
        UNSEEN,
        // The number of messages which do not have the \Seen flag set.

    }

    /*
     * FLAGS Defined flags in the mailbox. See the description of the FLAGS response for more
     * detail. <n> EXISTS The number of messages in the mailbox. See the description of the EXISTS
     * response for more detail. <n> RECENT The number of messages with the \Recent flag set. See
     * the description of the RECENT response for more detail. OK [UNSEEN <n>] The message sequence
     * number of the first unseen message in the mailbox. If this is missing, the client can not
     * make any assumptions about the first unseen message in the mailbox, and needs to issue a
     * SEARCH command if it wants to find it. OK [PERMANENTFLAGS (<list of flags>)] A list of
     * message flags that the client can change permanently. If this is missing, the client should
     * assume that all flags can be changed permanently. OK [UIDNEXT <n>] The next unique identifier
     * value. Refer to section 2.3.1.1 for more information. If this is missing, the client can not
     * make any assumptions about the next unique identifier value. OK [UIDVALIDITY <n>] The unique
     * identifier validity value. Refer to section 2.3.1.1 for more information. If this is missing,
     * the server does not support unique identifiers.
     */
}
