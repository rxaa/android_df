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
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat


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

/**
 * 亮色背景，黑色图标
 */
fun Activity.statusBarLight(color: Int = Color.WHITE) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
    }
    window.statusBarColor = color
    window.navigationBarColor = color
}

/**
 * 沉浸式状态栏
 */
fun Activity.statusNoBarLight(black: Boolean = true) {
    if (black) {
        statusBarLight()
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        val decorView = window.decorView
        decorView.setOnApplyWindowInsetsListener { v, insets ->
            val defaultInsets = v.onApplyWindowInsets(insets)
            defaultInsets.replaceSystemWindowInsets(
                defaultInsets.systemWindowInsetLeft,
                0,
                defaultInsets.systemWindowInsetRight,
                defaultInsets.systemWindowInsetBottom
            )
        }
        ViewCompat.requestApplyInsets(decorView)
        //将状态栏设成透明，如不想透明可设置其他颜色
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
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
