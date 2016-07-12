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

import com.gsma.rcs.api.connection.ConnectionManager;
import com.gsma.rcs.api.connection.utils.RcsActivity;
import com.gsma.rcs.ri.R;
import com.gsma.rcs.ri.utils.FileUtils;
import com.gsma.rcs.ri.utils.LogUtils;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.filetransfer.FileTransferServiceConfiguration;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * @author Philippe LEMORDANT
 * @author Sandrine LACHARME
 */
public class AudioMessageRecordActivity extends RcsActivity {

    private static final String LOGTAG = LogUtils.getTag(AudioRecorder.class.getName());
    private Button mBtnPlay;
    private Button mBtnStop;
    private Button mBtnRecord;
    private TextView mTextUri;
    private TextView mTextSize;
    private TextView mTextDuration;
    private boolean mRecording;
    private boolean mPlaying;
    private RcsActivity mThisActivity;
    private AudioMediaPlayer mAudioPlayer;
    private AudioRecorder mAudioRecorder;
    private Uri mFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.audio_msg_record);

        initialize();

        startMonitorServices(ConnectionManager.RcsServiceName.FILE_TRANSFER);
        /** Register to API manager */
        if (!isServiceConnected(ConnectionManager.RcsServiceName.FILE_TRANSFER)) {
            showMessageThenExit(R.string.label_service_not_available);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAudioPlayer != null) {
            mAudioPlayer.release();
        }
        if (mAudioRecorder != null) {
            mAudioRecorder.stopRecord();
            mAudioRecorder.release();
        }
    }

    private boolean deleteAudioRecord(Uri file) {
        File fileToDelete = new File(file.getPath());
        return !fileToDelete.exists() || fileToDelete.delete();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_BACK == keyCode && mAudioRecorder != null) {
            mAudioRecorder.stopRecord();
            mFile = mAudioRecorder.getFile();
            if (mFile != null) {
                MediaScannerConnection.scanFile(mThisActivity, new String[] {
                    mFile.getPath()
                }, null, null);
                Intent in = new Intent();
                in.setData(mFile);
                setResult(Activity.RESULT_OK, in);
                mThisActivity.finish();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void displayAudioFileInfo() {
        mTextUri.setText(FileUtils.getFileName(mThisActivity, mFile));
        long duration = AudioMediaPlayer.getDuration(mThisActivity, mFile) / 1000L;
        mTextDuration.setText(String.format(Locale.getDefault(), "%d", duration));
        long size = FileUtils.getFileSize(mThisActivity, mFile);
        mTextSize.setText(FileUtils.humanReadableByteCount(size, true));
        if (LogUtils.isActive) {
            Log.w(LOGTAG, "Audio recorded file='" + mFile + "' duration(sec)=" + duration
                    + " size=" + size);
        }
    }

    private void initialize() {
        mThisActivity = this;
        mBtnPlay = (Button) findViewById(R.id.buttonPlay);
        mBtnStop = (Button) findViewById(R.id.buttonStop);
        mBtnRecord = (Button) findViewById(R.id.buttonRecord);
        mTextDuration = (TextView) findViewById(R.id.duration);
        mTextUri = (TextView) findViewById(R.id.uri);
        mTextSize = (TextView) findViewById(R.id.size);
        mBtnStop.setEnabled(false);
        mBtnPlay.setEnabled(false);
        mBtnRecord.setEnabled(true);

        AudioRecorder.IAudioMessageRecordListener listenerRecorder = new AudioRecorder.IAudioMessageRecordListener() {

            @Override
            public void onMaxDurationReached() {
                mBtnStop.setEnabled(false);
                mBtnRecord.setEnabled(true);
                mBtnPlay.setEnabled(true);
                mRecording = false;
                mThisActivity.showMessage(R.string.max_audio_record_reached);
                mFile = mAudioRecorder.getFile();
                displayAudioFileInfo();
            }

        };
        try {
            FileTransferServiceConfiguration config = getFileTransferApi().getConfiguration();
            mAudioRecorder = new AudioRecorder(config.getMaxAudioMessageDuration(),
                    listenerRecorder);

        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
            return;
        }
        mBtnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (mFile != null) {
                        deleteAudioRecord(mFile);
                    }
                    mBtnStop.setEnabled(true);
                    mBtnPlay.setEnabled(false);
                    mBtnRecord.setEnabled(false);

                    mTextUri.setText("");
                    mTextDuration.setText("");
                    mTextSize.setText("");

                    mAudioRecorder.launchRecord();
                    mRecording = true;
                    mPlaying = false;

                } catch (IOException e) {
                    mThisActivity.showExceptionThenExit(e);
                }
            }
        });

        mBtnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBtnStop.setEnabled(false);
                mBtnRecord.setEnabled(true);
                mBtnPlay.setEnabled(true);
                if (mRecording) {
                    mRecording = false;
                    mAudioRecorder.stopRecord();
                    mFile = mAudioRecorder.getFile();
                    displayAudioFileInfo();
                    return;
                }
                if (mPlaying) {
                    mPlaying = false;
                    mAudioPlayer.stopPlay();
                }
            }
        });

        final AudioMediaPlayer.IAudioPlayerListener listenerPlayer = new AudioMediaPlayer.IAudioPlayerListener() {

            @Override
            public void onCompletion() {
                mBtnStop.setEnabled(false);
                mBtnRecord.setEnabled(true);
                mBtnPlay.setEnabled(true);
                mPlaying = false;
            }
        };

        mBtnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBtnStop.setEnabled(true);
                mBtnRecord.setEnabled(false);
                mBtnPlay.setEnabled(false);
                mPlaying = true;
                try {
                    mAudioPlayer = new AudioMediaPlayer(mThisActivity, mFile, listenerPlayer);
                    mAudioPlayer.startPlay();

                } catch (IOException e) {
                    mThisActivity.showExceptionThenExit(e);
                }
            }

        });
    }
}
