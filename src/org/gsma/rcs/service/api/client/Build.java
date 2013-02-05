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

package org.gsma.rcs.service.api.client;

/**
 * Class Build.
 */
public class Build {

    public static enum GsmaRelease {
        RCS_2 ((java.lang.String) null, 0),
        RCSE_HOTFIXES_1_2 ((java.lang.String) null, 0),
        RCSE_BLACKBIRD_BASE ((java.lang.String) null, 0)
    }

    /**
     * Constant API_RELEASE.
     */
    public static final String API_RELEASE = "2.8";

    /**
     * Constant API_CODENAME.
     */
    public static final String API_CODENAME = "OrangeLabs";

    /**
     * Constant GSMA_SUPPORTED_RELEASE.
     */
    public static final GsmaRelease GSMA_SUPPORTED_RELEASE = null;

    /**
     * Creates a new instance of Build.
     */
    public Build() {

    }

} // end Build
