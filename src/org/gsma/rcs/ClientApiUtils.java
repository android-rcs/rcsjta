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

package org.gsma.rcs;

/**
 * Class ClientApiUtils.
 */
public class ClientApiUtils {

    /**
     * Constant RCS_SERVICE_NAME.
     */
    public static final String RCS_SERVICE_NAME = "org.gsma.rcs.SERVICE";

    /**
     * Constant STARTUP_SERVICE_NAME.
     */
    public static final String STARTUP_SERVICE_NAME = "org.gsma.rcs.STARTUP";

    /**
     * Constant PROVISIONING_SERVICE_NAME.
     */
    public static final String PROVISIONING_SERVICE_NAME = "org.gsma.rcs.PROVISIONING";

    /**
     * Creates a new instance of ClientApiUtils.
     */
    public ClientApiUtils() {

    }

    /**
     *  
     * @param ctx Application context
     * @return <code>true</code> is the RCS service stack is running (in background), otherwise, returns <code>false</code>
     */
    public static boolean isServiceStarted(android.content.Context ctx) {
        return false;
    }

    /**
     *  
     * @param ctx Application context
     */
    public static void startRcsService(android.content.Context ctx) {

    }

    /**
     *  
     * @param ctx Application context
     */
    public static void stopRcsService(android.content.Context ctx) {

    }

} // end ClientApiUtils
