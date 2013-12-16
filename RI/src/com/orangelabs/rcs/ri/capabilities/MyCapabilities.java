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

package com.orangelabs.rcs.ri.capabilities;

import java.util.Set;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.TextView;

import com.gsma.services.rcs.JoynService;
import com.gsma.services.rcs.JoynServiceException;
import com.gsma.services.rcs.JoynServiceListener;
import com.gsma.services.rcs.JoynServiceNotAvailableException;
import com.gsma.services.rcs.capability.Capabilities;
import com.gsma.services.rcs.capability.CapabilityService;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * My capabilities
 */
public class MyCapabilities extends Activity implements JoynServiceListener {
    /**
	 * Capability API
	 */
    private CapabilityService capabilityApi;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.capabilities_mine);
        
        // Set title
        setTitle(R.string.menu_my_capabilities);
        
        // Instanciate API
        capabilityApi = new CapabilityService(getApplicationContext(), this);

        // Connect API
        capabilityApi.connect();
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	
		// Disconnect API
    	capabilityApi.disconnect();
    }
    
    /**
     * Callback called when service is connected. This method is called when the
     * service is well connected to the RCS service (binding procedure successfull):
     * this means the methods of the API may be used.
     */
    public void onServiceConnected() {
    	try {
    		// Get the current capabilities from the RCS contacts API
        	Capabilities capabilities = capabilityApi.getMyCapabilities();
	    	
	    	// Set capabilities
	        CheckBox imageCSh = (CheckBox)findViewById(R.id.image_sharing);
	        imageCSh.setChecked(capabilities.isImageSharingSupported());
	        CheckBox videoCSh = (CheckBox)findViewById(R.id.video_sharing);
	        videoCSh.setChecked(capabilities.isVideoSharingSupported());
	        CheckBox ft = (CheckBox)findViewById(R.id.file_transfer);
	        ft.setChecked(capabilities.isFileTransferSupported());
	        CheckBox im = (CheckBox)findViewById(R.id.im);
	        im.setChecked(capabilities.isImSessionSupported());
	        CheckBox geolocationPush = (CheckBox)findViewById(R.id.geoloc_push);
	        geolocationPush.setChecked(capabilities.isGeolocPushSupported());
	        CheckBox ipVoiceCall = (CheckBox)findViewById(R.id.ip_voice_call);
	        ipVoiceCall.setChecked(capabilities.isIPVoiceCallSupported());
	        CheckBox ipVideoCall = (CheckBox)findViewById(R.id.ip_video_call);
	        ipVideoCall.setChecked(capabilities.isIPVideoCallSupported());
	        
	        // Set extensions
	        TextView extensions = (TextView)findViewById(R.id.extensions);
	        String result = "";
	        Set<String> extensionList = capabilities.getSupportedExtensions();
	        for(String value : extensionList) {
	        	result += value + "\n";
	        }
	        extensions.setText(result);
	    } catch(JoynServiceNotAvailableException e) {
	    	e.printStackTrace();
			Utils.showMessageAndExit(MyCapabilities.this, getString(R.string.label_api_disabled));
	    } catch(JoynServiceException e) {
	    	e.printStackTrace();
			Utils.showMessageAndExit(MyCapabilities.this, getString(R.string.label_api_failed));
	    }
    }

    /**
     * Callback called when service has been disconnected. This method is called when
     * the service is disconnected from the RCS service (e.g. service deactivated).
     * 
     * @param error Error
     * @see JoynService.Error
     */
    public void onServiceDisconnected(int error) {
		Utils.showMessageAndExit(MyCapabilities.this, getString(R.string.label_api_disabled));
    }
}
