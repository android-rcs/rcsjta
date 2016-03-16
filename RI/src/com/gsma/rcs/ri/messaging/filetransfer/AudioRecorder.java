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

package com.gsma.rcs.ri.messaging.filetransfer;

import com.gsma.rcs.ri.utils.LogUtils;

import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class AudioRecorder extends MediaRecorder {

    private static final String LOGTAG = LogUtils.getTag(AudioRecorder.class.getSimpleName());

    private static final String AUDIO_FILE_DIR = "/ri/audios";
    private static final String AUDIO_FILE_EXTENSION = ".mp4";
    private static final String AUDIO_FILE_PREFIX = "audio_";

    private final long mMaxAudioDuration;
    private final IAudioMessageRecordListener mListener;
    private Uri mFile;
    private MediaRecorder mCurrentRecord;
    private boolean mStartRecording;

    /**
     * Constructor
     *
     * @param maxAudioDuration maximum duration
     * @param listener callback
     */
    public AudioRecorder(long maxAudioDuration, IAudioMessageRecordListener listener) {
        mMaxAudioDuration = maxAudioDuration;
        mListener = listener;
    }

    private MediaRecorder createMediaRecorder() {
        MediaRecorder record = new MediaRecorder();
        mFile = createAudioUri();
        record.setAudioSource(AudioSource.VOICE_COMMUNICATION);
        record.setOutputFormat(OutputFormat.DEFAULT);
        record.setAudioEncoder(AudioEncoder.AMR_WB);
        record.setOutputFile(mFile.getPath());
        record.setMaxDuration((int) mMaxAudioDuration);
        record.setOnInfoListener(new OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mr, int what, int extra) {
                if (MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED == what) {
                    mStartRecording = false;
                    mr.stop();
                    mr.reset();
                    mListener.onMaxDurationReached();
                }
            }
        });
        return record;
    }

    /**
     * Launches audio recording
     * 
     * @throws IOException
     */
    public void launchRecord() throws IOException {
        mCurrentRecord = createMediaRecorder();
        mCurrentRecord.prepare();
        mCurrentRecord.start();
        mStartRecording = true;
    }

    /**
     * Stops audio recording
     */
    public void stopRecord() {
        if (mCurrentRecord != null) {
            if (mStartRecording) {
                mStartRecording = false;
                mCurrentRecord.stop();
                mCurrentRecord.reset();
            }
        }
    }

    /**
     * Generates a random filename for audio file
     */
    private Uri createAudioUri() {
        File audio = new File(getAudioDirectory() + "/" + AUDIO_FILE_PREFIX
                + System.currentTimeMillis() + AUDIO_FILE_EXTENSION);
        Uri result = Uri.fromFile(audio);
        if (LogUtils.isActive) {
            Log.w(LOGTAG, "Audio file Uri=".concat(result.toString()));
        }
        return result;
    }

    public Uri getFile() {
        return mFile;
    }

    /**
     * Interface to notify AudioMessageRecord events
     */
    public interface IAudioMessageRecordListener {
        /**
         * Called when max duration is reached
         */
        void onMaxDurationReached();
    }

    /**
     * Get sent audio root directory
     *
     * @return Path of audio directory
     */
    private String getAudioDirectory() {
        File directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                .concat(AUDIO_FILE_DIR));
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return directory.getPath();
    }
}
