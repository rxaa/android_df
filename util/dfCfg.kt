package net.rxaa.util

import java.io.Serializable

class dfCfg : Serializable {

    companion object {
        val cfgFile = FileObject(dfCfg::class.java)
    }

    var deviceId = df.getRandStr(16);
}