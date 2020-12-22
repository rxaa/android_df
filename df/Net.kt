package net.rxaa.df

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
}

