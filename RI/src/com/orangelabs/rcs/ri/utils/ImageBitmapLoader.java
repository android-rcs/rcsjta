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

import com.orangelabs.rcs.ri.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.util.LruCache;
import android.util.Log;

/**
 * @author yplo6403
 *         <p/>
 *         A class to create Image bitmap and to store it in cache asynchronously.
 */
public class ImageBitmapLoader extends BitmapLoader {
    private static final String LOGTAG = LogUtils.getTag(ImageBitmapLoader.class.getSimpleName());

    private static Bitmap defaultImageBitmap;

    /**
     * @param memoryCache the map to save the image bitmap descriptor
     * @param callback the callback to be executed once the bitmap is built
     */
    public ImageBitmapLoader(Context ctx, LruCache<String, BitmapCacheInfo> memoryCache,
            int pixelMaxSizeWidth, int pixelMaxSizeHeight, SetViewCallback callback) {
        super(ctx, memoryCache, pixelMaxSizeWidth, pixelMaxSizeHeight, callback);
    }

    @Override
    protected BitmapCacheInfo doInBackground(String... params) {
        String imagePath = params[0];
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "doInBackground build bitmap for image:" + imagePath);
        }
        try {
            BitmapCacheInfo defaultResult;
            if (defaultImageBitmap == null) {
                defaultImageBitmap = BitmapFactory.decodeResource(mCtx.getResources(),
                        R.drawable.video_file);
            }
            if (defaultImageBitmap != null) {
                defaultResult = new BitmapCacheInfo(imagePath, defaultImageBitmap);
                mMemoryCache.put(imagePath, defaultResult);
            }
            Bitmap imageBitmap = ImageUtils.getImageBitmap2Display(imagePath, mPixelMaxSizeWidth,
                    mPixelMaxSizeHeight);
            if (imageBitmap != null) {
                BitmapCacheInfo bitmapCacheInfo = new BitmapCacheInfo(imagePath, imageBitmap);
                // Store the bitmap and path for an eventual later use
                if (imageBitmap.getHeight() != 1 && imageBitmap.getWidth() != 1) {
                    mMemoryCache.put(imagePath, bitmapCacheInfo);
                    return bitmapCacheInfo;
                }
            }
            return null;

        } catch (OutOfMemoryError oome) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "OOME while getting bitmap for image: " + imagePath);
            }
        }
        return null;
    }

}
