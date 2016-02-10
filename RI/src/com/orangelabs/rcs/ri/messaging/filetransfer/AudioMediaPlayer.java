/*******************************************************************************
 * Software Name : RCS IMS Stack
 * <p/>
 * Copyright (C) 2010-2016 Orange.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.orangelabs.rcs.ri.messaging.filetransfer;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import com.orangelabs.rcs.ri.utils.LogUtils;

import java.io.IOException;

/**
 * Created by Sandrine Lacharme on 12/01/2016.
 */
public class AudioMediaPlayer extends MediaPlayer {

    private static final String LOGTAG = LogUtils.getTag(AudioRecorder.class.getSimpleName());
    private final Uri mFile;
    private final Context mCtx;
    private final IAudioPlayerListener mListener;

    /**
     * Constructor
     */
    public AudioMediaPlayer(Context ctx, Uri file, IAudioPlayerListener listener) {
        super();
        mCtx = ctx;
        mFile = file;
        mListener = listener;
    }

    /**
     * Start playing an audio file
     */
    public void startPlay() throws IOException {
        setDataSource(mCtx, mFile);
        prepareAsync();
        setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                setVolume(1.0f, 1.0f);
                mp.start();
            }
        });
        setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.reset();
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onCompletion");
                }
                mListener.onCompletion();
            }
        });

    }

    /**
     * Stop playing an audio file and reset the peripheral
     */
    public void stopPlay() {
        stop();
        reset();
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "stopPlay");
        }
    }

    /**
     * Gets audio file duration
     * 
     * @param ctx the context
     * @param file the file Uri
     * @return the duration (msec)
     */
    public static long getDuration(Context ctx, Uri file) {
        MediaPlayer mp = MediaPlayer.create(ctx, file);
        if (mp == null) {
            return -1;
        }
        int duration = mp.getDuration();
        mp.release();
        return duration;
    }

    /**
     * Interface to notify AudioPlayer events
     */
    public interface IAudioPlayerListener {
        /**
         * Called when record is played
         */
        void onCompletion();
    }
}
