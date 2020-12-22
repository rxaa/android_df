package net.rxaa.util

class dfCfg {

    companion object {
        val cfgFile = FileObject(dfCfg::class.java)
    }

    var deviceId = df.getRandStr(16);
}