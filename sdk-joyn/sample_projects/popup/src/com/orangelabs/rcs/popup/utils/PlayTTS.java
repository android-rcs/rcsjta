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
package com.orangelabs.rcs.popup.utils;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;

/**
 * Play text-to-speech
 * 
 * @author Jean-Marc AUFFRET
 */
public class PlayTTS extends Service implements OnInitListener {
	private static final String TAG = "PlayTTS";

	/**
     * TTS engine 
     */
	private TextToSpeech tts = null;
	
	/**
	 * Message to be played
	 */
	private String message = null;
    
    @Override
    public void onCreate() {
    }
    
    @Override
    public IBinder onBind(Intent intent) {
		return null;
    }
    
    @Override
	public void onStart(Intent intent, int startId) {
		Log.v(TAG, "Start TTS");

		// Get parameters
		message = intent.getStringExtra("message");
		
		// Instanciate the TTS engine
		try {
			tts = new TextToSpeech(getApplicationContext(), this);
		} catch(Exception e) {
			Log.v(TAG, "Can't instanciate TTS engine");
			e.printStackTrace();
		}
	}
    
    @Override
    public void onDestroy() {
        super.onDestroy();

        // Deallocate TTS engine
		Log.v(TAG, "Shutdown TTS");
        if (tts != null) {
        	tts.shutdown();
        }
    }

    /**
     * TTS engine init
     * 
     * @param status Status
     */
    public void onInit(int status) {
		if ((tts != null) && (status == TextToSpeech.SUCCESS)) {
			Log.v(TAG, "TTS engine initialized with success");
			if (message != null) {
	    		// Speak
				Log.v(TAG, "Start TTS session");
		        tts.speak(message, TextToSpeech.QUEUE_ADD, null);
		        
		        // Wait end of speech
		        while(tts.isSpeaking()) {
		        	try {
		        		Thread.sleep(500);
		        	} catch(Exception e) {}
		        }
		        
		        // Stop the service
				Log.v(TAG, "Exit TTS session");
		        this.stopSelf();
			}
    	}
    }
}
