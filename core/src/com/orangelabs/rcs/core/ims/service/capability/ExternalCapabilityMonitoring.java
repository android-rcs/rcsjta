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

package com.orangelabs.rcs.core.ims.service.capability;

import com.orangelabs.rcs.provider.settings.RcsSettings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * External capability monitoring
 * 
 * @author jexa7410
 */
public class ExternalCapabilityMonitoring extends BroadcastReceiver {
    @Override
	public void onReceive(Context context, Intent intent) {
    	// Instanciate the settings manager
    	RcsSettings.createInstance(context);
    	
    	// Check if there are new RCS extensions installed or removed
		CapabilityUtils.updateExternalSupportedFeatures(context);
    }
}