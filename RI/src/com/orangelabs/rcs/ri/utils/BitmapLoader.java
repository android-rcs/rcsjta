/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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

package com.orangelabs.rcs.ri.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.util.Log;

/**
 * @author yplo6403
 *         <p/>
 *         A class to create bitmap and to store it cache asynchronously.
 */
public abstract class BitmapLoader extends AsyncTask<String, Void, BitmapLoader.BitmapCacheInfo> {

    private static final String LOGTAG = LogUtils.getTag(BitmapLoader.class.getSimpleName());

    protected LruCache<String, BitmapCacheInfo> mMemoryCache;
    protected SetViewCallback mCallback;
    protected Context mCtx;
    protected int mPixelMaxSizeWidth;
    protected int mPixelMaxSizeHeight;

    public BitmapLoader(Context ctx, LruCache<String, BitmapCacheInfo> memoryCache,
            int pixelMaxSizeWidth, int pixelMaxSizeHeight, SetViewCallback callback) {
        super();
        mCtx = ctx;
        mMemoryCache = memoryCache;
        mCallback = callback;
        mPixelMaxSizeWidth = pixelMaxSizeWidth;
        mPixelMaxSizeHeight = pixelMaxSizeHeight;
    }

    @Override
    protected void onPostExecute(BitmapCacheInfo result) {
        if (result != null) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "onPostExecute build bitmap for:" + result.getFilename());
            }
            if (mCallback != null) {
                if (result.getBitmap() != null)
                    mCallback.loadView(result);
            }
        }
    }

    public interface SetViewCallback {
        void loadView(final BitmapCacheInfo cacheInfo);
    }

    /**
     * @author yplo6403
     *         <p/>
     *         This inner class creates a bitmap for the bitmap and store it with its associated
     *         filename
     */
    public class BitmapCacheInfo {
        private final String mFilename;
        private final Bitmap mBitmap;

        public BitmapCacheInfo(String filename, Bitmap bitmap) {
            mFilename = filename;
            mBitmap = bitmap;
        }

        public Bitmap getBitmap() {
            return mBitmap;
        }

        public String getFilename() {
            return mFilename;
        }

    }

}
