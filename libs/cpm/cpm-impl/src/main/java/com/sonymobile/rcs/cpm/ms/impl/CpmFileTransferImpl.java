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

import com.sonymobile.rcs.cpm.ms.CpmFileTransfer;
import com.sonymobile.rcs.cpm.ms.FileItem;
import com.sonymobile.rcs.cpm.ms.Participant;
import com.sonymobile.rcs.imap.ImapMessage;
import com.sonymobile.rcs.imap.Part;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CpmFileTransferImpl extends AbstractCpmObject implements CpmFileTransfer {

    protected CpmFileTransferImpl(ImapMessage msg, MessageFolderImpl folder) {
        super(msg, folder);
    }

    @Override
    public String getFrom() {
        return getImapMessage().getFrom();
    }

    @Override
    public FileItem getFile() {
        return getFiles().iterator().next();
    }

    @Override
    public int getType() {
        String value = extractFromTag("file-transfer-type");
        if (value == null)
            return -1;
        if (value.equals("1-1")) {
            return OneToOne;
        } else if (value.equals("Ad-Hoc")) {
            return AdHoc;
        } else if (value.equals("Pre-Defined")) {
            return PreDefined;
        } else
            return -1;
    }

    private String extractFromTag(String tag) {
        String content = getImapMessage().getBody().getContent();
        return extractFromTag(content, tag);
    }

    private static String extractFromTag(String source, String tag) {
        tag = "<" + tag + ">";
        int i = source.indexOf(tag);
        if (i == -1)
            return null;
        i += tag.length();
        return source.substring(i, source.indexOf("</", i)).trim();
    }

    @Override
    public Set<Participant> getParticipants() {
        String value = extractFromTag("invited-participants");
        String[] addresses = value.split(";");
        return Participant.asSet(addresses);
    }

    @Override
    public Set<FileItem> getFiles() {
        Set<FileItem> li = new HashSet<FileItem>();

        List<Part> parts = getImapMessage().getBody().getMultiParts();

        Part p1 = parts.get(0);

        String cid = extractFromTag(p1.getContent(), "cid");

        if (cid.startsWith("cid:")) {
            cid = cid.substring(4).trim();
        }

        String sdpString = extractFromTag(p1.getContent(), "sdp");

        FileItem fi = new FileItem(cid, sdpString);

        Part p2 = parts.get(1);
        fi.setContent(p2.getContent());

        li.add(fi);
        return li;
    }

    public static String toXml(int type, List<Participant> participants, List<FileItem> files) {
        StringBuilder sb = new StringBuilder();
        sb.append("<file-transfer-type>");
        sb.append(getTypeString(type));
        sb.append("</file-transfer-type>");
        sb.append("\n");
        sb.append("<invited-participants>");
        for (int i = 0; i < participants.size(); i++) {
            if (i > 0)
                sb.append(';');
            sb.append(participants.get(i).getCommunicationAddress());
        }

        sb.append("</invited-participants>");
        sb.append("\n");

        for (FileItem f : files) {
            sb.append("<file-object>\n");
            // cid: set to the Content-ID, as defined in [RFC2392], of the corresponding stored
            // file.
            sb.append("<cid>"); //
            sb.append(f.getContentId());
            sb.append("</cid>\n");
            // SDP parameters associated with the corresponding stored file (e.g., file name).
            sb.append("<sdp>\n");
            sb.append(f.getSdpMapAsString());
            sb.append("</sdp>\n");
            sb.append("</file-object>\n");
        }

        return sb.toString();
    }

    public static String getTypeString(int code) {
        if (code == AdHoc)
            return "Ad-Hoc";
        else if (code == PreDefined)
            return "Pre-Defined";
        else if (code == OneToOne)
            return "1-1";
        else
            return "Unknown:" + code;
    }

}
