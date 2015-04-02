/*
 * Copyright (C) 2015 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.gsma.rcs;

public class ExceptionUtil {

    /**
     * Special Delimiter that will be appended while propagating server exceptions over AIDL layer.
     */
    private static final char DELIMITER_PIPE = '|';

    private static void addTrace(StringBuilder sb, Throwable t, boolean first) {
        StackTraceElement[] traces = t.getStackTrace();
        if (null != traces && traces.length > 0) {
            if (!first) {
                sb.append("Caused by: ");
            }
            sb.append(t.getClass().getName());
            String msg = t.getMessage();
            if (msg != null) {
                sb.append(": ");
                if (first) {
                    final int delimiterIndex = msg.indexOf(DELIMITER_PIPE);
                    if (delimiterIndex > 0 && delimiterIndex < msg.length() - 1) {
                        sb.append(msg.substring(delimiterIndex + 1));
                    } else {
                        sb.append(msg);
                    }
                } else {
                    sb.append(msg);
                }
            }
            sb.append('\n');
            for (StackTraceElement trace : traces) {
                sb.append("\tat ").append(trace.getClassName()).append('.')
                        .append(trace.getMethodName()).append('(');
                int lineNumber = trace.getLineNumber();
                if (lineNumber > 0) {
                    sb.append(trace.getFileName()).append(':').append(lineNumber);
                } else {
                    sb.append("Native Method");
                }
                sb.append(')').append('\n');
            }
        }
    }

    public static String getFullStackTrace(Throwable t) {
        StringBuilder sb = new StringBuilder();
        addTrace(sb, t, true);
        Throwable cause = t.getCause();
        while (null != cause) {
            addTrace(sb, cause, false);
            cause = cause.getCause();
        }
        return sb.toString();
    }
}
