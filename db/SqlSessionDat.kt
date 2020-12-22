package net.rxaa.db

import java.util.*
import java.util.concurrent.ConcurrentHashMap


object SqlSessionDat {


    private val classMap = ConcurrentHashMap<String, Any>()

    /**
     * 获取字段名函数列表
     */
    val fieldNameArray = ArrayList<(addTable: Boolean) -> String>();


    fun getFieldName(row: Int, addTable: Boolean = false): String {
        return fieldNameArray[row](addTable)
    }


    fun <T : Any> getClassSqlData(clas: Class<T>): SqlData<T>? {
        return classMap[clas.name + "_SqlData_"] as SqlData<T>?
    }

    fun <T : Any> setClassSqlData(clas: Class<T>, obj: SqlData<T>) {
        classMap[clas.name + "_SqlData_"] = obj
    }


}
