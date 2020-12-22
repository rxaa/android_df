package net.rxaa.util

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent


object WakeLock {

    val rtcWakeUpName = "rtcWakeUp"

    /**
     * 设置一次定时唤醒
     */
    fun rtcWakeUp(time: Long, service: Class<*>, valu: String = "") {
        val am = df.appContext!!.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(df.appContext, service)
        intent.putExtra(rtcWakeUpName, valu);
        val pi =
            PendingIntent.getService(df.appContext, 0, intent, 0)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + time,
                    pi
                )
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + time, pi);
            }
        } else {
            am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + time, pi);
        }
    }


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