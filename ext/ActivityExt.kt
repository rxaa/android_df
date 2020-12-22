package net.rxaa.ext

import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import net.rxaa.view.ActivityEx
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


suspend fun Activity.awaitRequestPermission(permissionStr: String) = suspendCoroutine<Boolean> { cont ->
    val code = ActivityEx.getReqCode();
    val permission = ActivityCompat.checkSelfPermission(this, permissionStr)
    if (permission != PackageManager.PERMISSION_GRANTED) {
        // We don't have permission so prompt the user
        ActivityCompat.requestPermissions(
            this,
            arrayOf(permissionStr), //需要请求的所有权限，这是个数组String[]
            code//请求码
        )

        ActivityEx.permissionCameraContinuation.put(code, cont);
    } else {
        cont.resume(true);
    }
}
