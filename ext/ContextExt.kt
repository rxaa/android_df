package net.rxaa.ext

import android.app.Activity
import android.app.Service
import android.content.Context
import android.graphics.Color
import android.hardware.SensorManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.PowerManager
import android.telephony.TelephonyManager
import android.view.View
import android.view.Window
import android.view.WindowManager


fun Context.getConnectivityManager(): ConnectivityManager {
    return this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
}

fun Context.getWindowManager(): WindowManager {
    return this.getSystemService(Context.WINDOW_SERVICE) as WindowManager
}

fun Context.getAudioManager(): AudioManager {
    return this.getSystemService(Context.AUDIO_SERVICE) as AudioManager
}

fun Context.getSensorManager(): SensorManager? {
    return this.getSystemService(Context.SENSOR_SERVICE) as SensorManager?
}

fun Context.getPowerManager(): PowerManager {
    return this.getSystemService(Context.POWER_SERVICE) as PowerManager
}

fun Context.getTelephonyManager(): TelephonyManager {
    return this.getSystemService(Service.TELEPHONY_SERVICE) as TelephonyManager
}

/**
 * 用于获取状态栏的高度。 使用Resource对象获取（推荐这种方式）
 *
 * @return 返回状态栏高度的像素值。
 */
fun Context.getStatusBarHeight(): Int {
    var result = 0;
    val resourceId = this.getResources().getIdentifier(
        "status_bar_height", "dimen",
        "android"
    );
    if (resourceId > 0) {
        result = this.getResources().getDimensionPixelSize(resourceId);
    }
    return result;
}


fun Activity.statusBarColor(color: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        this.window.statusBarColor = color;
        this.window.navigationBarColor = color;
    }
}

/**
 * 亮色背景，黑色图标
 */
fun Activity.statusBarLight() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
    }
}

/**
 * 状态栏全透明
 */
fun Window.statusBarTransparent() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        this.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        this.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        this.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        this.statusBarColor = Color.TRANSPARENT;
        this.navigationBarColor = Color.TRANSPARENT;
    }
}

/**
 * 状态栏半透明
 */
fun Window.statusBarTransparentHalf() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        //window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        this.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
    }
}