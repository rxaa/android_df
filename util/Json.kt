package net.rxaa.util

import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.ParameterizedType
import java.util.*


class JSONObjBuild(val obj: JSONObject, val encodeHZ: Boolean = false) {
    public operator fun <B : Any> String.minus(that: B) {
        obj.put(this, that);
    }

    public operator fun <T : Any> String.minus(that: List<T>) {
        obj.put(this, JSONArray().apply {
            that.forEach { put(it) }
        });
    }

    public operator fun <T : Any> String.minus(that: Array<T>) {
        obj.put(this, JSONArray().apply {
            that.forEach { put(it) }
        });
    }

    override fun toString() = obj.toString()
}


/**
 * 构造JSONObject对象
 */
inline fun jsonObj(encodeHZ: Boolean = false, func: JSONObjBuild.() -> Unit): JSONObject {
    val ret = JSONObjBuild(JSONObject(), encodeHZ)
    func(ret)
    return ret.obj
}


object Json {

    /**
     * 转换List Pair为json对象字串
     */
    @JvmStatic
    @JvmOverloads
    fun <T> arrayToJsonObj(objArr: List<Pair<String, T>>, encodeHZ: Boolean = false): String {
        val ret = StringBuilder()
        ret.append("{ ")
        for (v in objArr) {
            ret.append("\"" + v.first + "\":")
            Json.objToJson(v.second, encodeHZ, ret);
            ret.append(',')
        }
        ret.setLength(ret.length - 1)
        ret.append("}")

        return ret.toString()
    }

    /**
     * 转义json字串
     */
    private fun jsonString(obj: Any?, encodeHZ: Boolean = false): String {
        var ret = "\"";
        for (s in obj.toString()) {
            when (s) {
                '"', '\\' -> {
                    ret += '\\'
                    ret += s;
                }
                '\r' -> {
                    ret += "\\r";
                }
                '\n' -> {
                    ret += "\\n";
                }
                '\t' -> {
                    ret += "\\t";
                }
                else -> {
                    if (encodeHZ && (s.toInt() <= 0 || s.toInt() >= 0xFF))
                        ret += String.format("\\u%04x", s.toInt())
                    else
                        ret += s;

                }
            }
        }

        ret += "\"";

        return ret
    }


    /**
     * 将object转换为json字串
     */
    @JvmStatic
    @JvmOverloads
    fun objToJson(obj: Any?, encodeHZ: Boolean = false): String {
        val ret = StringBuilder();
        objToJson(obj, encodeHZ, ret);
        return ret.toString()
    }

    /**
     * 将object转换为json字串
     */
    @JvmStatic
    @JvmOverloads
    fun objToJson(obj: Any?, encodeHZ: Boolean = false, ret: StringBuilder) {
        if (obj == null) {
            ret.append("null")
            return;
        }

        when (obj) {
            is String? -> {
                ret.append(jsonString(obj, encodeHZ))
                return
            }
            is JsonBuilder?,
            is JSONArray?,
            is JSONObject?,
            is Int?, is Long?, is Double?, is Boolean?,is Float? -> {
                ret.append("" + obj)
                return
            }
            is Array<*>? -> {
                ret.append("[ ")
                for (v in obj) {
                    objToJson(v, encodeHZ, ret)
                    ret.append(',')
                }
                ret.setLength(ret.length - 1)
                ret.append("]")
            }
            is List<*>? -> {
                ret.append("[ ")
                for (v in obj) {
                    objToJson(v, encodeHZ, ret)
                    ret.append(',')
                }
                ret.setLength(ret.length - 1)
                ret.append("]")
            }
            is Map<*, *>? -> {
                ret.append("{ ")
                for ((key, valu) in obj) {
                    ret.append("\"" + key + "\":")
                    objToJson(valu, encodeHZ, ret);
                    ret.append(',')
                }
                ret.setLength(ret.length - 1)
                ret.append("}")
            }
            else -> {
                ret.append("{ ")
                df.getClassFields(obj.javaClass, true) { field, i ->
                    ret.append('"' + field.name + "\":")
                    objToJson(field.get(obj), encodeHZ, ret)
                    ret.append(',')
                }
                ret.setLength(ret.length - 1)
                ret.append("}")
            }
        }
    }

    /**
     * 将json字串传换为java对象
     */
    @JvmStatic
    fun <T : Any> jsonToObj(json: String, obj: T): T {
        val j = JSONObject(json);
        jsonToObj(j, obj);
        return obj;
    }

