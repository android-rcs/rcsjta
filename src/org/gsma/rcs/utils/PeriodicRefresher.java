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

package org.gsma.rcs.utils;

/**
 * Class PeriodicRefresher.
 */
public abstract class PeriodicRefresher {
    /**
     * Creates a new instance of PeriodicRefresher.
     */
    public PeriodicRefresher() {

    }

    public abstract void periodicProcessing();

    /**
     *  
     * @param arg1 The arg1.
     */
    public void startTimer(int arg1) {

    }

    /**
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     */
    public synchronized void startTimer(int arg1, double arg2) {

    }

    public synchronized void stopTimer() {

    }

} // end PeriodicRefresher
