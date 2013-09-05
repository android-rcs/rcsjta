package com.orangelabs.rcs.tts;

import java.util.ArrayList;

import org.gsma.joyn.chat.ChatIntent;
import org.gsma.joyn.chat.ChatMessage;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * Chat invitation event receiver
 * 
 * @author jexa7410
 */
public class ChatEvent extends BroadcastReceiver {
    @Override
	public void onReceive(Context context, Intent intent) {
        SharedPreferences preferences = context.getSharedPreferences(Registry.REGISTRY, Activity.MODE_PRIVATE);
        boolean flag = Registry.readBoolean(preferences, Registry.ACTIVATE_TTS, false);
        if (flag) {
        	// Capture the chat message
    		ChatMessage message = intent.getParcelableExtra(ChatIntent.EXTRA_MESSAGE);
    		
			// Play TTS of the chat message
        	ArrayList<String> messages = new ArrayList<String>();
			messages.add(context.getString(R.string.label_new_msg));        		
			messages.add(message.getMessage());        		
			Intent serviceIntent = new Intent(context, PlayTextToSpeech.class);
			serviceIntent.putStringArrayListExtra("messages", messages);
			context.startService(serviceIntent);        	    	
        }
    }
}