    fun <T : Any> jsonToObj(json: JSONObject?, obj: HashMap<String, T>, clas: Class<*>?, para: ParameterizedType? = null): HashMap<String, T> {
        if (json == null)
            return obj;

        if (para != null) {
            if (para.rawType == ArrayList::class.java) {
                for (vl in json.keys()) {
                    val o = json.optJSONArray(vl);
                    if (o != null) {
                        val tt = para.actualTypeArguments[0];
                        if (tt is ParameterizedType) {
                            obj.put(vl, jsonToObj(o, ArrayList<Any>(), null, tt) as T);
                        } else
                            obj.put(vl, jsonToObj(o, ArrayList<Any>(), tt as Class<*>) as T);
                    }
                }
            } else if (para.rawType == HashMap::class.java) {
                for (vl in json.keys()) {
                    val o = json.optJSONObject(vl)
                    if (o != null) {
                        val tt = para.actualTypeArguments[1];
                        if (tt is ParameterizedType) {
                            obj.put(vl, jsonToObj(o, HashMap<String, Any>(), null, para) as T);
                        } else {
                            obj.put(vl, jsonToObj(o, HashMap<String, Any>(), tt as Class<*>) as T);
                        }

                    }
                }
            }
            return obj;
        }

        if (clas == null)
            return obj;

        when (clas) {
            String::class.java -> {
                for (vl in json.keys()) {
                    obj.put(vl, json.optString(vl) as T);
                }
            }
            Integer::class.java,
            Int::class.java -> {
                for (vl in json.keys()) {
                    obj.put(vl, json.optInt(vl) as T);
                }
            }
            java.lang.Long::class.java,
            Long::class.java -> {
                for (vl in json.keys()) {
                    obj.put(vl, json.optLong(vl) as T);
                }
            }
            java.lang.Double::class.java,
            Double::class.java -> {
                for (vl in json.keys()) {
                    obj.put(vl, json.optDouble(vl) as T);
                }
            }
            java.lang.Boolean::class.java,
            Boolean::class.java -> {
                for (vl in json.keys()) {
                    obj.put(vl, json.optBoolean(vl, false) as T);
                }
            }
            else -> {
                for (vl in json.keys()) {
                    val o = json.optJSONObject(vl);
                    if (o != null)
                        obj.put(vl, jsonToObj(o, clas.newInstance()) as T);
                }
            }
        }
        return obj;
    }

    @JvmStatic
    fun <T : Any?> jsonToObj(json: JSONArray?, obj: MutableList<T>, clas: Class<*>?, para: ParameterizedType? = null): MutableList<T> {
        if (json == null || clas == null)
            return obj

        if (para != null) {
            if (para.rawType == ArrayList::class.java) {
                loop@ for (i in 0..json.length() - 1) {
                    val tt = para.actualTypeArguments[0];
                    if (tt is ParameterizedType) {
                        obj.add(jsonToObj(json.optJSONArray(i), ArrayList<Any>(), null, tt) as T)
                    } else {
                        obj.add(jsonToObj(json.optJSONArray(i), ArrayList<Any>(), tt as Class<*>) as T)
                    }
                }
            } else if (para.rawType == HashMap::class.java) {
                loop@ for (i in 0..json.length() - 1) {
                    val tt = para.actualTypeArguments[1];
                    if (tt is ParameterizedType) {
                        obj.add(jsonToObj(json.optJSONObject(i), HashMap<String, Any>(), null, tt) as T)
                    } else {
                        obj.add(jsonToObj(json.optJSONObject(i), HashMap<String, Any>(), tt as Class<*>) as T)
                    }
                }
            }

            return obj;
        }

        loop@ for (i in 0..json.length() - 1) {
            when (clas) {
                String::class.java -> {
                    obj.add(json.optString(i, "") as T);
                }
                Integer::class.java,
                Int::class.java -> {
                    obj.add(json.optInt(i, 0) as T);
                }
                java.lang.Long::class.java,
                Long::class.java -> {
                    obj.add(json.optLong(i, 0L) as T);
                }
                java.lang.Double::class.java,
                Double::class.java -> {
                    obj.add(json.optDouble(i, 0.0) as T);
                }
                java.lang.Boolean::class.java,
                Boolean::class.java -> {
                    obj.add(json.optBoolean(i, false) as T);
                }
                else -> {
                    obj.add(jsonToObj(json.getJSONObject(i), clas.newInstance()) as T)
                }
            }
        }
        return obj
    }

