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
package com.orangelabs.rcs.popup;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.xml.sax.InputSource;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.orangelabs.rcs.popup.utils.PlayTTS;
import com.orangelabs.rcs.popup.utils.ShakeMotionDetector;
import com.orangelabs.rcs.popup.utils.ShakeMotionListener;

/**
 * Receive popup
 * 
 * @author Jean-Marc AUFFRET
 */
public class ReceivePopup extends Activity implements View.OnClickListener, ShakeMotionListener {
	private static final String TAG = "Popup";

	/**
     * Share motion detector
     */
    private ShakeMotionDetector shakeDetector = null;  

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    
        // Set title
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.receive_popup);

        // Set screen
        setFullscreen(true);
        
        // Create shake detector 
        shakeDetector = new ShakeMotionDetector(this);
        shakeDetector.addListener(this);
        shakeDetector.start();

        try {
			// Parse received popup
			String content = getIntent().getStringExtra("content");
			InputSource input = new InputSource(new ByteArrayInputStream(content.getBytes()));
			PopupParser parser = new PopupParser(input);
			Log.d(TAG, "Popup:\n" + content);

	        // Display message
			String remote = getIntent().getStringExtra("contact");
			String txt = parser.getMessage();
			String msg = getString(R.string.label_remote, remote) + "\n" + txt;
	        TextView msgView = (TextView)findViewById(R.id.message);
	        msgView.setText(msg);
			
			// Play TTS
	        if (parser.isTTS() && !TextUtils.isEmpty(txt)) {
				Log.d(TAG, "Play TTS");
				Intent ttsIntent = new Intent(getApplicationContext(), PlayTTS.class);
				ttsIntent.putExtra("message", txt);
				startService(ttsIntent);        	    	
	        }
	        
			// Make a vibration
	        Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
	        long[] pattern = {500L, 200L, 500L};
	        vibrator.vibrate(pattern, -1);

	        // Display animation
	        ImageView imageView = (ImageView)findViewById(R.id.picture);
	        imageView.setClickable(true);
	        imageView.setFocusable(true);
	        imageView.setOnClickListener(this);
			String animation = parser.getAnimation();
			InputStream stream = getResources().getAssets().open(animation.toLowerCase() + ".png");
			Drawable d = Drawable.createFromStream(stream, null);
        	imageView.setImageDrawable(d);

        	// Play animation
	        Animation hyperspaceJumpAnimation = AnimationUtils.loadAnimation(this, R.anim.zoom);
	        imageView.startAnimation(hyperspaceJumpAnimation);	        

        } catch(Exception e) {
			Log.e(TAG, "Unexpected exception", e);
		}
    }    

    @Override
    protected void onDestroy() {
    	// Remove shake detector
        if (shakeDetector != null) {
	    	shakeDetector.stop();
	    	shakeDetector.removeListener();
        }
        
        super.onDestroy();
    }
    
	/**
	 * Share event detected
	 */
	public void onShakeDetected() {
		// Exit
        finish();
	}
    
    /**
     * Set full screen mode
     * 
     * @param on Mode
     */
    private void setFullscreen(boolean on) {
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        final int bits = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        if (on) {
            winParams.flags |=  bits;
        } else {
            winParams.flags &= ~bits;
        }
        win.setAttributes(winParams);
    }

    public void onClick(View v) {
    	finish();
    }
}