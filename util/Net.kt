package net.rxaa.util

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import net.rxaa.ext.getConnectivityManager
import java.io.IOException
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException


object Net {
    @JvmStatic
    fun isConnected(): Boolean {
        val cont = df.appContext ?: return false
        val net = cont.getConnectivityManager().activeNetworkInfo ?: return false
        return net.isConnected
    }

    @JvmStatic
    fun isWifi(): Boolean {
        val cont = df.appContext ?: return false
        val net = cont.getConnectivityManager().activeNetworkInfo ?: return false
        return net.isConnected && net.type == ConnectivityManager.TYPE_WIFI
    }

    @JvmStatic
    inline fun getAllCapabilities(func: (net: NetworkCapabilities) -> Unit) {
        val cont = df.appContext ?: return
        val netMana = cont.getConnectivityManager();
        for (net in netMana.allNetworks) {
            val ca = netMana.getNetworkCapabilities(net) ?: continue;
            func(ca);
        }
    }

    fun isNetworkConnected(): Boolean {
        val context = df.appContext ?: return false;
        var result = false
        val cm = context.getConnectivityManager() ?: return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
            if (capabilities != null) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    result = true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    result = true
                }
            }
        } else {
            val activeNetwork = cm.activeNetworkInfo
            if (activeNetwork != null) {
                // connected to the internet
                if (activeNetwork.type == ConnectivityManager.TYPE_WIFI) {
                    result = true
                } else if (activeNetwork.type == ConnectivityManager.TYPE_MOBILE) {
                    result = true
                }
            }
        }
        return result
    }

    @JvmStatic
    inline fun getAll(func: (net: NetworkInfo) -> Unit) {
        val cont = df.appContext ?: return
        val netMana = cont.getConnectivityManager();

        if (Build.VERSION.SDK_INT < 21) {
            for (net in netMana.allNetworkInfo) {
                func(net)
            }
        } else {
            for (net in netMana.allNetworks) {
                val n = netMana.getNetworkInfo(net);
                if (n != null)
                    func(n);
            }
        }

    }

    /**
     * 获取网络类型
     */
    fun getInternetType(context: Context): Int {
        @SuppressLint("MissingPermission") val info = (context
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo
        return info.type
    }

    /**
     * 手机卡运营商
     */
    fun getSimOperator(context: Context): String {
        var opeType = ""
        val teleManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val operator = teleManager.simOperator
        if ("46001" == operator || "46006" == operator || "46009" == operator) {
            //中国联通
            opeType = "1"
        } else if ("46003" == operator || "46005" == operator || "46011" == operator) {
            //中国电信
            opeType = "1"
        } else if ("46000" == operator || "46002" == operator || "46004" == operator || "46007" == operator) {
            //中国移动
            opeType = "3"
        }
        return opeType
    }

    fun getIpAddress(context: Context): String? {
        @SuppressLint("MissingPermission") val info = (context
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo
        if (info != null && info.isConnected) {
            // 3/4g网络
            if (info.type == ConnectivityManager.TYPE_MOBILE) {
                try {
                    val en = NetworkInterface.getNetworkInterfaces()
                    while (en.hasMoreElements()) {
                        val intf = en.nextElement()
                        val enumIpAddr = intf.inetAddresses
                        while (enumIpAddr.hasMoreElements()) {
                            val inetAddress = enumIpAddr.nextElement()
                            if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                                return inetAddress.getHostAddress()
                            }
                        }
                    }
                } catch (e: SocketException) {
                    e.printStackTrace()
                }
            } else if (info.type == ConnectivityManager.TYPE_WIFI) {
                //  wifi网络
                @SuppressLint("WifiManagerPotentialLeak") val wifiManager =
                    context.getSystemService(
                        Context.WIFI_SERVICE
                    ) as WifiManager
                @SuppressLint("MissingPermission") val wifiInfo = wifiManager.connectionInfo
                return intIP2StringIP(wifiInfo.ipAddress)
            } else if (info.type == ConnectivityManager.TYPE_ETHERNET) {
                // 有限网络
                return getLocalIp()
            }
        }
        return null
    }

    private fun intIP2StringIP(ip: Int): String? {
        return (ip and 0xFF).toString() + "." +
                (ip shr 8 and 0xFF) + "." +
                (ip shr 16 and 0xFF) + "." +
                (ip shr 24 and 0xFF)
    }

    /**
     * 获取有限网IP
     */
    private fun getLocalIp(): String? {
        try {
            val en = NetworkInterface
                .getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf = en.nextElement()
                val enumIpAddr = intf
                    .inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress = enumIpAddr.nextElement()
                    if (!inetAddress.isLoopbackAddress
                        && inetAddress is Inet4Address
                    ) {
                        return inetAddress.getHostAddress()
                    }
                }
            }
        } catch (ignored: SocketException) {
        }
        return "0.0.0.0"
    }

    /**
     * 判断网络是否可用
     *
     * 需添加权限 `<uses-permission android:name="android.permission.INTERNET"/>`
     *
     * 需要异步ping，如果ping不通就说明网络不可用
     *
     * @param ip ip地址（自己服务器ip），如果为空，ip为阿里巴巴公共ip
     * @return `true`: 可用<br></br>`false`: 不可用
     */
    fun isAvailableByPing(ip: String?): Boolean {
        var ip = ip
        if (ip == null || ip.length <= 0) {
            ip = "114.114.114.114"
        }
        val runtime = Runtime.getRuntime()
        var ipProcess: Process? = null
        try {
            //-c 后边跟随的是重复的次数，-w后边跟随的是超时的时间，单位是秒，不是毫秒，要不然也不会anr了
            ipProcess = runtime.exec("ping -c 3 -w 3 $ip")
            val exitValue = ipProcess.waitFor()
            return exitValue == 0
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } finally {
            //在结束的时候应该对资源进行回收
            ipProcess?.destroy()
            runtime.gc()
        }
        return false
    }

}