    @JvmStatic
    fun <T : Any> jsonToObj(json: JSONObject?, obj: T?): T? {
        if (json == null || obj == null)
            return obj
        df.getClassFields(obj.javaClass) { f, _ ->
            if (f.getAnnotation(JSONIgnore::class.java) != null)
                return@getClassFields

            var subObj = f.get(obj);
            val ty = if (subObj == null) f.type else subObj.javaClass;
            when (ty) {
                String::class.java -> {
                    val r = json.opt(f.name);
                    if (r != null)
                        f.set(obj, "" + r);
                }
                Integer::class.java -> {
                    val r = json.opt(f.name) ?: return@getClassFields
                    if (r is Int) {
                        f.set(obj, r);
                    } else if (r is Number) {
                        f.set(obj, r.toInt())
                    } else if (r is String && r.length > 0) {
                        f.set(obj, java.lang.Double.parseDouble(r).toInt())
                    }
                }
                Int::class.java -> {
                    val r = json.opt(f.name) ?: return@getClassFields
                    if (r is Int) {
                        f.setInt(obj, r);
                    } else if (r is Number) {
                        f.setInt(obj, r.toInt())
                    } else if (r is String && r.length > 0) {
                        f.setInt(obj, java.lang.Double.parseDouble(r).toInt())
                    }
                }
                java.lang.Long::class.java -> {
                    val r = json.opt(f.name) ?: return@getClassFields
                    if (r is Long) {
                        f.set(obj, r);
                    } else if (r is Number) {
                        f.set(obj, r.toLong())
                    } else if (r is String && r.length > 0) {
                        f.set(obj, java.lang.Double.parseDouble(r).toLong())
                    }
                }
                Long::class.java -> {
                    val r = json.opt(f.name) ?: return@getClassFields
                    if (r is Long) {
                        f.setLong(obj, r);
                    } else if (r is Number) {
                        f.setLong(obj, r.toLong())
                    } else if (r is String && r.length > 0) {
                        f.setLong(obj, java.lang.Double.parseDouble(r).toLong())
                    }
                }
                java.lang.Double::class.java -> {
                    val r = json.opt(f.name) ?: return@getClassFields
                    if (r is Double) {
                        f.set(obj, r);
                    } else if (r is Number) {
                        f.set(obj, r.toDouble())
                    } else if (r is String && r.length > 0) {
                        f.set(obj, r.toDouble())
                    }
                }
                Double::class.java -> {
                    val r = json.opt(f.name) ?: return@getClassFields
                    if (r is Double) {
                        f.setDouble(obj, r);
                    } else if (r is Number) {
                        f.setDouble(obj, r.toDouble())
                    } else if (r is String && r.length > 0) {
                        f.setDouble(obj, r.toDouble())
                    }
                }
                HashMap::class.java -> {
                    val genericType = f.genericType
                    if (genericType is ParameterizedType && genericType.actualTypeArguments.size > 1) {
                        //var map = f.get(obj) as HashMap<*, *>?
                        if (subObj == null) {
                            subObj = HashMap<Any?, Any?>()
                            f.set(obj, subObj)
                        }
                        val tt = genericType.actualTypeArguments[1];
                        if (tt is ParameterizedType) {
                            val mapObj = json.optJSONObject(f.name);
                            if (mapObj != null)
                                Json.jsonToObj(mapObj, subObj as HashMap<String, *>, null, tt);
                        } else {
                            val mapObj = json.optJSONObject(f.name);
                            if (tt != null && mapObj != null)
                                Json.jsonToObj(mapObj, subObj as HashMap<String, T>, tt as Class<*>);
                        }
                    }
                }
                java.lang.Boolean::class.java,
                Boolean::class.java -> {
                    val r = json.opt(f.name) ?: return@getClassFields
                    if (r is Boolean) {
                        f.setBoolean(obj, r);
                    } else if (r is Number) {
                        f.setBoolean(obj, r != 0)
                    } else if (r is String && r.length > 0) {
                        f.setBoolean(obj, r.toBoolean())
                    }
                }
                ArrayList::class.java,
                MutableList::class.java -> {
                    val genericType = f.genericType
                    if (genericType is ParameterizedType) {
                        if (subObj == null) {
                            subObj = ArrayList<Any?>()
                            f.set(obj, subObj)
                        }
                        val tt = genericType.actualTypeArguments[0];
                        if (tt is ParameterizedType) {
                            jsonToObj(json.optJSONArray(f.name), subObj as ArrayList<*>, null, tt)
                        } else
                            jsonToObj(json.optJSONArray(f.name), subObj as ArrayList<*>, tt as Class<*>)
                    }

                }
                JSONObject::class.java -> {
                    val r = json.optJSONObject(f.name) ?: return@getClassFields
                    f.set(obj, r)
                }
                JSONArray::class.java -> {
                    val r = json.optJSONArray(f.name) ?: return@getClassFields
                    f.set(obj, r)
                }
                else -> {
                    if (subObj == null) {
                        subObj = f.type.newInstance();
                        f.set(obj, subObj)
                    }
                    val subJson = json.optJSONObject(f.name);

                    jsonToObj(subJson, subObj)

                }
            }

        }
        return obj;
    }


    /////////////////////
}