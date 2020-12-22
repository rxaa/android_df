package net.rxaa.util

import kotlin.annotation.AnnotationRetention.*


/**
 * 指定json2obj忽略解析的字段

 */
@Retention(RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class JSONIgnore
