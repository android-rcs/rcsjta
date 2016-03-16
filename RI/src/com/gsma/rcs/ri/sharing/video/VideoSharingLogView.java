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

package com.gsma.rcs.ri.sharing.video;

import com.gsma.rcs.api.connection.utils.RcsActivity;
import com.gsma.rcs.ri.R;
import com.gsma.rcs.ri.RiApplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;

/**
 * A class to view the persisted information for video sharing <br>
 * Created by Philippe LEMORDANT.
 */
public class VideoSharingLogView extends RcsActivity {
    private static final String EXTRA_SHARING_ID = "id";
    private String mSharingId;
    private TextView mTxtViewContact;
    private TextView mTxtViewDate;
    private TextView mTxtViewDir;
    private TextView mTxtViewDuration;
    private TextView mTxtViewEncoding;
    private TextView mTxtViewHeight;
    private TextView mTxtViewReason;
    private TextView mTxtViewState;
    private TextView mTxtViewWidth;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sharing_log_video_item);
        initialize();
        mSharingId = getIntent().getStringExtra(EXTRA_SHARING_ID);
    }

    private void initialize() {
        mTxtViewContact = (TextView) findViewById(R.id.history_log_item_contact);
        mTxtViewState = (TextView) findViewById(R.id.history_log_item_state);
        mTxtViewReason = (TextView) findViewById(R.id.history_log_item_reason);
        mTxtViewDir = (TextView) findViewById(R.id.history_log_item_direction);
        mTxtViewDate = (TextView) findViewById(R.id.history_log_item_date);
        mTxtViewDuration = (TextView) findViewById(R.id.history_log_item_duration);
        mTxtViewHeight = (TextView) findViewById(R.id.history_log_item_height);
        mTxtViewWidth = (TextView) findViewById(R.id.history_log_item_width);
        mTxtViewEncoding = (TextView) findViewById(R.id.history_log_item_encoding);
    }

    @Override
    protected void onResume() {
        super.onResume();
        VideoSharingDAO dao = VideoSharingDAO.getVideoSharingDAO(this, mSharingId);
        if (dao == null) {
            showMessageThenExit(R.string.error_item_not_found);
            return;
        }
        mTxtViewContact.setText(dao.getContact().toString());
        mTxtViewState.setText(RiApplication.sVideoSharingStates[dao.getState().toInt()]);
        mTxtViewReason.setText(RiApplication.sVideoReasonCodes[dao.getReasonCode().toInt()]);
        mTxtViewDir.setText(RiApplication.getDirection(dao.getDirection()));
        mTxtViewDate.setText(DateFormat.getInstance().format(new Date(dao.getTimestamp())));
        mTxtViewDuration.setText(DateUtils.formatElapsedTime(dao.getDuration() / 1000));
        mTxtViewHeight.setText(String.valueOf(dao.getHeight()));
        mTxtViewWidth.setText(String.valueOf(dao.getWidth()));
        mTxtViewEncoding.setText(dao.getVideoEncoding());
    }

    /**
     * Start activity to view details of video sharing record
     *
     * @param context the context
     * @param sharingId the sharing ID
     */
    public static void startActivity(Context context, String sharingId) {
        Intent intent = new Intent(context, VideoSharingLogView.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(EXTRA_SHARING_ID, sharingId);
        context.startActivity(intent);
    }
}
