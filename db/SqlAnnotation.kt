package net.rxaa.db

import kotlin.reflect.KClass


/**
 * 注解class字段名
 */
@Target(AnnotationTarget.FIELD)
annotation class FieldName(val value: String)

/**
 * 注解class字段的表名
 */
@Target(AnnotationTarget.FIELD)
annotation class FieldTable(val value: KClass<*>)

/**
 * 注解class表名
 */
@Target(AnnotationTarget.CLASS)
annotation class TableName(val value: String)

/**
 * 标识sqlite忽略解析的字段
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class SqlIgnore

/**
 * 标识sqlite忽略更新的字段
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class NotUpdate

/**
 * 标识sqlite唯一
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class SqlUnique

/**
 * 主键

 */
@Target(AnnotationTarget.FIELD)
annotation class PrimaryKey

/**
 *
 * 标识sqlite索引
 *
 */
@Target(AnnotationTarget.FIELD)
annotation class SqlIndex


/**
 *
 * 标识sqlite自增长字段
 *
 */
@Target(AnnotationTarget.FIELD)
annotation class Autoincrement

/**
 * 标识非空字段
 */
@Target(AnnotationTarget.FIELD)
annotation class SqlNotNull

/**
 * 标识sqlite字段缺省值
 */
@Target(AnnotationTarget.FIELD)
annotation class SqlDefault(val value: String)


@Target(AnnotationTarget.FIELD)
annotation class SqlJSON



