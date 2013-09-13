package com.orangelabs.rcs.tts;

import java.util.ArrayList;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;

/**
 * Play text-to-speech
 * 
 * @author jexa7410
 */
public class PlayTextToSpeech extends Service implements OnInitListener {
	private static final String TAG = "PlayTextToSpeech";

	/**
     * TTS engine 
     */
	private TextToSpeech tts = null;
	
	/**
	 * List of messages to be played
	 */
	private ArrayList<String> messages = null;
    
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
		messages = intent.getStringArrayListExtra("messages");
		
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
			if ((messages != null) && (messages.size() > 0)) {
	    		// Speak
				Log.v(TAG, "Start TTS session: play " + messages.size() + " messages");
		        tts.speak(messages.get(0), TextToSpeech.QUEUE_FLUSH, null);
		        if (messages.size() > 1) {
			        for(int i=1; i < messages.size(); i++) {
				        tts.speak(messages.get(i), TextToSpeech.QUEUE_ADD, null);
			        }
		        }
		        
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
