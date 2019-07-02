package rxaa.df

import kotlin.annotation.AnnotationRetention.*
import kotlin.reflect.KClass


/**
 * 指定json2obj忽略解析的字段

 */
@Retention(RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class JSONIgnore
