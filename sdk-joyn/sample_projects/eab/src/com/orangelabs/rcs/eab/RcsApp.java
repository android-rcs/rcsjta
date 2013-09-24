/*******************************************************************************
 * Software Name : RCS
 *
 * Copyright © 2010 France Telecom S.A.
 ******************************************************************************/

package com.orangelabs.rcs.eab;

import android.app.Application;
import com.orangelabs.rcs.platform.logger.AndroidAppender;
import com.orangelabs.rcs.utils.logger.Appender;
import com.orangelabs.rcs.utils.logger.Logger;

public class RcsApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
		// Set logger appenders
		Appender[] appenders = new Appender[] { 
				new AndroidAppender()
			};
		Logger.setAppenders(appenders);
    }

    @Override
    public void onTerminate() {
    }
}
