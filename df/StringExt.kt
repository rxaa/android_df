package rxaa.df

import java.util.*

inline fun <T> MutableList<T>?.pop(func: (last: T) -> Unit): Boolean {
    if (this != null && this.size > 0) {
        func(this.get(this.size - 1));
        this.removeAt(this.size - 1)
        return true
    }
    return false;
}

fun String.removeLast(): String {
    return this.substring(0, this.length - 1)
}

fun <K, V, RET> Map<K, V>?.toArray(func: (k: K, v: V) -> RET): ArrayList<RET> {
    val arr = ArrayList<RET>();
    if (this != null) {
        for ((k, v) in this) {
            arr.add(func(k, v));
        }
    }
    return arr;
}

fun <T, T2> List<T>.toHashMap(func: (k: T) -> T2): HashMap<T2, T> {
    val map = HashMap<T2, T>()
    for (v in this) {
        map[func(v)] = v;
    }
    return map
}
