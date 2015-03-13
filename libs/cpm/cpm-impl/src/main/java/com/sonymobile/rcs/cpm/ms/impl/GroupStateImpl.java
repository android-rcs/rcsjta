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

package com.sonymobile.rcs.cpm.ms.impl;

import com.sonymobile.rcs.cpm.ms.CpmGroupState;
import com.sonymobile.rcs.cpm.ms.CpmObjectFolder;
import com.sonymobile.rcs.cpm.ms.Participant;
import com.sonymobile.rcs.cpm.ms.SessionHistoryFolder;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Set;

public class GroupStateImpl implements CpmGroupState {

    private final long mTimestamp;

    private final String mLastFocusSessionId;

    private final String mType;

    private final Set<Participant> mParticipants;

    private static String DATEPATTERN = "yyyy-MM-dd'T'HH:mm:ssZ";

    private final SessionHistoryFolder mSessionHistoryFolder;

    private final int mStorageId;

    protected GroupStateImpl(int uid, String type, SessionHistoryFolder session, long timestamp,
            String lastFocusSessionId, Set<Participant> participants) {
        super();
        this.mType = type;
        this.mStorageId = uid;
        this.mSessionHistoryFolder = session;
        this.mTimestamp = timestamp;
        this.mLastFocusSessionId = lastFocusSessionId;
        this.mParticipants = participants;
    }

    @Override
    public CpmObjectFolder getFolder() {
        return mSessionHistoryFolder;
    }

    @Override
    public int getStorageId() {
        return mStorageId;
    }

    @Override
    public String getConversationId() {
        return mSessionHistoryFolder.getConversation().getId();
    }

    @Override
    public SessionHistoryFolder getSession() {
        return mSessionHistoryFolder;
    }

    public String getContributionId() {
        if (mSessionHistoryFolder != null)
            return mSessionHistoryFolder.getId();
        else
            return null;
    }

    public String getLastFocusSessionId() {
        return mLastFocusSessionId;
    }

    public long getTimeStamp() {
        return mTimestamp;
    }

    @Override
    public String getType() {
        return mType;
    }

    public String toXml() {
        SimpleDateFormat sdt = new SimpleDateFormat(DATEPATTERN);

        StringBuilder sb = new StringBuilder();
        sb.append("<groupstate timestamp=\"");
        sb.append(sdt.format(mTimestamp));
        sb.append("\" lastfocussessionid=\"");
        sb.append(mLastFocusSessionId);
        sb.append("\" group-type=\"");
        sb.append(getType());
        sb.append("\" contributionid=\"");
        sb.append(getContributionId());
        sb.append("\">\n");

        if (mParticipants != null) {
            for (Participant p : mParticipants) {
                sb.append("\t<participant name=\"");
                sb.append(p.getName());
                sb.append("\" comm-addr=\"");
                sb.append(p.getCommunicationAddress());
                sb.append("\"/>\n");
            }
        }
        sb.append("</groupstate>");
        return sb.toString();
    }

    public synchronized static GroupStateImpl fromXml(int uid, SessionHistoryFolder sess, String xml)
            throws IOException, XmlPullParserException, ParseException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(new StringReader(xml));
        return read(uid, sess, parser);
    }

    public static GroupStateImpl read(int uid, SessionHistoryFolder sess, XmlPullParser parser)
            throws IOException, XmlPullParserException, ParseException {
        parser.nextTag();

        String type = null;

        String tt = parser.getAttributeValue(null, "timestamp");
        // TODO CONVERT ISO8601 TO GENERAL TIME ZONE XML DATETIME
        tt = tt.substring(0, 19) + "GMT" + tt.substring(19);
        String lastFocusSessionId = parser.getAttributeValue(null, "lastfocussessionid");

        type = parser.getAttributeValue(null, "group-type");

        SimpleDateFormat sdt = new SimpleDateFormat(DATEPATTERN);
        long timestamp = sdt.parse(tt).getTime();

        Set<Participant> participants = null;
        while (parser.nextTag() == XmlPullParser.START_TAG) {
            // handle element content
            String name = parser.getAttributeValue(null, "name");
            String communicationAddress = parser.getAttributeValue(null, "comm-addr");
            if (participants == null) {
                participants = new HashSet<Participant>();
            }
            participants.add(new Participant(name, communicationAddress));
            parser.nextTag();
        }

        return new GroupStateImpl(uid, type, sess, timestamp, lastFocusSessionId, participants);
    }

    public Set<Participant> getParticipants() {
        return mParticipants;
    }

    @Override
    public Set<String> getParticipantAddresses() {
        Set<String> addr = new HashSet<String>();

        for (Participant p : mParticipants) {
            addr.add(p.getCommunicationAddress());
        }

        return addr;
    }

    @Override
    public String toString() {
        return "GROUPSTATE[" + toXml() + "]";
    }

}
