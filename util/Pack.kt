package net.rxaa.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import net.rxaa.ext.FileExt
import java.io.File


object Pack {
    private var deviceId: String? = null

    /***
     * 重启整个APP
     */
    fun restartAPP() {
        val intent =
            df.appContext!!.packageManager.getLaunchIntentForPackage(df.appContext!!.packageName)
        val restartIntent =
            PendingIntent.getActivity(
                df.appContext!!.applicationContext,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT
            )
        val mgr = df.appContext!!.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 50, restartIntent)
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    @JvmStatic
    fun getDeviceID(): String {
        if (deviceId == null) {
            try {
                deviceId = Settings.Secure.getString(
                    df.appContext!!.contentResolver,
                    Settings.Secure.ANDROID_ID
                )

                if (deviceId.isNullOrEmpty() || deviceId == "9774d56d682e549c") {
                    deviceId = (df.appContext!!.getSystemService(
                        Context.TELEPHONY_SERVICE
                    ) as TelephonyManager).deviceId
                    if (deviceId.isNullOrEmpty())
                        deviceId = Build.SERIAL

                }
            } catch (e: Throwable) {
                deviceId = Build.SERIAL
            }
            if (deviceId.isNullOrEmpty()) {
                //都没有则自己创建一个
                deviceId = dfCfg.cfgFile.dat.deviceId;
                dfCfg.cfgFile.save()
            }
        }
        return deviceId!!;
    }

    @JvmStatic
    fun installApp(cont: Context?, app: String) {
        installApp(cont, File(app))
    }

    @JvmStatic
    fun installApp(cont: Context?, app: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val install = Intent(Intent.ACTION_VIEW)
            install.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            //赋予临时权限
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            //设置dataAndType
            install.setDataAndType(
                FileExt.getFileUri(app),
                "application/vnd.android.package-archive"
            );
            cont?.startActivity(install)
        } else {
            //创建URI
            val uri = Uri.fromFile(app)
            //创建Intent意图
            val intent = Intent(Intent.ACTION_VIEW)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK//启动新的activity
            //设置Uri和类型
            intent.setDataAndType(uri, "application/vnd.android.package-archive")
            //执行安装
            cont?.startActivity(intent)
        }
    }


    @JvmStatic
    fun isAppInstalled(packagename: String): Boolean {
        FileExt.catchLog {
            val packageInfo = df.appContext?.packageManager?.getPackageInfo(packagename, 0)
            if (packageInfo != null)
                return true
        }
        return false
    }

    @JvmStatic
    val versionCode: Int
        get() {
            FileExt.catchLog {
                val info = df.appContext?.packageManager?.getPackageInfo(
                    df.appContext?.packageName!!, 0
                )
                return info?.versionCode ?: 0;
            }
            return 0
        }

    fun versionToLong(ver: String): Long {
        var res = 0L;
        val strs = ver.split(".");

        val len = arrayOf(1000000000, 1000000, 1000, 1, 1, 1, 1);
        for (i in 0 until strs.size) {
            res += strs[i].toLong() * len[i];
            if (i >= 3) {
                break;
            }
        }
        return res;
    }

    @JvmStatic
    val versionName: String
        get() {
            FileExt.catchLog {
                val info = df.appContext?.packageManager?.getPackageInfo(
                    df.appContext?.packageName!!, 0
                )
                return info?.versionName ?: "";
            }
            return ""
        }
}

