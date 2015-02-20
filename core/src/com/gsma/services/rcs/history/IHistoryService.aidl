package com.gsma.services.rcs.history;

import android.net.Uri;

/**
 * History service API
 */
interface IHistoryService {

    void registerExtraHistoryLogMember(in int providerId, in Uri providerUri, in Uri databaseUri, in String table, in Map columnMapping);

    void unregisterExtraHistoryLogMember(in int providerId);

    long createUniqueId(in int providerId);
}
