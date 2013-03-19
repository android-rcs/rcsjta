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

package org.gsma.joyn.session;

/**
 * Interface for RCS session state constants
 *
 * @author Jean-Marc AUFFRET (Orange)
 * @version 1.0
 * @since 1.0
 */
public interface SessionState {

    /**
     * Session state unknown
     */
    public static final int UNKNOWN = -1;

    /**
     * Session cancelled
     */
    public static final int CANCELLED = 0;

    /**
     * Session established
     */
    public static final int ESTABLISHED = 1;

    /**
     * Session terminated
     */
    public static final int TERMINATED = 2;

    /**
     * Session pending
     */
    public static final int PENDING = 3;

}
