package net.rxaa.util

import org.json.JSONArray
import org.json.JSONObject
import java.util.*


fun JSONObject.obj(k: String): JSONObject? {
    val obj = this.opt(k);
    if (obj is JSONObject)
        return obj;
    return null;
}

fun JSONObject.array(k: String): JSONArray? {
    val obj = this.opt(k);
    if (obj is JSONArray)
        return obj;
    return null;
}

inline fun JSONObject.forEach(func: (k: String) -> Unit) {
    val ite = this.keys()
    while (ite.hasNext()) {
        func(ite.next());
    }
}

inline fun JSONObject.forEachItor(func: (k: String, ite: Iterator<String>) -> Unit) {
    val ite = this.keys()
    while (ite.hasNext()) {
        func(ite.next(), ite);
    }
}

inline fun <T> JSONArray.forEach(func: (k: T) -> Unit) {
    var i = 0;

    while (i < this.length()) {
        func(this.opt(i) as T);
        i++;
    }
}

inline fun JSONArray.forEachString(func: (k: String) -> Unit) {
    var i = 0;

    while (i < this.length()) {
        func("" + this.opt(i));
        i++;
    }
}

inline fun <T> JSONObject.map(func: (k: String) -> T): ArrayList<T> {
    val list = ArrayList<T>();
    forEach { list.add(func(it)) }
    return list;
}

inline operator fun JSONObject.set(key: String, v: String) {
    this.put(key, v);
}


inline operator fun JSONObject.set(key: String, v: Int) {
    this.put(key, v);
}

inline operator fun JSONObject.set(key: String, v: Long) {
    this.put(key, v);
}

inline operator fun JSONObject.set(key: String, v: Double) {
    this.put(key, v);
}

inline operator fun JSONObject.set(key: String, v: Boolean) {
    this.put(key, v);
}

inline operator fun <T> JSONObject.set(key: String, v: T) {
    this.put(key, v);
}

class JsonBuilder(val encodeHZ: Boolean = false, var arrArr: List<Any>? = null) {

    val objArr = ArrayList<Pair<String, Any>>();

    public operator fun <B : Any> String.minus(that: B) {
        objArr.add(Pair(this, that))
    }

    override fun toString(): String {

        if (arrArr != null) {
            return Json.objToJson(arrArr, encodeHZ)
        }

        return Json.arrayToJsonObj(objArr, encodeHZ);
    }
}

/**
 * 构造json数组字串
 */
fun jsonArr(obj: List<Any>, encodeHZ: Boolean = false): JsonBuilder {
    val ret = JsonBuilder(encodeHZ, obj)
    return ret
}

/**
 * 构造json数组字串
 */
fun jsonArr(vararg obj: Any, encodeHZ: Boolean = false): JsonBuilder {
    val ret = JsonBuilder(encodeHZ, obj.toCollection(ArrayList<Any>()))
    return ret
}