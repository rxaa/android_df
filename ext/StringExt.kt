package net.rxaa.ext


fun String.removeLast(): String {
    return this.substring(0, this.length - 1)
}




/**
 * 从字串中抽取数字
 */
fun String?.getNumber(): String {
    if (this == null || this.length == 0)
        return ""
    var ret = "";
    this.forEach {
        if (it in '0'..'9')
            ret += it
    }

    return ret
}

fun String?.getInt(default: Int = 0): Int {
    if (this == null || this.length == 0)
        return default
    try {
        return this.toDouble().toInt()
    } catch (e: Exception) {
    }

    return default
}

fun String?.getLong(default: Long = 0): Long {
    if (this == null || this.length == 0)
        return default
    try {
        return this.toDouble().toLong()
    } catch (e: Exception) {
    }
    return default
}

fun String?.getDouble(default: Double = 0.0): Double {
    if (this == null || this.length == 0)
        return default
    try {
        return this.toDouble()
    } catch (e: Exception) {
    }
    return default
}
