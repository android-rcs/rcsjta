/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.utils.logger;

import com.gsma.rcs.platform.logger.AndroidAppender;
import com.gsma.rcs.service.api.ExceptionUtil;

/**
 * Logger
 * 
 * @author jexa7410
 */
public class Logger {
    /**
     * Trace ON
     */
    public static final boolean TRACE_ON = true;

    /**
     * Trace OFF
     */
    public static final boolean TRACE_OFF = false;

    /**
     * DEBUG level
     */
    public static final int DEBUG_LEVEL = 0;

    /**
     * INFO level
     */
    public static final int INFO_LEVEL = 1;

    /**
     * WARN level
     */
    public static final int WARN_LEVEL = 2;

    /**
     * ERROR level
     */
    public static final int ERROR_LEVEL = 3;

    /**
     * FATAL level
     */
    public static final int FATAL_LEVEL = 4;

    /**
     * Trace flag
     */
    public static boolean sActivationFlag = TRACE_ON;

    /**
     * Trace level
     */
    public static int traceLevel = DEBUG_LEVEL;

    /**
     * List of appenders
     */
    private static Appender[] sAppenders = new Appender[] {
        new AndroidAppender()
    };

    /**
     * Classname
     */
    private String mClassname;

    /**
     * Constructor
     * 
     * @param classname Classname
     */
    private Logger(String classname) {
        int index = classname.lastIndexOf('.');
        if (index != -1) {
            mClassname = classname.substring(index + 1);
        } else {
            mClassname = classname;
        }
    }

    /**
     * Is logger activated
     * 
     * @return boolean
     */
    public boolean isActivated() {
        return (sActivationFlag == TRACE_ON);
    }

    /**
     * Debug trace
     * 
     * @param trace Trace
     */
    public void debug(String trace) {
        printTrace(trace, DEBUG_LEVEL);
    }

    /**
     * Info trace
     * 
     * @param trace Trace
     */
    public void info(String trace) {
        printTrace(trace, INFO_LEVEL);
    }

    /**
     * Warning trace
     * 
     * @param trace Trace
     */
    public void warn(String trace) {
        printTrace(trace, WARN_LEVEL);
    }

    /**
     * Warning trace
     * 
     * @param trace Trace
     * @param e Exception
     */
    public void warn(String trace, Throwable e) {
        printTrace(trace, WARN_LEVEL);
        printTrace(ExceptionUtil.getFullStackTrace(e), WARN_LEVEL);
    }

    /**
     * Error trace
     * 
     * @param trace Trace
     */
    public void error(String trace) {
        printTrace(trace, ERROR_LEVEL);
    }

    /**
     * Error trace
     * 
     * @param trace Trace
     * @param e Exception
     */
    public void error(String trace, Throwable e) {
        printTrace(trace, ERROR_LEVEL);
        printTrace(ExceptionUtil.getFullStackTrace(e), ERROR_LEVEL);
    }

    /**
     * Fatal trace
     * 
     * @param trace Trace
     */
    public void fatal(String trace) {
        printTrace(trace, FATAL_LEVEL);
    }

    /**
     * Fatal trace
     * 
     * @param trace Trace
     * @param e Exception
     */
    public void fatal(String trace, Throwable e) {
        printTrace(trace, FATAL_LEVEL);
        printTrace(ExceptionUtil.getFullStackTrace(e), FATAL_LEVEL);

    }

    /**
     * Print a trace
     * 
     * @param trace Trace
     * @param level Trace level
     */
    private void printTrace(String trace, int level) {
        if (sAppenders != null && level >= traceLevel) {
            /*
             * String having '\' characters are not printed out in locat console !
             */
            trace = trace.replace("\r", "");
            for (Appender appender : sAppenders) {
                appender.printTrace(mClassname, level, trace);
            }
        }
    }

    /**
     * Create a static instance
     * 
     * @param classname Classname
     * @return Instance
     */
    public static synchronized Logger getLogger(String classname) {
        return new Logger(classname);
    }

}
