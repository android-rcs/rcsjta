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

package com.gsma.rcs.provider;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * As the base column "_id" is mandatory and unique in all providers, this class is used to generate
 * the new id as "_id" cannot be primary key and AUTOINCREMENT cannot be used on other columns.
 */
public class ContentProviderBaseIdCreator {

    private static final String MAX_PROJECTION = new StringBuilder().append("MAX(")
            .append(BaseColumns._ID).append(")").toString();

    private static final Map<Uri, AtomicLong> sNextIds = new HashMap<Uri, AtomicLong>();

    public static long createUniqueId(Context ctx, Uri contentProviderUri) {

        AtomicLong nextId = sNextIds.get(contentProviderUri);

        if (nextId != null) {
            return nextId.incrementAndGet();
        }

        synchronized (sNextIds) {
            nextId = sNextIds.get(contentProviderUri);

            if (nextId != null) {
                return nextId.incrementAndGet();
            }

            nextId = new AtomicLong();

            Cursor cursor = null;
            try {
                cursor = ctx.getContentResolver().query(contentProviderUri, new String[] {
                    MAX_PROJECTION
                }, null, null, null);
                if (cursor.moveToNext()) {
                    nextId.set(cursor.getLong(0));
                }
            } finally {
                cursor.close();
            }

            sNextIds.put(contentProviderUri, nextId);

            return nextId.incrementAndGet();
        }
    }

}
