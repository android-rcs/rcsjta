/*
 * Copyright 2013, France Telecom
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 *    http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gsma.rcs.terms;

/**
 * Class TermsApi.
 */
public class TermsApi extends org.gsma.rcs.ClientApi {
    /**
     * Creates a new instance of TermsApi.
     *
     * @param arg1 The arg1.
     */
    public TermsApi(android.content.Context arg1) {
        super((android.content.Context) null);
    }

    public void connectApi() {

    }

    public void disconnectApi() {

    }

    /**
     *
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     * @return  The boolean.
     */
    public boolean acceptTerms(String arg1, String arg2) throws org.gsma.rcs.ClientApiException {
        return false;
    }

    /**
     *
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     * @return  The boolean.
     */
    public boolean rejectTerms(String arg1, String arg2) throws org.gsma.rcs.ClientApiException {
        return false;
    }

} // end TermsApi
