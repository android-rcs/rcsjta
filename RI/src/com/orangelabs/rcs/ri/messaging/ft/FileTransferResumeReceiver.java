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

package com.orangelabs.rcs.ri.messaging.ft;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.gsma.services.rcs.ft.FileTransfer;
import com.gsma.services.rcs.ft.FileTransferIntent;
import com.orangelabs.rcs.ri.utils.LogUtils;

/**
 * File transfer resume receiver
 * 
 * @author YPLO6403
 *
 */
public class FileTransferResumeReceiver extends BroadcastReceiver {
	/**
	 * Action FT is resuming
	 */
	public static final String ACTION_FT_RESUME = "FT_RESUME";
	
    /**
	 * The log tag for this class
	 */
	private static final String LOGTAG = LogUtils.getTag(FileTransferResumeReceiver.class.getSimpleName());
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (LogUtils.isActive) {
			Log.d(LOGTAG, "onReceive");
		}
		// FT is resuming so already accepted
		int dir = intent.getIntExtra(FileTransferIntent.EXTRA_DIRECTION,FileTransfer.Direction.INCOMING);
		Intent intentLocal = new Intent(intent);
		if (dir == FileTransfer.Direction.INCOMING) {
			intentLocal.setClass(context, ReceiveFileTransfer.class);
		} else {
			intentLocal.setClass(context, InitiateFileTransfer.class);
		}
		intentLocal.addFlags(Intent.FLAG_FROM_BACKGROUND);
		intentLocal.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
		intentLocal.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intentLocal.setAction(ACTION_FT_RESUME);
		context.startActivity(intentLocal);
	}
}
