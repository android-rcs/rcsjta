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

import com.gsma.services.rcs.history.HistoryLog;

import android.content.Context;
import android.util.SparseArray;

import java.util.concurrent.atomic.AtomicLong;

/**
 * As the base column "_id" is unique across all content providers that are history members, this
 * class is used to generate this id.
 */
public class HistoryMemberBaseIdCreator {

    private static final long RANGE_SIZE = Long.MAX_VALUE / HistoryProvider.MAX_ATTACHED_PROVIDERS;

    private static SparseArray<AtomicLong> sNextIds = new SparseArray<AtomicLong>();

    public static long createUniqueId(Context ctx, int memberId) {
        AtomicLong nextId = sNextIds.get(memberId);

        if (nextId != null) {
            return nextId.incrementAndGet();
        }

        synchronized (sNextIds) {
            nextId = sNextIds.get(memberId);

            if (nextId != null) {
                return nextId.incrementAndGet();
            }

            String historyLogAuthority = HistoryLog.CONTENT_URI.getAuthority();
            HistoryProvider provider = (HistoryProvider) ctx.getContentResolver()
                    .acquireContentProviderClient(historyLogAuthority).getLocalContentProvider();
            long maxId = provider.getMaxId(memberId);
            if (maxId == 0) {
                maxId = (memberId - 1) * RANGE_SIZE;
            }
            nextId = new AtomicLong(maxId);
            sNextIds.put(memberId, nextId);
            nextId.set(maxId);
            return nextId.incrementAndGet();

        }
    }

}
