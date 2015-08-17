package com.orangelabs.rcs.ri;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * On device boot starts the RCS service notification manager
 * automatically 
 *  
 * @author Jean-Marc AUFFRET
 */
public class DeviceBoot extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, RcsServiceNotifManager.class));
    }
}