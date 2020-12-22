package net.rxaa.ext

import net.rxaa.util.df
import java.util.*


var Calendar.year: Int
    get() {
        return this.get(Calendar.YEAR)
    }
    set(value) {
        this.set(Calendar.YEAR, value)
    }

var Calendar.dayOfWeek: Int
    get() {
        return this.get(Calendar.DAY_OF_WEEK)
    }
    set(value) {
        this.set(Calendar.DAY_OF_WEEK, value)
    }

var Calendar.month: Int
    get() {
        return this.get(Calendar.MONTH)
    }
    set(value) {
        this.set(Calendar.MONTH, value)
    }

var Calendar.dayOfMonth: Int
    get() {
        return this.get(Calendar.DAY_OF_MONTH)
    }
    set(value) {
        this.set(Calendar.DAY_OF_MONTH, value)
    }

fun Calendar?.getDayOfYear(): Int {
    return this!!.get(Calendar.DAY_OF_YEAR)
}

var Calendar.hour: Int
    get() {
        return this.get(Calendar.HOUR_OF_DAY)
    }
    set(value) {
        this.set(Calendar.HOUR_OF_DAY, value)
    }

var Calendar.minute: Int
    get() {
        return this.get(Calendar.MINUTE)
    }
    set(value) {
        this.set(Calendar.MINUTE, value)
    }

var Calendar.second: Int
    get() {
        return this.get(Calendar.SECOND)
    }
    set(value) {
        this.set(Calendar.SECOND, value)
    }

fun Calendar?.todayString(showSecond: Boolean = false): String {
    if (this == null)
        return "";
    val now = df.calendar();

    var ret = "";
    if (year < now.year) {
        ret += "" + year % 100 + "年";
    }
    if (year * 10000 + month * 100 + dayOfMonth < now.year * 10000 + now.month * 100 + now.dayOfMonth) {
        ret += (month + 1).to2String() + "月" + dayOfMonth.to2String() + "日" + " ";
    }

    ret += hour.to2String() + ":" + minute.to2String();
    if (showSecond) {
        ret += ":" + second.to2String();
    }
    return ret;
}

fun Calendar?.getHourMinStr(): String {
    if (this == null)
        return "";

    return "" + hour.to2String() + ":" + minute.to2String();
}

fun Calendar?.getChinese(showHour: Boolean = false): String {
    if (this == null)
        return "";

    var ret = "" + year + "年"

    ret += "" + (month + 1).to2String() + "月"

    ret += dayOfMonth.to2String() + "日"
    if (showHour) {
        ret += " "

        ret += hour.to2String() + ":"

        ret += minute.to2String()
    }
    return ret;
}

fun Calendar?.getString(yearOp: String = "-", showSecond: Boolean = true, showHour: Boolean = true): String {
    if (this == null)
        return "";

    var ret = "" + year + yearOp

    ret += "" + (month + 1).to2String() + yearOp

    ret += dayOfMonth.to2String()

    if (!showHour)
        return ret;

    ret += " "

    ret += hour.to2String() + ":"

    ret += minute.to2String()

    if (showSecond) {
        ret += ":"
        ret += second.to2String()
    }
    return ret
}