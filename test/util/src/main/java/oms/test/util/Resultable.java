package oms.test.util;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import android.util.Log;

import junit.framework.AssertionFailedError;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public abstract class Resultable {
    private final String TAG = "ics_test_util";
    private CountDownLatch latch;

    public Resultable(int count) {
        latch = new CountDownLatch(count);
    }

    protected void reinitLatch(int count) {
        if (latch.getCount() != 0) {
            Log.w(TAG, "Renew a latch before its count reaches to zero.");
        }
        latch = new CountDownLatch(count);
    }

    protected void onResult() {
        assertTrue("Unexpected event triggered.", latch.getCount() > 0);
        latch.countDown();
    }

    /**
     * @param timeout timeout
     * @return return false upon timeout or interrupted, otherwise return true.
     */
    protected boolean getResult(int timeout) {
        try {
            if (latch.await(timeout, TimeUnit.MILLISECONDS)) {
                return true;
            } else {
                Log.w(TAG, "Timeout on Resultable.getResult.");
            }
        } catch (InterruptedException e) {
            Log.w(TAG, "InterruptedException during latch.await");
        }
        return false;
    }
}
