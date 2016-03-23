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

package android.tests.sigtest;

/**
 * Created by YPLO6403 on 17/03/2016.
 */
public class RcsApiSignatureTestResult {

    private final SignatureTestActivity.FAILURE_TYPE mType;
    private final String mClassName;
    private final String mReason;

    public RcsApiSignatureTestResult(SignatureTestActivity.FAILURE_TYPE type, String className,
            String reason) {
        mType = type;
        mClassName = className;
        mReason = reason;
    }

    public SignatureTestActivity.FAILURE_TYPE getType() {
        return mType;
    }

    public String getClassName() {
        return mClassName;
    }

    public String getReason() {
        return mReason;
    }
}
