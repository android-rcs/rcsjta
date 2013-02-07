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
 * Class ITermsApi.
 */
public interface ITermsApi extends android.os.IInterface {
    /**
     *
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     * @return  The boolean.
     */
    public boolean acceptTerms(String arg1, String arg2) throws android.os.RemoteException;

    /**
     *
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     * @return  The boolean.
     */
    public boolean rejectTerms(String arg1, String arg2) throws android.os.RemoteException;

    /**
     * Class Stub.
     */
    public abstract static class Stub extends android.os.Binder implements ITermsApi {
        /**
         * Creates a new instance of Stub.
         */
        public Stub() {
            super();
        }

        /**
         *
         * @return  The i binder.
         */
        public android.os.IBinder asBinder() {
            return (android.os.IBinder) null;
        }

        /**
         *
         * @param arg1 The arg1.
         * @param arg2 The arg2.
         * @param arg3 The arg3.
         * @param arg4 The arg4.
         * @return  The boolean.
         */
        public boolean onTransact(int arg1, android.os.Parcel arg2, android.os.Parcel arg3, int arg4) throws android.os.RemoteException {
            return false;
        }

        /**
         *
         * @param arg1 The arg1.
         * @return  The i terms api.
         */
        public static ITermsApi asInterface(android.os.IBinder arg1) {
            return (ITermsApi) null;
        }

    } // end Stub

} // end ITermsApi
