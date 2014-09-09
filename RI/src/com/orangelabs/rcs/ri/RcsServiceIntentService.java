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
package com.orangelabs.rcs.ri;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.orangelabs.rcs.ri.utils.LogUtils;

/**
 * RCS service intent service
 * 
 * @author YPLO6403
 * 
 */
public class RcsServiceIntentService extends IntentService {

	/**
	 * The log tag for this class
	 */
	private static final String LOGTAG = LogUtils.getTag(RcsServiceIntentService.class.getSimpleName());

	public RcsServiceIntentService(String name) {
		super(name);
	}

	public RcsServiceIntentService() {
		super("RcsServiceIntentService");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		// We want this service to stop running if forced stop
		// so return not sticky.
		return START_NOT_STICKY;
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (LogUtils.isActive) {
			Log.d(LOGTAG, "RCS service is UP");
		}
		try {
			ApiConnectionManager.getInstance(this).connectApis();
		} catch (Exception e) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, e.getMessage(), e);
			}
		}
	}

}
