package com.orangelabs.rcs.tts;

import java.util.ArrayList;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.gsma.services.rcs.chat.ChatIntent;
import com.gsma.services.rcs.chat.ChatMessage;

/**
 * Chat invitation event receiver
 * 
 * @author jexa7410
 */
public class ChatEvent extends BroadcastReceiver {
    @Override
	public void onReceive(Context context, Intent intent) {
    	// Check activation state before to continue
        SharedPreferences preferences = context.getSharedPreferences(Registry.REGISTRY, Activity.MODE_PRIVATE);
        boolean flag = Registry.readBoolean(preferences, Registry.ACTIVATE_TTS, false);
        if (flag) {
        	// Get the chat message from the Intent
    		ChatMessage message = intent.getParcelableExtra(ChatIntent.EXTRA_MESSAGE);
    		
			// Play TTS on the chat message
        	ArrayList<String> messages = new ArrayList<String>();
			messages.add(context.getString(R.string.label_new_msg));        		
			messages.add(message.getMessage());        		
			Intent serviceIntent = new Intent(context, PlayTextToSpeech.class);
			serviceIntent.putStringArrayListExtra("messages", messages);
			context.startService(serviceIntent);        	    	
        }
    }
}
