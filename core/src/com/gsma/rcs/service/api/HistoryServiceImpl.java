/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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

package com.gsma.rcs.service.api;

import com.gsma.rcs.provider.history.HistoryMemberBaseIdCreator;
import com.gsma.rcs.provider.history.HistoryProvider;
import com.gsma.rcs.provider.messaging.FileTransferData;
import com.gsma.rcs.provider.sharing.GeolocSharingData;
import com.gsma.rcs.provider.sharing.ImageSharingData;
import com.gsma.rcs.provider.sharing.VideoSharingData;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.history.HistoryLog;
import com.gsma.services.rcs.history.IHistoryService;

import android.content.ContentProvider;
import android.content.Context;
import android.net.Uri;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * History service implementation
 */
public class HistoryServiceImpl extends IHistoryService.Stub {

    private static final Set<Integer> INTERNAL_MEMBER_IDS = new HashSet<Integer>(Arrays.asList(
            ChatLog.Message.HISTORYLOG_MEMBER_ID, FileTransferData.HISTORYLOG_MEMBER_ID,
            ImageSharingData.HISTORYLOG_MEMBER_ID, VideoSharingData.HISTORYLOG_MEMBER_ID,
            GeolocSharingData.HISTORYLOG_MEMBER_ID));

    private static final Logger sLogger = Logger
            .getLogger(HistoryServiceImpl.class.getSimpleName());

    private final Context mCtx;

    private HistoryProvider mHistoryProvider;

    public HistoryServiceImpl(Context ctx) throws IOException {
        if (sLogger.isActivated()) {
            sLogger.info("History service API is loaded.");
        }
        mCtx = ctx;
    }

    private HistoryProvider retrieveHistoryLogProvider() {
        if (mHistoryProvider != null) {
            return mHistoryProvider;
        }
        String historyLogAuthority = HistoryLog.CONTENT_URI.getAuthority();
        ContentProvider provider = mCtx.getContentResolver()
                .acquireContentProviderClient(historyLogAuthority).getLocalContentProvider();
        mHistoryProvider = (HistoryProvider) provider;
        return mHistoryProvider;
    }

    /**
     * Registers an external history log member.
     * 
     * @param int Id of provider
     * @param Uri Database URI
     * @param String Table name
     * @param Map<String, String> Translator of history log field names to internal field names
     */
    /*
     * Unchecked cast must be suppressed since AIDL provides a raw Map type that must be cast.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void registerExtraHistoryLogMember(int providerId, Uri providerUri, Uri databaseUri,
            String table,
            /* Only raw map types are supported by AIDL. */
            @SuppressWarnings("rawtypes")
            Map columnMapping) {
        try {
            retrieveHistoryLogProvider().registerDatabase(providerId, providerUri, databaseUri,
                    table, columnMapping);
        } catch (IOException e) {
            throw new IllegalStateException("Error registering external provider " + providerId
                    + " with table " + table + " and database " + databaseUri, e);
        }
    }

    /**
     * Unregisters an external history log member.
     * 
     * @param int Id of history log member
     */
    @Override
    public void unregisterExtraHistoryLogMember(int providerId) {
        retrieveHistoryLogProvider().unregisterDatabaseByProviderId(providerId);
    }

    @Override
    public long createUniqueId(int providerId) {
        if (INTERNAL_MEMBER_IDS.contains(providerId)
                || providerId > HistoryProvider.MAX_ATTACHED_PROVIDERS) {
            /* TODO: This exception handling will be changed with CR037. */
            throw new IllegalStateException("Cannot create ID (not allowed) for internal provider "
                    + providerId);
        }
        return HistoryMemberBaseIdCreator.createUniqueId(mCtx, providerId);
    }

}
