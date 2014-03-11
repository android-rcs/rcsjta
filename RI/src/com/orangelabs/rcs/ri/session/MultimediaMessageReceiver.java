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
package com.orangelabs.rcs.ri.session;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import com.gsma.services.rcs.session.MultimediaMessageIntent;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Multimedia message receiver
 * 
 * @author Jean-Marc AUFFRET
 */
public class MultimediaMessageReceiver extends BroadcastReceiver {
	/**
	 * UI handler
	 */
	private final Handler handler = new Handler();

	@Override
	public void onReceive(Context context, Intent intent) {
		final Context ctx = context;
		String contact = intent.getStringExtra(MultimediaMessageIntent.EXTRA_CONTACT);
		byte[] content = intent.getByteArrayExtra(MultimediaMessageIntent.EXTRA_CONTENT);
		final String msg = context.getString(R.string.label_recv_mm_msg, contact) + "\n" + new String(content);
		handler.post(new Runnable(){
			public void run(){
		        // Display received message
				Utils.displayLongToast(ctx, msg);
			}
		});
    }
}
