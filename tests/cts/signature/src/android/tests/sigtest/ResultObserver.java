/*
 * Copyright (C) 2008 The Android Open Source Project.
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
 */

package android.tests.sigtest;

/**
 * Interface for saving signature test result.
 */
public interface ResultObserver {
    /**
     * Notify failure.
     * @param type Failure type.
     * @param name Name of the failed element (interface/class/method/field)
     * @param errorMessage a descriptive message indicating why it failed.
     */
    void notifyFailure(SignatureTestActivity.FAILURE_TYPE type,
                       String name,
                       String errorMessage);

}
