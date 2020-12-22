package net.rxaa.ext

import java.util.*

inline fun <T> List<T>?.forEachDesc(action: (T) -> Unit) {
    if (this != null) {
        for (i in this.size - 1 downTo 0) {
            action(this[i])
        }
    }
}

fun List<String>.toArr(): Array<String> {
    val arrs = Array<String>(this.size, { i -> "" })
    for (i in this.indices) {
        arrs[i] = this[i]
    }
    return arrs
}

fun <T, T2> List<T>.toHashMap(func: (k: T) -> T2): HashMap<T2, T> {
    val map = HashMap<T2, T>()
    for (v in this) {
        map[func(v)] = v;
    }
    return map
}

class PairClass<T>(val list: ArrayList<Pair<String, T>>) {
    /**
     * 重载-号，赋值给list
     */
    operator fun String.minus(that: T) {
        list.add(Pair(this, that))
    }
}


/**
 * 用于构造ArrayList<Pair>
 */
inline fun <T> arrayPair(action: PairClass<T>.() -> Unit): ArrayList<Pair<String, T>> {
    val list = ArrayList<Pair<String, T>>();
    action(PairClass(list));
    return list
}

/**
 * 用于构造ArrayList<Pair>
 */
inline fun <T> arrayPairTo(
    list: ArrayList<Pair<String, T>>,
    action: PairClass<T>.() -> Unit
): ArrayList<Pair<String, T>> {
    action(PairClass(list));
    return list
}

/**
 * 倒序遍历
 */
inline fun <T> List<T>?.forEachDescIndexed(action: (Int, T) -> Boolean) {
    if (this != null) {
        for (i in this.size - 1 downTo 0) {
            if (!action(i, this[i]))
                break
        }
    }
}

inline fun <T> List<T>?.each(selector: (T) -> Boolean): Boolean {
    if (this != null) {
        for (v in this) {
            if (!selector(v)) {
                return false;
            }
        }
    }
    return true;
}

inline fun <T, T1 : Comparable<T1>, T2 : Comparable<T2>> MutableList<T>?.sortBy2(
    crossinline selector: (T) -> T1,
    crossinline selector2: (T) -> T2
) {
    if (this != null) {
        this.sortWith(Comparator { l, r ->
            val res = selector(l).compareTo(selector(r));
            if (res == 0) {
                return@Comparator selector2(l).compareTo(selector2(r));
            }
            return@Comparator res;
        });
    }
}

inline fun <T, T1 : Comparable<T1>, T2 : Comparable<T2>> MutableList<T>?.sortBy2Desc(
    crossinline selector: (T) -> T1,
    crossinline selector2: (T) -> T2
) {
    if (this != null) {
        this.sortWith(Comparator { l, r ->
            val res = selector(r).compareTo(selector(l));
            if (res == 0) {
                return@Comparator selector2(r).compareTo(selector2(l));
            }
            return@Comparator res;
        });
    }
}


inline fun <T> MutableList<T>?.pop(func: (last: T) -> Unit): Boolean {
    if (this != null && this.size > 0) {
        func(this.get(this.size - 1));
        this.removeAt(this.size - 1)
        return true
    }
    return false;
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