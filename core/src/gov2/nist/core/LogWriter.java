/*
 * Conditions Of Use
 *
 * This software was developed by employees of the National Institute of
 * Standards and Technology (NIST), an agency of the Federal Government.
 * Pursuant to title 15 Untied States Code Section 105, works of NIST
 * employees are not subject to copyright protection in the United States
 * and are considered to be in the public domain.  As a result, a formal
 * license is not needed to use the software.
 *
 * This software is provided by NIST as a service and is expressly
 * provided "AS IS."  NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED
 * OR STATUTORY, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT
 * AND DATA ACCURACY.  NIST does not warrant or make any representations
 * regarding the use of the software or the results thereof, including but
 * not limited to the correctness, accuracy, reliability or usefulness of
 * the software.
 *
 * Permission to use this software is contingent upon your acceptance
 * of the terms of this agreement.
 *
 */
/***************************************************************************
 * Product of NIST/ITL Advanced Networking Technologies Division (ANTD).    *
 ***************************************************************************/
package gov2.nist.core;

import java.io.*;
import java.util.Properties;

// BEGIN ANDROID-added
// TODO: this class should be replaced by android logging mechanism.
public class LogWriter implements StackLogger {
    private static final String TAG = "SIP_STACK";

    private boolean mEnabled = true;

    public void logStackTrace() {
        // TODO
    }

    public void logStackTrace(int traceLevel) {
        // TODO
    }

    public int getLineCount() {
        return 0;
    }

    public void logException(Throwable ex) {
        //Log.e(TAG, "", ex);
    }
    public void logDebug(String message) {
        //Log.d(TAG, message);
    }
    public void logTrace(String message) {
        //Log.d(TAG, message);
    }
    public void logFatalError(String message) {
        //Log.e(TAG, message);
    }
    public void logError(String message) {
        //Log.e(TAG, message);
    }
    public boolean isLoggingEnabled() {
        return mEnabled;
    }
    public boolean isLoggingEnabled(int logLevel) {
        // TODO
        return mEnabled;
    }
    public void logError(String message, Exception ex) {
        //Log.e(TAG, message, ex);
    }
    public void logWarning(String string) {
        //Log.w(TAG, string);
    }
    public void logInfo(String string) {
        //Log.i(TAG, string);
    }

    public void disableLogging() {
        mEnabled = false;
    }

    public void enableLogging() {
        mEnabled = true;
    }

    public void setBuildTimeStamp(String buildTimeStamp) {
    }

    public void setStackProperties(Properties stackProperties) {
    }

    public String getLoggerName() {
        return "Android SIP Logger";
    }
}
