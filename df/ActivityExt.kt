package rxaa.df

import android.app.Activity
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.view.View

fun Activity.requestPermission(permissionStr: String, resp: (success: Boolean) -> Unit) {
    val code = ActivityEx.getReqCode();
    val permission = ActivityCompat.checkSelfPermission(this, permissionStr)
    if (permission != PackageManager.PERMISSION_GRANTED) {
        // We don't have permission so prompt the user
        ActivityCompat.requestPermissions(
            this,
            arrayOf(permissionStr), //需要请求的所有权限，这是个数组String[]
            code//请求码
        )
        ActivityEx.permissionCameraFunc[code] = resp;
    } else {
        resp(true);
    }
}