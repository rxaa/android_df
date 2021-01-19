package net.rxaa.ext

import net.rxaa.util.df

/**
 * 将Int dp值转换为适应屏幕的px值
 */
fun Int.dp2px(): Int {
    return df.dp2px(this.toFloat())
}


fun Long.toTime(): String {
    val hour = (this / 1000) / 3600;
    val minute = (this / 1000) / 60;
    val sec = (this / 1000) % 60;
    return hour.to2String() + ":" + minute.to2String() + ":" + sec.to2String();
}

/**
 * 转换为带单位的字节大小
 */
fun Long.toByteString(): String {
    if (this <= 1024) {
        return "" + this + " Byte"
    }

    if (this <= 1024 * 1024) {
        return "" + this / 1024 + " KB"
    }

    if (this <= 1024 * 1024 * 1024) {
        return "%.1f MB".format(this.toDouble() / 1024.0 / 1024.0)
    }

    return "%.1f GB".format(this.toDouble() / 1024.0 / 1024.0 / 1024.0)
}

/**
 * 转换为带单位的字节大小
 */
fun Int.toByteString(): String {
    return this.toLong().toByteString()
}

/**
 * 转换为至少两位字串
 */
fun Int.to2String(): String {
    if (this < 10)
        return "0" + this
    return this.toString();
}

fun Long.to2String(): String {
    if (this < 10)
        return "0" + this
    return this.toString();
}