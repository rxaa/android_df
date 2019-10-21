package rxaa.df

import android.content.Context
import android.os.PowerManager

object WakeLock {

    /**
     * 后台唤醒，不点亮屏幕(默认唤醒30秒)
     */
    fun partialWake(timeOut: Long = 1000 * 30, func: suspend () -> Unit) {
        val pm = df.appContext!!.getSystemService(Context.POWER_SERVICE) as PowerManager;
        val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "qplink:wake_lock");
        wl.acquire(timeOut);
        df.launch {
            try {
                func()
            } finally {
                wl.release();
            }
        }


    }
}