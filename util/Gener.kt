package net.rxaa.util

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

object Gener {

    @JvmStatic
    fun getType(obj: Any): Array<Type> {
        val type = obj.javaClass.genericSuperclass;
        if (type is ParameterizedType) {
            return type.actualTypeArguments
        }

        return arrayOf<Type>()
    }
}