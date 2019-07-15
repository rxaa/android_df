package rxaa.df

import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build


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
    inline fun getAll(func: (net: NetworkInfo) -> Unit) {
        val cont = df.appContext ?: return
        val netMana = cont.getConnectivityManager();

        if (Build.VERSION.SDK_INT < 21) {
            for (net in netMana.allNetworkInfo) {
                func(net)
            }
        } else {
            for (net in netMana.allNetworks) {
                func(netMana.getNetworkInfo(net))
            }
        }

    }
}
