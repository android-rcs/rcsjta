/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.ri.utils;

import android.support.v4.util.LruCache;

/**
 * Created by yplo6403 on 07/12/2015.
 */
public class BitmapCache {

    private static volatile BitmapCache sInstance;
    private LruCache<String, BitmapLoader.BitmapCacheInfo> mMemoryCache;

    private BitmapCache() {
        /*
         * Get max available VM memory, exceeding this amount will throw an OutOfMemory exception.
         * Stored in kilobytes as LruCache takes an int in its constructor.
         */
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        /* Use 1/8th of the available memory for this memory cache. */
        final int cacheSize = maxMemory / 8;

        mMemoryCache = new LruCache<String, BitmapLoader.BitmapCacheInfo>(cacheSize) {

            /**
             * Measure item size in kilobytes rather than units which is more practical for a bitmap
             * cache
             */
            @Override
            protected int sizeOf(String key, BitmapLoader.BitmapCacheInfo bitmapCacheInfo) {
                /*
                 * The cache size will be measured in kilobytes rather than number of items.
                 */
                return bitmapCacheInfo.getBitmap().getByteCount() / 1024;
            }
        };
    }

    public static BitmapCache getInstance() {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (BitmapCache.class) {
            if (sInstance == null) {
                sInstance = new BitmapCache();
            }
            return sInstance;
        }
    }

    public LruCache<String, BitmapLoader.BitmapCacheInfo> getMemoryCache() {
        return mMemoryCache;
    }
}
