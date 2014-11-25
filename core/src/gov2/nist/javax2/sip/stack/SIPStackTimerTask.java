/*
 * @author:     Brett Buckingham
 * @author:     Last modified by: $Author: emcho $
 * @version:    $Date: 2009/07/17 18:58:14 $ $Revision: 1.3 $
 *
 * This source code has been contributed to the public domain.
 */

package gov2.nist.javax2.sip.stack;

import android.os.SystemClock;

import java.util.TimerTask;

/**
 * A subclass of TimerTask which runs TimerTask code within a try/catch block to
 * avoid killing the SIPTransactionStack timer thread. Note: subclasses MUST not
 * override run(); instead they should override runTask().
 *
 * @author Brett Buckingham
 *
 */
public abstract class SIPStackTimerTask extends TimerTask {
    long taskOutdatedTime;

    // / Implements code to be run when the SIPStackTimerTask is executed.
    protected abstract void runTask();

    // / The run() method is final to ensure that all subclasses inherit the
    // exception handling.
    public final void run() {
        try {
            runTask();
        } catch (Throwable e) {
            System.out.println("SIP stack timer task failed due to exception:");
            e.printStackTrace();
        }
    }

    /**
     * Set the number of ticks after which the timer is considered to be expired (i.e. outdated).
     *
     * A tick is a period of {@link gov2.nist.javax2.sip.stack.SIPTransactionStack#BASE_TIMER_INTERVAL} milliseconds.
     *
     * @param ticks number of ticks after which the timer is expired
     *
     * @see #isTaskOutdated()
     */
    protected void setTaskOutdatedTime(int ticks) {
        taskOutdatedTime = SystemClock.elapsedRealtime() + ticks * SIPTransactionStack.BASE_TIMER_INTERVAL;
    }

    /**
     * Check whether the task is still valid or already outdated.
     *
     * Ticks may not be processed as regularly and timely as scheduled if device is sleeping.
     * This may result in the fact that there are still ticks left although the initial expiry time
     * (start time + number of ticks * waiting period * per tick) is already exceeded.
     *
     * @return <code>true</code> if the task is outdated, i.e. its validity has expired
     */
    protected boolean isTaskOutdated() {
        return SystemClock.elapsedRealtime() > taskOutdatedTime;
    }
}
