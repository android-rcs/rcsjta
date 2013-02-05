/*
 * Copyright 2013, France Telecom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gsma.rcs.utils.logger;

/**
 * Class Logger.
 */
public class Logger {
    /**
     * Constant TRACE_ON.
     */
    public static final boolean TRACE_ON = true;

    /**
     * Constant TRACE_OFF.
     */
    public static final boolean TRACE_OFF = false;

    /**
     * Constant DEBUG_LEVEL.
     */
    public static final int DEBUG_LEVEL = 0;

    /**
     * Constant INFO_LEVEL.
     */
    public static final int INFO_LEVEL = 1;

    /**
     * Constant WARN_LEVEL.
     */
    public static final int WARN_LEVEL = 2;

    /**
     * Constant ERROR_LEVEL.
     */
    public static final int ERROR_LEVEL = 3;

    /**
     * Constant FATAL_LEVEL.
     */
    public static final int FATAL_LEVEL = 4;

    /**
     * The activation flag.
     */
    public static boolean activationFlag;

    /**
     * The trace level.
     */
    public static int traceLevel;

    /**
     * Creates a new instance of Logger.
     *  
     * @param arg1 The arg1.
     */
    private Logger(String arg1) {

    }

    /**
     *  
     * @param arg1 The arg1.
     */
    public void debug(String arg1) {

    }

    /**
     *  
     * @param arg1 The arg1.
     */
    public void error(String arg1) {

    }

    /**
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     */
    public void error(String arg1, Throwable arg2) {

    }

    /**
     *  
     * @param arg1 The arg1.
     */
    public void info(String arg1) {

    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isActivated() {
        return false;
    }

    /**
     *  
     * @param arg1 The arg1.
     */
    public void warn(String arg1) {

    }

    /**
     *  
     * @param arg1 The arg1.
     */
    public void fatal(String arg1) {

    }

    /**
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     */
    public void fatal(String arg1, Throwable arg2) {

    }

    /**
     * Returns the logger.
     *  
     * @param arg1 The arg1.
     * @return  The logger.
     */
    public static synchronized Logger getLogger(String arg1) {
        return (Logger) null;
    }

    /**
     * Sets the appenders.
     *  
     * @param arg1 The appenders array.
     */
    public static void setAppenders(Appender[] arg1) {

    }

    /**
     * Returns the appenders.
     *  
     * @return  The appenders array.
     */
    public static synchronized Appender[] getAppenders() {
        return (Appender []) null;
    }

} // end Logger
