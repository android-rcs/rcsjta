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

package com.gsma.rcs.provider.history;

import com.gsma.rcs.provider.eab.RichAddressBookProvider;
import com.gsma.rcs.provider.ipcall.IPCallProvider;
import com.gsma.rcs.provider.messaging.ChatProvider;
import com.gsma.rcs.provider.messaging.FileTransferData;
import com.gsma.rcs.provider.messaging.FileTransferProvider;
import com.gsma.rcs.provider.messaging.GroupDeliveryInfoProvider;
import com.gsma.rcs.provider.settings.RcsSettingsProvider;
import com.gsma.rcs.provider.sharing.GeolocSharingData;
import com.gsma.rcs.provider.sharing.GeolocSharingProvider;
import com.gsma.rcs.provider.sharing.ImageSharingData;
import com.gsma.rcs.provider.sharing.ImageSharingProvider;
import com.gsma.rcs.provider.sharing.VideoSharingData;
import com.gsma.rcs.provider.sharing.VideoSharingProvider;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.filetransfer.FileTransferLog;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharingLog;
import com.gsma.services.rcs.sharing.image.ImageSharingLog;
import com.gsma.services.rcs.sharing.video.VideoSharingLog;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/* package private */final class HistoryConstants {

    /* package private */static final String[] FULL_PROJECTION = {
            HistoryLogData.KEY_BASECOLUMN_ID, HistoryLogData.KEY_PROVIDER_ID,
            HistoryLogData.KEY_ID, HistoryLogData.KEY_MIME_TYPE, HistoryLogData.KEY_DIRECTION,
            HistoryLogData.KEY_CONTACT, HistoryLogData.KEY_TIMESTAMP,
            HistoryLogData.KEY_TIMESTAMP_SENT, HistoryLogData.KEY_TIMESTAMP_DELIVERED,
            HistoryLogData.KEY_TIMESTAMP_DISPLAYED, HistoryLogData.KEY_STATUS,
            HistoryLogData.KEY_REASON_CODE, HistoryLogData.KEY_READ_STATUS,
            HistoryLogData.KEY_CHAT_ID, HistoryLogData.KEY_CONTENT, HistoryLogData.KEY_FILEICON,
            HistoryLogData.KEY_FILEICON_MIME_TYPE, HistoryLogData.KEY_FILENAME,
            HistoryLogData.KEY_FILESIZE, HistoryLogData.KEY_TRANSFERRED,
            HistoryLogData.KEY_DURATION
    };

    /* package private */static final Set<String> PROTECTED_INTERNAL_DATABASES = getProtectedInternalDatabases();

    /* package private */static final Set<Integer> INTERNAL_MEMBER_IDS = getInternalMemberIds();

    /* package private */static final Set<HistoryMemberDatabase> INTERNAL_MEMBERS = getInternalMembers();

    private static final Set<String> getProtectedInternalDatabases() {
        Set<String> protectedInternalDatabases = new HashSet<String>();
        protectedInternalDatabases.add(RcsSettingsProvider.DATABASE_NAME);
        protectedInternalDatabases.add(RichAddressBookProvider.DATABASE_NAME);
        protectedInternalDatabases.add(GroupDeliveryInfoProvider.DATABASE_NAME);
        protectedInternalDatabases.add(IPCallProvider.DATABASE_NAME);
        return protectedInternalDatabases;
    }

    private static final Set<Integer> getInternalMemberIds() {
        Set<Integer> internalMemberIds = new HashSet<Integer>();
        internalMemberIds.add(ChatLog.Message.HISTORYLOG_MEMBER_ID);
        internalMemberIds.add(FileTransferData.HISTORYLOG_MEMBER_ID);
        internalMemberIds.add(ImageSharingData.HISTORYLOG_MEMBER_ID);
        internalMemberIds.add(VideoSharingData.HISTORYLOG_MEMBER_ID);
        internalMemberIds.add(GeolocSharingData.HISTORYLOG_MEMBER_ID);
        return internalMemberIds;
    }

    private static final Set<HistoryMemberDatabase> getInternalMembers() {
        Set<HistoryMemberDatabase> internalMembers = new HashSet<HistoryMemberDatabase>();
        internalMembers.add(new HistoryMemberDatabase(ChatLog.Message.HISTORYLOG_MEMBER_ID,
                ChatLog.Message.CONTENT_URI, ChatProvider.DATABASE_NAME, null,
                ChatProvider.TABLE_MESSAGE, getChatMessageProviderColumnMapping()));
        internalMembers.add(new HistoryMemberDatabase(FileTransferData.HISTORYLOG_MEMBER_ID,
                FileTransferLog.CONTENT_URI, FileTransferProvider.DATABASE_NAME, null,
                FileTransferProvider.TABLE, getFileTransferProviderColumnMapping()));
        internalMembers.add(new HistoryMemberDatabase(ImageSharingData.HISTORYLOG_MEMBER_ID,
                ImageSharingLog.CONTENT_URI, ImageSharingProvider.DATABASE_NAME, null,
                ImageSharingProvider.TABLE, getImageSharingProviderColumnMapping()));
        internalMembers.add(new HistoryMemberDatabase(VideoSharingData.HISTORYLOG_MEMBER_ID,
                VideoSharingLog.CONTENT_URI, VideoSharingProvider.DATABASE_NAME, null,
                VideoSharingProvider.TABLE, getVideoSharingProviderColumnMapping()));
        internalMembers.add(new HistoryMemberDatabase(GeolocSharingData.HISTORYLOG_MEMBER_ID,
                GeolocSharingLog.CONTENT_URI, GeolocSharingProvider.DATABASE_NAME, null,
                GeolocSharingProvider.TABLE, getGeolocSharingProviderColumnMapping()));
        return internalMembers;
    }

    private static final Map<String, String> getChatMessageProviderColumnMapping() {
        Map<String, String> columnMapping = new HashMap<String, String>();
        columnMapping.put(HistoryLogData.KEY_PROVIDER_ID,
                String.valueOf(ChatLog.Message.HISTORYLOG_MEMBER_ID));
        columnMapping.put(HistoryLogData.KEY_BASECOLUMN_ID, ChatLog.Message.BASECOLUMN_ID);
        columnMapping.put(HistoryLogData.KEY_ID, ChatLog.Message.MESSAGE_ID);
        columnMapping.put(HistoryLogData.KEY_MIME_TYPE, ChatLog.Message.MIME_TYPE);
        columnMapping.put(HistoryLogData.KEY_DIRECTION, ChatLog.Message.DIRECTION);
        columnMapping.put(HistoryLogData.KEY_CONTACT, ChatLog.Message.CONTACT);
        columnMapping.put(HistoryLogData.KEY_TIMESTAMP, ChatLog.Message.TIMESTAMP);
        columnMapping.put(HistoryLogData.KEY_TIMESTAMP_SENT, ChatLog.Message.TIMESTAMP_SENT);
        columnMapping.put(HistoryLogData.KEY_TIMESTAMP_DELIVERED,
                ChatLog.Message.TIMESTAMP_DELIVERED);
        columnMapping.put(HistoryLogData.KEY_TIMESTAMP_DISPLAYED,
                ChatLog.Message.TIMESTAMP_DISPLAYED);
        columnMapping.put(HistoryLogData.KEY_STATUS, ChatLog.Message.STATUS);
        columnMapping.put(HistoryLogData.KEY_REASON_CODE, ChatLog.Message.REASON_CODE);
        columnMapping.put(HistoryLogData.KEY_READ_STATUS, ChatLog.Message.READ_STATUS);
        columnMapping.put(HistoryLogData.KEY_CHAT_ID, ChatLog.Message.CHAT_ID);
        columnMapping.put(HistoryLogData.KEY_CONTENT, ChatLog.Message.CONTENT);
        return columnMapping;
    }

    private static final Map<String, String> getFileTransferProviderColumnMapping() {
        Map<String, String> columnMapping = new HashMap<String, String>();
        columnMapping.put(HistoryLogData.KEY_PROVIDER_ID,
                String.valueOf(FileTransferData.HISTORYLOG_MEMBER_ID));
        columnMapping.put(HistoryLogData.KEY_BASECOLUMN_ID, FileTransferLog.BASECOLUMN_ID);
        columnMapping.put(HistoryLogData.KEY_ID, FileTransferLog.FT_ID);
        columnMapping.put(HistoryLogData.KEY_MIME_TYPE, FileTransferLog.MIME_TYPE);
        columnMapping.put(HistoryLogData.KEY_DIRECTION, FileTransferLog.DIRECTION);
        columnMapping.put(HistoryLogData.KEY_CONTACT, FileTransferLog.CONTACT);
        columnMapping.put(HistoryLogData.KEY_TIMESTAMP, FileTransferLog.TIMESTAMP);
        columnMapping.put(HistoryLogData.KEY_TIMESTAMP_SENT, FileTransferLog.TIMESTAMP_SENT);
        columnMapping.put(HistoryLogData.KEY_TIMESTAMP_DELIVERED,
                FileTransferLog.TIMESTAMP_DELIVERED);
        columnMapping.put(HistoryLogData.KEY_TIMESTAMP_DISPLAYED,
                FileTransferLog.TIMESTAMP_DISPLAYED);
        columnMapping.put(HistoryLogData.KEY_STATUS, FileTransferLog.STATE);
        columnMapping.put(HistoryLogData.KEY_REASON_CODE, FileTransferLog.REASON_CODE);
        columnMapping.put(HistoryLogData.KEY_READ_STATUS, FileTransferLog.READ_STATUS);
        columnMapping.put(HistoryLogData.KEY_CHAT_ID, FileTransferLog.CHAT_ID);
        columnMapping.put(HistoryLogData.KEY_CONTENT, FileTransferLog.FILE);
        columnMapping.put(HistoryLogData.KEY_FILEICON, FileTransferLog.FILEICON);
        columnMapping.put(HistoryLogData.KEY_FILENAME, FileTransferLog.FILENAME);
        columnMapping.put(HistoryLogData.KEY_FILESIZE, FileTransferLog.FILESIZE);
        columnMapping.put(HistoryLogData.KEY_TRANSFERRED, FileTransferLog.TRANSFERRED);
        return columnMapping;
    }

    private static final Map<String, String> getImageSharingProviderColumnMapping() {
        Map<String, String> columnMapping = new HashMap<String, String>();
        columnMapping.put(HistoryLogData.KEY_PROVIDER_ID,
                String.valueOf(ImageSharingData.HISTORYLOG_MEMBER_ID));
        columnMapping.put(HistoryLogData.KEY_BASECOLUMN_ID, ImageSharingLog.BASECOLUMN_ID);
        columnMapping.put(HistoryLogData.KEY_ID, ImageSharingLog.SHARING_ID);
        columnMapping.put(HistoryLogData.KEY_MIME_TYPE, ImageSharingLog.MIME_TYPE);
        columnMapping.put(HistoryLogData.KEY_DIRECTION, ImageSharingLog.DIRECTION);
        columnMapping.put(HistoryLogData.KEY_CONTACT, ImageSharingLog.CONTACT);
        columnMapping.put(HistoryLogData.KEY_TIMESTAMP, ImageSharingLog.TIMESTAMP);
        columnMapping.put(HistoryLogData.KEY_STATUS, ImageSharingLog.STATE);
        columnMapping.put(HistoryLogData.KEY_REASON_CODE, ImageSharingLog.REASON_CODE);
        columnMapping.put(HistoryLogData.KEY_CONTENT, ImageSharingLog.FILE);
        columnMapping.put(HistoryLogData.KEY_FILENAME, ImageSharingLog.FILENAME);
        columnMapping.put(HistoryLogData.KEY_FILESIZE, ImageSharingLog.FILESIZE);
        columnMapping.put(HistoryLogData.KEY_TRANSFERRED, ImageSharingLog.TRANSFERRED);
        return columnMapping;
    }

    private static final Map<String, String> getVideoSharingProviderColumnMapping() {
        Map<String, String> columnMapping = new HashMap<String, String>();
        columnMapping.put(HistoryLogData.KEY_PROVIDER_ID,
                String.valueOf(VideoSharingData.HISTORYLOG_MEMBER_ID));
        columnMapping.put(HistoryLogData.KEY_BASECOLUMN_ID, VideoSharingLog.BASECOLUMN_ID);
        columnMapping.put(HistoryLogData.KEY_ID, VideoSharingLog.SHARING_ID);
        columnMapping.put(HistoryLogData.KEY_DIRECTION, VideoSharingLog.DIRECTION);
        columnMapping.put(HistoryLogData.KEY_CONTACT, VideoSharingLog.CONTACT);
        columnMapping.put(HistoryLogData.KEY_TIMESTAMP, VideoSharingLog.TIMESTAMP);
        columnMapping.put(HistoryLogData.KEY_STATUS, VideoSharingLog.STATE);
        columnMapping.put(HistoryLogData.KEY_REASON_CODE, VideoSharingLog.REASON_CODE);
        columnMapping.put(HistoryLogData.KEY_DURATION, VideoSharingLog.DURATION);
        return columnMapping;
    }

    private static final Map<String, String> getGeolocSharingProviderColumnMapping() {
        Map<String, String> columnMapping = new HashMap<String, String>();
        columnMapping.put(HistoryLogData.KEY_PROVIDER_ID,
                String.valueOf(GeolocSharingData.HISTORYLOG_MEMBER_ID));
        columnMapping.put(HistoryLogData.KEY_BASECOLUMN_ID, GeolocSharingLog.BASECOLUMN_ID);
        columnMapping.put(HistoryLogData.KEY_ID, GeolocSharingLog.SHARING_ID);
        columnMapping.put(HistoryLogData.KEY_DIRECTION, GeolocSharingLog.DIRECTION);
        columnMapping.put(HistoryLogData.KEY_CONTACT, GeolocSharingLog.CONTACT);
        columnMapping.put(HistoryLogData.KEY_TIMESTAMP, GeolocSharingLog.TIMESTAMP);
        columnMapping.put(HistoryLogData.KEY_STATUS, GeolocSharingLog.STATE);
        columnMapping.put(HistoryLogData.KEY_REASON_CODE, GeolocSharingLog.REASON_CODE);
        columnMapping.put(HistoryLogData.KEY_MIME_TYPE, GeolocSharingLog.MIME_TYPE);
        return columnMapping;
    }

}
