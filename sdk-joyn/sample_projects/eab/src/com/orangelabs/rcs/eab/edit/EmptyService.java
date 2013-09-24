/*******************************************************************************
 * Software Name : RCS
 *
 * Copyright © 2010 France Telecom S.A.
 ******************************************************************************/

package com.orangelabs.rcs.eab.edit;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Background {@link Service} that is used to keep our process alive long enough
 * for background threads to finish. Started and stopped directly by specific
 * background tasks when needed.
 */
public class EmptyService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
