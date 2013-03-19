/*
 * Copyright 2013, France Telecom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gsma.joyn.media.video;

/**
 * Class VideoSurfaceView.
 *
 * @author Jean-Marc AUFFRET (Orange)
 * @version 1.0
 * @since 1.0
 */
public class VideoSurfaceView extends android.view.SurfaceView {
    /**
     * Constant NO_RATIO.
     */
    public static final float NO_RATIO = 0.0f;

    /**
     * Creates a new instance of VideoSurfaceView.
     *
     * @param 
     */
    public VideoSurfaceView(android.content.Context context) {
        super((android.content.Context) null);
    }

    /**
     * Creates a new instance of VideoSurfaceView.
     *
     * @param 
     * @param 
     */
    public VideoSurfaceView(android.content.Context context, android.util.AttributeSet attrs) {
        super((android.content.Context) null);
    }

    /**
     * Creates a new instance of VideoSurfaceView.
     *
     * @param 
     * @param 
     * @param 
     */
    public VideoSurfaceView(android.content.Context context, android.util.AttributeSet attrs, int defStyle) {
        super((android.content.Context) null);
    }

    public void clearImage() {

    }

    /**
     * Sets the aspect ratio.
     *
     * @param 
     * @param 
     */
    public void setAspectRatio(int width, int height) {

    }

    /**
     * Sets the aspect ratio.
     *
     * @param 
     */
    public void setAspectRatio(float ratio) {

    }

    /**
     * Sets the image.
     *
     * @param 
     */
    public void setImage(android.graphics.Bitmap bmp) {

    }

    /**
     *
     * @param 
     * @param 
     */
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

    }

}
