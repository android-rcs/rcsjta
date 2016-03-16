/*
 * ******************************************************************************
 *  * Software Name : RCS IMS Stack
 *  *
 *  * Copyright (C) 2010-2016 Orange.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *****************************************************************************
 */

package com.gsma.rcs.ri.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Log;

import java.io.IOException;

public class ImageUtils {

    private static final String LOGTAG = LogUtils.getTag(ImageUtils.class.getName());

    /**
     * Get the image bitmap from Image filename. If required, the image resolution is reduced and
     * rotation is performed.
     *
     * @param imageFilename the image filename
     * @return the bitmap
     */
    public static Bitmap getImageBitmap2Display(String imageFilename) {
        BitmapFactory.Options options = readImageOptions(imageFilename);
        if (options.outHeight > options.outWidth) {
            return getImageBitmap2Display(imageFilename, 256, 128);// if landscape
        }
        return getImageBitmap2Display(imageFilename, 128, 256);// if portrait
    }

    private static BitmapFactory.Options readImageOptions(String imageFilename) {
        /*
         * Read the dimensions and type of the image data prior to construction (and memory
         * allocation) of the bitmap
         */
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        /* Decode image just to display dimension */
        BitmapFactory.decodeFile(imageFilename, options);
        return options;
    }

    public static Bitmap getImageBitmap2Display(String imageFilename, int reqWidth, int reqHeight) {
        BitmapFactory.Options options = readImageOptions(imageFilename);
        /* Calculate the reduction factor */
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        int loopCount = 1;
        for (; loopCount++ <= 4;) {
            try {
                if (LogUtils.isActive) {
                    Log.i(LOGTAG, "bitmap: " + imageFilename + " Sample factor: "
                            + options.inSampleSize);
                }
                /* Rotate image is orientation is not 0 degree */
                Bitmap bitmapTmp = BitmapFactory.decodeFile(imageFilename, options);
                if (bitmapTmp == null) {
                    return null;
                }
                int degree = getExifOrientation(imageFilename);
                if (degree == 0) {
                    return bitmapTmp;
                }
                return rotateBitmap(bitmapTmp, degree);

            } catch (OutOfMemoryError e) {
                /*
                 * If an OutOfMemoryError occurred, we continue with for loop and next inSampleSize
                 * value
                 */
                if (LogUtils.isActive) {
                    Log.w(LOGTAG, "OutOfMemoryError: options.inSampleSize= " + options.inSampleSize);
                }
                options.inSampleSize++;
            }
        }
        return null;
    }

    /**
     * Calculate the sub-sampling factor to load image on screen Image will be sized to be the
     * smallest possible, keeping it bigger than requested size. No resize factor will be applied if
     * one or both dimensions are smallest than requested. If width and height are both bigger than
     * requested, ensure final image will have both dimensions larger than or equal to the requested
     * height and width
     * <p/>
     * A requested dimension set to 1 pixel will let its size to adapt with no constraints.
     *
     * @param options contains the raw height and width of image
     * @param reqWidth the target width
     * @param reqHeight the target height
     * @return the sub-sampling factor
     */
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth,
            int reqHeight) {
        /* Raw height and width of image */
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            /*
             * Calculate the largest inSampleSize value that is a power of 2 and keeps both height
             * and width larger than the requested height and width.
             */
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    /**
     * Read the EXIF data from a given file to know the corresponding rotation, if any
     *
     * @param filename The filename
     * @return rotation in degree
     */
    private static int getExifOrientation(String filename) {
        // Also check if the image has to be rotated by reading metadata
        try {
            ExifInterface exif = new ExifInterface(filename);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
            if (orientation != -1) {
                // We only recognize a subset of orientation tag values.
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        return 90;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        return 180;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        return 270;
                }
            }
            return 0;
        } catch (IOException ex) {
            if (LogUtils.isActive) {
                Log.w(LOGTAG, "Cannot read exif", ex);
            }
            return 0;
        }
    }

    /**
     * Rotates the bitmap by the specified degree If a new bitmap is created, the original bitmap is
     * recycled.
     *
     * @param b bitmap
     * @param degrees number of degree to rotate
     * @return bitmap
     */
    private static Bitmap rotateBitmap(Bitmap b, int degrees) {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "Rotate bitmap degrees: " + degrees);
        }
        Matrix m = new Matrix();
        m.setRotate(degrees, (float) b.getWidth() / 2, (float) b.getHeight() / 2);
        try {
            Bitmap b2 = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
            if (b != b2) {
                b.recycle();
                b = b2;
            }
            return b;
        } catch (OutOfMemoryError ex) {
            if (LogUtils.isActive) {
                // We have no memory to rotate. Return the original bitmap.
                Log.w(LOGTAG, "OutOfMemoryError: cannot rotate image");
            }
            System.gc();
            return b;
        }
    }

}
