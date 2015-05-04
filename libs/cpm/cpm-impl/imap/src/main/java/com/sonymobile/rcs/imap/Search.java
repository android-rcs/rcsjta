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

import java.util.Calendar;

public class Search {

    private static String[] IMAPMONTHS = {
            "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    private StringBuffer mBuffer = new StringBuffer();

    private String mImmutable = null;

    public static final Search ALL = new Search(" ALL");

    private Search(String criteria) {
        mImmutable = criteria;
    }

    public Search() {
    }

    public static final String getImapDate(Calendar date) {
        return date.get(Calendar.DAY_OF_MONTH) + "-" + IMAPMONTHS[date.get(Calendar.MONTH)] + "-"
                + date.get(Calendar.YEAR);
    }

    private void append(String a) {
        if (mImmutable == null)
            mBuffer.append(a);
    }

    @Override
    public String toString() {
        if (mImmutable != null) {
            return mImmutable;
        } else {
            return mBuffer.toString();
        }
    }

    // ALL
    // All messages in the mailbox; the default initial key for
    // ANDing.
    public Search all() {
        append(" ALL");
        return this;
    }

    // ANSWERED
    // Messages with the \Answered flag set.
    public Search answered() {
        append(" ANSWERED");
        return this;
    }

    // BCC <string>
    // Messages that contain the specified string in the envelope
    // structure's BCC field.
    public Search bcc(String string) {
        append(" BCC");
        append(" ");
        append(string);
        return this;
    }

    // BEFORE <date>
    // Messages whose internal date (disregarding time and timezone)
    // is earlier than the specified date.
    public Search before(Calendar date) {
        append(" BEFORE");
        append(" ");
        append(getImapDate(date));
        return this;
    }

    // BODY <string>
    // Messages that contain the specified string in the body of the
    // message.
    public Search body(String string) {
        append(" BODY");
        append(" ");
        append(string);
        return this;
    }

    // CC <string>
    // Messages that contain the specified string in the envelope
    // structure's CC field.
    public Search cc(String string) {
        append(" CC");
        append(" ");
        append(string);
        return this;
    }

    // DELETED
    // Messages with the \Deleted flag set.
    public Search deleted() {
        append(" DELETED");
        return this;
    }

    // DRAFT
    // Messages with the \Draft flag set.
    public Search draft() {
        append(" DRAFT");
        return this;
    }

    // FLAGGED
    // Messages with the \Flagged flag set.
    public Search flagged() {
        append(" FLAGGED");
        return this;
    }

    // FROM <string>
    // Messages that contain the specified string in the envelope
    // structure's FROM field.
    public Search from(String string) {
        append(" FROM");
        append(" ");
        append(string);
        return this;
    }

    // HEADER <field-name> <string>
    // Messages that have a header with the specified field-name (as
    // defined in [RFC-2822]) and that contains the specified string
    // in the text of the header (what comes after the colon). If the
    // string to search is zero-length, this matches all messages that
    // have a header line with the specified field-name regardless of
    // the contents.
    public Search header(String fieldName, String string) {
        append(" HEADER");
        append(" ");
        append(fieldName);
        append(" ");
        append(string);
        return this;
    }

    // KEYWORD <flag>
    // Messages with the specified keyword flag set.
    public Search keyword(String string) {
        append(" KEYWORD");
        append(" ");
        append(string);
        return this;
    }

    // LARGER <n>
    // Messages with an [RFC-2822] size larger than the specified
    // number of octets.
    public Search larger(int size) {
        append(" LARGER");
        append(" ");
        append("" + size);
        return this;
    }

    //
    // NEW
    // Messages that have the \Recent flag set but not the \Seen flag.
    // This is functionally equivalent to "(RECENT UNSEEN)".
    public Search isnew() {
        append(" NEW");
        return this;
    }

    // NOT <search-key>
    // Messages that do not match the specified search key.
    public Search not(Search s) {
        append(" NOT");
        append(s.toString());
        return this;
    }

    // OLD
    // Messages that do not have the \Recent flag set. This is
    // functionally equivalent to "NOT RECENT" (as opposed to "NOT
    // NEW").
    public Search old() {
        append(" OLD");
        return this;
    }

    // ON <date>
    // Messages whose internal date (disregarding time and timezone)
    // is within the specified date.
    public Search on(Calendar date) {
        append(" ON");
        append(" ");
        append(getImapDate(date));
        return this;
    }

    // OR <search-key1> <search-key2>
    // Messages that match either search key.
    public Search or(Search s1, Search s2) {
        append(" OR");
        append(s1.toString());
        append(s2.toString());
        return this;
    }

    // RECENT
    // Messages that have the \Recent flag set.
    public Search recent() {
        append(" RECENT");
        return this;
    }

    // SEEN
    // Messages that have the \Seen flag set.
    public Search seen() {
        append(" SEEN");
        return this;
    }

    // SENTBEFORE <date>
    // Messages whose [RFC-2822] Date: header (disregarding time and
    // timezone) is earlier than the specified date.
    public Search sentBefore(Calendar date) {
        append(" SENTBEFORE");
        append(" ");
        append(getImapDate(date));
        return this;
    }

    // SENTON <date>
    // Messages whose [RFC-2822] Date: header (disregarding time and
    // timezone) is within the specified date.
    public Search sentOn(Calendar date) {
        append(" SENTON");
        append(" ");
        append(getImapDate(date));
        return this;
    }

    // SENTSINCE <date>
    // Messages whose [RFC-2822] Date: header (disregarding time and
    // timezone) is within or later than the specified date.
    public Search sentSince(Calendar date) {
        append(" SENTSINCE");
        append(" ");
        append(getImapDate(date));
        return this;
    }

    // SINCE <date>
    // Messages whose internal date (disregarding time and timezone)
    // is within or later than the specified date.
    public Search since(Calendar date) {
        append(" SINCE");
        append(" ");
        append(getImapDate(date));
        return this;
    }

    // SMALLER <n>
    // Messages with an [RFC-2822] size smaller than the specified
    // number of octets.
    public Search smaller(int size) {
        append(" SMALLER");
        append(" ");
        append("" + size);
        return this;
    }

    // SUBJECT <string>
    // Messages that contain the specified string in the envelope
    // structure's SUBJECT field.
    public Search subject(String string) {
        append(" SUBJECT");
        append(" ");
        append(string);
        return this;
    }

    // TEXT <string>
    // Messages that contain the specified string in the header or
    // body of the message.
    public Search text(String string) {
        append(" TEXT");
        append(" ");
        append(string);
        return this;
    }

    // TO <string>
    // Messages that contain the specified string in the envelope
    // structure's TO field.
    public Search to(String string) {
        append(" TO");
        append(" ");
        append(string);
        return this;
    }

    // UID <sequence set>
    // Messages with unique identifiers corresponding to the specified
    // unique identifier set. Sequence set ranges are permitted.
    /*
     * sequence-set = (seq-number / seq-range) *("," sequence-set) ; set of seq-number values,
     * regardless of order. ; Servers MAY coalesce overlaps and/or execute the ; sequence in any
     * order. ; Example: a message sequence number set of ; 2,4:7,9,12:* for a mailbox with 15
     * messages is ; equivalent to 2,4,5,6,7,9,12,13,14,15 ; Example: a message sequence number set
     * of *:4,5:7 ; for a mailbox with 10 messages is equivalent to ; 10,9,8,7,6,5,4,5,6,7 and MAY
     * be reordered and ; overlap coalesced to be 4,5,6,7,8,9,10.
     */
    public Search uid(String sequence) {
        append(" UID");
        append(" ");
        append(sequence);
        return this;
    }

    public Search fromUid(int uid) {
        append(" UID");
        append(" ");
        append(uid + ":*");
        return this;
    }

    // UNANSWERED
    // Messages that do not have the \Answered flag set.
    public Search unanswered() {
        append(" UNANSWERED");
        return this;
    }

    // UNDELETED
    // Messages that do not have the \Deleted flag set.
    public Search undeleted() {
        append(" UNDELETED");
        return this;
    }

    // UNDRAFT
    // Messages that do not have the \Draft flag set.
    public Search undraft() {
        append(" UNDRAFT");
        return this;
    }

    // UNFLAGGED
    // Messages that do not have the \Flagged flag set.
    public Search unflagged() {
        append(" UNFLAGGED");
        return this;
    }

    // UNKEYWORD <flag>
    // Messages that do not have the specified keyword flag set.
    public Search unkeyword(String string) {
        append(" UNKEYWORD");
        append(" ");
        append(string);
        return this;
    }

    // UNSEEN
    // Messages that do not have the \Seen flag set.
    public Search unseen() {
        append(" UNSEEN");
        return this;
    }
}
