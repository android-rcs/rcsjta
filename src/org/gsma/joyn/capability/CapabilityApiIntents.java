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

package org.gsma.joyn.capability;

import java.lang.String;

/**
 * Interface CapabilityApiIntents.
 *
 * @author Jean-Marc AUFFRET (Orange)
 * @version 1.0
 * @since 1.0
 */
public interface CapabilityApiIntents {
    /**
     * Constant CONTACT_CAPABILITIES.
     */
    public static final String CONTACT_CAPABILITIES = "org.gsma.joyn.capability.CONTACT_CAPABILITIES";

    /**
     * Constant RCS_EXTENSIONS.
     */
    public static final String RCS_EXTENSIONS = "org.gsma.joyn.capability.EXTENSION";

    /**
     * Constant RCSE_EXTENSION_BASE.
     */
    public static final String RCSE_EXTENSION_BASE = "+g.3gpp.iari-ref";

    /**
     * Constant RCSE_EXTENSION_PREFIX.
     */
    public static final String RCSE_EXTENSION_PREFIX = "urn%3Aurn-7%3A3gpp-application.ims.iari.rcse";

} // end CapabilityApiIntents
