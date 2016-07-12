/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2016 Orange.
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
 *
 ******************************************************************************/

package com.gsma.rcs.ri.messaging.filetransfer;

import com.gsma.rcs.api.connection.utils.RcsActivity;
import com.gsma.rcs.ri.R;
import com.gsma.rcs.ri.RiApplication;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;

/**
 * A class to view the persisted information for chat message<br>
 * Created by Philippe LEMORDANT.
 */
public class FileTransferLogView extends RcsActivity {
    private static final String EXTRA_FT_ID = "id";
    private String mFileTransferId;
    private TextView mTxtViewChatId;
    private TextView mTxtViewContact;
    private TextView mTxtViewDate;
    private TextView mTxtViewDir;
    private TextView mTxtViewFileSize;
    private TextView mTxtViewFilename;
    private TextView mTxtViewMime;
    private TextView mTxtViewReason;
    private TextView mTxtViewState;
    private TextView mTxtViewTransferred;
    private TextView mTxtViewUri;
    private TextView mTxtViewDateSent;
    private TextView mTxtViewDateDelivered;
    private TextView mTxtViewDateDisplayed;
    private TextView mTxtViewRead;
    private TextView mTxtViewExpiredDelivery;
    private static DateFormat sDateFormat;
    private TextView mTxtViewFileExpiration;
    private TextView mTxtViewIconExpiration;
    private TextView mTxtViewIconUri;
    private TextView mTxtViewIconMime;
    private TextView mTxtViewId;
    private TextView mTxtViewDisposition;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.filetransfer_log_item);
        initialize();
        mFileTransferId = getIntent().getStringExtra(EXTRA_FT_ID);
    }

    private void initialize() {
        mTxtViewId = (TextView) findViewById(R.id.history_log_item_id);
        mTxtViewChatId = (TextView) findViewById(R.id.history_log_item_chat_id);
        mTxtViewContact = (TextView) findViewById(R.id.history_log_item_contact);
        mTxtViewState = (TextView) findViewById(R.id.history_log_item_state);
        mTxtViewReason = (TextView) findViewById(R.id.history_log_item_reason);
        mTxtViewDir = (TextView) findViewById(R.id.history_log_item_direction);
        mTxtViewDate = (TextView) findViewById(R.id.history_log_item_date);
        mTxtViewMime = (TextView) findViewById(R.id.history_log_item_mime);
        mTxtViewFilename = (TextView) findViewById(R.id.history_log_item_filename);
        mTxtViewFileSize = (TextView) findViewById(R.id.history_log_item_size);
        mTxtViewUri = (TextView) findViewById(R.id.history_log_item_uri);
        mTxtViewTransferred = (TextView) findViewById(R.id.history_log_item_transferred);
        mTxtViewDateSent = (TextView) findViewById(R.id.history_log_item_date_sent);
        mTxtViewDateDelivered = (TextView) findViewById(R.id.history_log_item_date_delivered);
        mTxtViewDateDisplayed = (TextView) findViewById(R.id.history_log_item_date_displayed);
        mTxtViewRead = (TextView) findViewById(R.id.history_log_item_read_status);
        mTxtViewExpiredDelivery = (TextView) findViewById(R.id.history_log_item_expired_delivery);
        mTxtViewFileExpiration = (TextView) findViewById(R.id.history_log_item_date_file_expiration);
        mTxtViewIconExpiration = (TextView) findViewById(R.id.history_log_item_date_icon_expiration);
        mTxtViewIconUri = (TextView) findViewById(R.id.history_log_item_icon);
        mTxtViewIconMime = (TextView) findViewById(R.id.history_log_item_icon_mime);
        mTxtViewDisposition = (TextView) findViewById(R.id.history_log_item_disposition);
    }

    private String getDateFromDb(long timestamp) {
        if (0 == timestamp) {
            return "";
        }
        if (sDateFormat == null) {
            sDateFormat = DateFormat.getInstance();
        }
        return sDateFormat.format(new Date(timestamp));
    }

    @Override
    protected void onResume() {
        super.onResume();
        FileTransferDAO dao = FileTransferDAO.getFileTransferDAO(this, mFileTransferId);
        if (dao == null) {
            showMessageThenExit(R.string.error_item_not_found);
            return;
        }
        mTxtViewId.setText(mFileTransferId);
        mTxtViewChatId.setText(dao.getChatId());
        ContactId contact = dao.getContact();
        if (contact != null) {
            mTxtViewContact.setText(contact.toString());
        } else {
            mTxtViewContact.setText("");
        }
        mTxtViewState.setText(RiApplication.sFileTransferStates[dao.getState().toInt()]);
        mTxtViewReason.setText(RiApplication.sFileTransferReasonCodes[dao.getReasonCode().toInt()]);
        mTxtViewDir.setText(RiApplication.getDirection(dao.getDirection()));
        mTxtViewDate.setText(getDateFromDb(dao.getTimestamp()));
        mTxtViewDateSent.setText(getDateFromDb(dao.getTimestampSent()));
        mTxtViewDateDelivered.setText(getDateFromDb(dao.getTimestampDelivered()));
        mTxtViewDateDisplayed.setText(getDateFromDb(dao.getTimestampDisplayed()));
        mTxtViewRead.setText(dao.getReadStatus().toString());
        mTxtViewExpiredDelivery.setText(Boolean.toString(dao.isExpiredDelivery()));
        mTxtViewMime.setText(dao.getMimeType());
        mTxtViewFilename.setText(dao.getFilename());
        mTxtViewFileSize.setText(String.valueOf(dao.getSize()));
        mTxtViewUri.setText(dao.getFile().toString());
        mTxtViewTransferred.setText(String.valueOf(dao.getSizeTransferred()));
        mTxtViewFileExpiration.setText(getDateFromDb(dao.getFileExpiration()));
        mTxtViewIconExpiration.setText(getDateFromDb(dao.getFileIconExpiration()));
        mTxtViewIconUri.setText(dao.getThumbnail() == null ? "" : dao.getThumbnail().toString());
        mTxtViewIconMime
                .setText(dao.getIconMimeType() == null ? "" : dao.getThumbnail().toString());
        mTxtViewDisposition.setText(dao.getDisposition().toString());
    }

    /**
     * Start activity to view details of file transfer record
     *
     * @param context the context
     * @param fileTransferId the file transfer ID
     */
    public static void startActivity(Context context, String fileTransferId) {
        Intent intent = new Intent(context, FileTransferLogView.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(EXTRA_FT_ID, fileTransferId);
        context.startActivity(intent);
    }
}
