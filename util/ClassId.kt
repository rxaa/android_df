package net.rxaa.util

object ClassId {

    val viewMap = HashMap<Class<*>, Int>();

    var classId = 0;

    /**
     * 获取指定类型对应的唯一id
     */
    fun getId(c: Class<*>): Int {
        return viewMap.getOrPut(c) {
            ++classId;
        }
    }
}