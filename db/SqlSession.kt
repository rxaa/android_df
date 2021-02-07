package net.rxaa.db

import android.database.Cursor
import org.json.JSONArray
import net.rxaa.util.Json
import net.rxaa.util.df
import net.rxaa.ext.removeLast
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.util.*
import kotlin.reflect.KClass


class SqlData<T : Any>(classT: Class<T>) {
    val classInst: T = classT.newInstance()

    val fields = ArrayList<Field>();
    val getFieldArr = ArrayList<(addTable: Boolean) -> String>();


    var primaryKeyI = 0;


    fun getPrimaryKey(): String {
        return getFieldNameByIndex(primaryKeyI)
    }

    fun getPrimaryKeyVal(obj: Any): Any? {
        return fields[primaryKeyI].get(obj)
    }

    /**
     * 获取 表名.原始字段名 列表
     */
    fun getFieldNameByIndex(row: Int, addTableName: Boolean = false): String {
        return getFieldArr[row](addTableName)
    }

    /**
     * 获取 别名 列表
     */
    //    fun getAsName(row: Int): String {
//        if (fieldAsNames[row].isNotEmpty())
//            return getFieldName(row) + " as " + fieldAsNames[row];
//
//        return getFieldName(row)
//    }


//    fun getAsNameList() = df.joinStr(fieldRawNames.size, ",") {
//        getAsName(it)
//    }

//    fun getFieldList() = df.joinStr(fieldRawNames.size, ",") {
//        getFieldName(it)
//    }

    //,号分隔的字段列表
    var fieldsStr = ""

    val tableName = getTableName(classT)

    init {

        df.getClassFields(classT) { f, index ->
            fields.add(f)

            val asName = f.getAnnotation(FieldName::class.java);
            val name = asName?.value ?: f.name

            val fieldTable = f.getAnnotation(FieldTable::class.java)
            val table = if (fieldTable != null) {
                getTableName(fieldTable.value)
            } else {
                ""
            }
            val getName = fun(addTable: Boolean): String {
                if (addTable)
                    return "$tableName.`$name`"

                if (table.isNotEmpty())
                    return "$table.`$name`"

                return "`$name`"
            }

            val count = SqlSessionDat.fieldNameArray.size;
            SqlSessionDat.fieldNameArray.add(getName);
            getFieldArr.add(getName);


            fieldsStr += getName(false) + ","

            val priAn = f.getAnnotation(PrimaryKey::class.java)
            if (priAn != null) {
                primaryKeyI = index;
            }

            when (f.type) {
                String::class.java -> {
                    f.set(classInst, "" + count)
                }
                Integer::class.java -> {
                    f.set(classInst, count)
                }
                Int::class.java -> {
                    f.setInt(classInst, count)
                }
                java.lang.Long::class.java -> {
                    f.set(classInst, count.toLong())
                }
                Long::class.java -> {
                    f.setLong(classInst, count.toLong())
                }
                java.lang.Double::class.java -> {
                    f.set(classInst, count.toDouble())
                }
                Double::class.java -> {
                    f.setDouble(classInst, count.toDouble())
                }
            }
        }

        fieldsStr = fieldsStr.removeLast();
        SqlSessionDat.setClassSqlData(classT, this)
    }


    companion object {
        fun getFieldName(f: Field, quto: Boolean = true): String {

            val fAnn = f.getAnnotation(FieldName::class.java)
            val name = fAnn?.value ?: f.name
            if (quto)
                return "`$name`";

            return name;
        }

        fun getTableName(f: Class<*>, quto: Boolean = true): String {
            val fAnn = f.getAnnotation(TableName::class.java)

            val name = fAnn?.value?.replace(".", "_") ?: f.simpleName.replace(".", "_")

            if (quto)
                return "`$name`";

            return name;
        }

        fun getTableName(f: KClass<*>): String {
            return "`" + f.simpleName?.replace(".", "_") + "`"
        }


        fun sqlFilter(valu: String): String {
            //return "'" + valu.replace("'", "''").replace("\\", "\\\\") + "'"
            return "'" + valu.replace("'", "''") + "'"
        }
    }
}


class SqlSession<T : Any>(
    val classT: Class<T>,
    val connect: SqliteConnect,
    val sqlJoin: ISqlJoin? = null,
    var tableName: String = SqlData.getTableName(classT)
) {


    fun getTableData() = SqlSessionDat.getClassSqlData(classT) ?: SqlData(classT);

    internal var tableData: SqlData<T> = getTableData()
    internal var where = ""
    internal var sqlStr = StringBuilder();
    internal var limit: String = ""
    internal var order: String = ""


    init {
        if (sqlJoin != null)
            tableName = sqlJoin.session.where;
    }


    fun copy(): SqlSession<T> {
        val s = SqlSession(classT, connect, sqlJoin, tableName);
        s.where = where;
        s.sqlStr = sqlStr;
        s.limit = limit
        s.order = order;
        return s;
    }

    /**
     * 清空sql语句
     */
    protected fun initSql() {
        sqlStr.setLength(0)
        where = ""
        limit = ""
        order = ""
    }


    /**
     * 获取最后自增长id
     */
    fun getLastInsertId(): Long {
        return connect.getOneLong("select last_insert_rowid() from " + tableName)
    }


    /**
     *  连表查询
     * @param table2 join的表名
     * @param func on 条件表达式
     * @returns {SqlBuilder}
     */
    private fun <T1 : Any> makeJoin(
        table2: Class<T1>,
        func: SqlOp<T>.(l: T, r: T1) -> Unit,
        joinStr: String
    ): SqlJoin2<T, T1> {
        if (where.isEmpty()) {
            where = tableName;
        }
        val right = SqlSession(table2, connect);
        val op = SqlOp(tableData.classInst, this, true)
        where += joinStr + right.tableName + " on "
        op.func(tableData.classInst, right.tableData.classInst);
        return SqlJoin2(this, right);
    }

    /**
     *  内连表查询
     * @param right join的表名
     * @param func on 条件表达式
     * @returns {SqlBuilder}
     */
    fun <T1 : Any> innerJoin(
        right: Class<T1>,
        func: SqlOp<T>.(l: T, r: T1) -> Unit
    ): SqlJoin2<T, T1> {
        return this.makeJoin(right, func, " inner join ");
    }

    /**
     *  左连表查询
     * @param right join的表名
     * @param func on 条件表达式
     * @returns {SqlBuilder}
     */
    fun <T2 : Any> leftJoin(
        right: Class<T2>,
        func: SqlOp<T>.(l: T, r: T2) -> Unit
    ): SqlJoin2<T, T2> {
        return this.makeJoin(right, func, " left join ");
    }

    /**
     *  右连表查询
     * @param right join的表名
     * @param func on 条件表达式
     * @returns {SqlBuilder}
     */
    fun <T1 : Any> rightJoin(
        right: Class<T1>,
        func: SqlOp<T>.(l: T, r: T1) -> Unit
    ): SqlJoin2<T, T1> {
        return this.makeJoin(right, func, " right join ");
    }

    private fun initInto(obj: T, prefix: String = "insert", ignoreAutoInc: Boolean = true) {
        sqlStr.append(prefix + " into ${tableName} (${tableData.fieldsStr}) values(")
        for (f in tableData.fields) {
            val v = getFieldString(f, obj)


            if (v == null)
                sqlStr.append("null,")
            else if (ignoreAutoInc && f.getAnnotation(Autoincrement::class.java) != null) {
                sqlStr.append("null,")
            } else if (v is String)
                sqlStr.append(SqlData.sqlFilter(v) + ",")
            else {
                sqlStr.append("'$v',")
            }
        }

        sqlStr.setLength(sqlStr.length - 1)
        sqlStr.append(")")
    }

    /**
     * 插入一个对象
     */
    @JvmOverloads
    @Throws(Exception::class)
    fun insert(obj: T?, ignoreAutoInc: Boolean = true): SqlSession<T> {
        if (obj == null)
            return this
        initSql()
        initInto(obj, "insert", ignoreAutoInc)
        connect.update(sqlStr.toString())
        return this
    }


    /**
     * 替换一个对象
     */
    @Throws(Exception::class)
    fun replaceInto(obj: T?): SqlSession<T> {
        if (obj == null)
            return this
        initSql()
        initInto(obj, "replace", false)
        connect.update(sqlStr.toString())
        return this
    }

    /**
     * 添加一列数据

     * @param li
     * *
     * @throws Exception
     */
    @JvmOverloads
    @Throws(Exception::class)
    fun insert(li: Iterable<T?>?, ignoreAutoInc: Boolean = true): SqlSession<T> {
        if (li == null)
            return this

        connect.db!!.beginTransaction()
        try {
            for (m in li) {
                insert(m, ignoreAutoInc)
            }
            connect.db!!.setTransactionSuccessful()
        } finally {
            connect.db!!.endTransaction()
        }

        return this
    }

    @JvmOverloads
    @Throws(Exception::class)
    fun insert(li: Array<T?>, ignoreAutoInc: Boolean = true): SqlSession<T> {
        return insert(li.asIterable(), ignoreAutoInc)
    }


    /**
     * 通过对象的主键来更新对象,忽略where条件

     * @param obj
     * *
     * @throws Exception
     */
    @Throws(Exception::class)
    fun update(obj: T?): SqlSession<T> {
        if (obj == null)
            return this

        initSql()
        sqlStr.append("update ${tableName} set ")

        var i = -1
        for (f in tableData.fields) {
            i++;
            if (f.getAnnotation(NotUpdate::class.java) != null) {
                continue
            }

            val v = getFieldString(f, obj);
            if (v != null && i != tableData.primaryKeyI) {

                if (v is String) {
                    sqlStr.append(
                        tableData.getFieldNameByIndex(i) + "="
                                + SqlData.sqlFilter(v) + ","
                    )
                } else {
                    sqlStr.append(tableData.getFieldNameByIndex(i) + "='" + v + "',")
                }

            }
        }
        sqlStr.setLength(sqlStr.length - 1)
        sqlStr.append(
            " where " + tableData.getPrimaryKey() + "='"
                    + tableData.getPrimaryKeyVal(obj) + "'"
        )

        connect.update(sqlStr.toString())

        return this
    }

    /**
     * 在已有的where条件上执行
     */
    @Throws(Exception::class)
    fun update(func: SqlUpdateOp<T>.(res: T) -> T): SqlSession<T> {

        sqlStr.setLength(0)

        sqlStr.append("update ${tableName} set ")

        val op = SqlUpdateOp(tableData.classInst, this)

        op.func(tableData.classInst)

        sqlStr.setLength(sqlStr.length - 1)

        addCondition(false)

        connect.update(sqlStr.toString())
        return this
    }

    /**
     * 在已有的where条件上执行delete
     * *
     * @return
     * *
     * @throws Exception
     */
    @Throws(Exception::class)
    fun delete(): SqlSession<T> {
        sqlStr.setLength(0)
        sqlStr.append("delete from ${tableName} ")
        addCondition(false)
        connect.update(sqlStr.toString())

        return this
    }

    fun limit(start: Long, count: Long): SqlSession<T> {
        limit = " limit $start,$count "
        return this
    }

    fun limit(count: Long): SqlSession<T> {
        limit = " limit $count"
        return this;
    }

    fun order(func: SqlOrderOp<T>.(res: T) -> Unit): SqlSession<T> {
        order = " order by "
        val op = SqlOrderOp(tableData.classInst, this)
        op.func(tableData.classInst)
        order = order.substring(0, order.length - 1)
        return this;
    }

    fun where(func: SqlOp<T>.(res: T) -> Unit): SqlSession<T> {
        return and(func)
    }

    fun and(func: SqlOp<T>.(res: T) -> Unit): SqlSession<T> {
        val op = SqlOp(tableData.classInst, this)
        where += " and ("
        op.func(tableData.classInst)
        where += ") "
        return this
    }

    fun or(func: SqlOp<T>.(res: T) -> Unit): SqlSession<T> {
        val op = SqlOp(tableData.classInst, this)
        where += "  or ("
        op.func(tableData.classInst)
        where += ") "
        return this
    }

    public fun addCondition(hasOrder:Boolean) {
        if (where.length > 4) {
            if (where != " and () " && where != " or () ")
                sqlStr.append(" where " + where.substring(4))
            where = ""
        }

        if (hasOrder && order.length > 0)
            sqlStr.append(order)

        if (limit.length > 0)
            sqlStr.append(limit)
    }

    fun _initSelect() {
        sqlStr.setLength(0)
        sqlStr.append("select ${tableData.fieldsStr} from ${tableName} ")
    }

    /**
     * 获取生成的sql语句
     */
    fun getSqlStr(): String {
        return sqlStr.toString()
    }


    /**
     * 回调函数获取select结果
     */
    @Throws(Exception::class)
    inline fun select(func: (res: T) -> Unit): SqlSession<T> {
        _initSelect()
        addCondition(true)

        connect.query(getSqlStr()) {
            func(cursorToObj(it))
        }

        return this
    }

    /**
     * 获取select单个对象
     */
    @Throws(Exception::class)
    fun toOne(): T? {
        limit(1)
        select { return it }
        return null;
    }

    /**
     * 获取select多个对象
     */
    @Throws(Exception::class)
    fun toArray(): ArrayList<T> {
        val list = ArrayList<T>()

        select {
            list.add(it)
        }

        return list
    }


    @Throws(Exception::class)
    fun count(): Long {
        sqlStr.setLength(0)
        sqlStr.append("select count(*) from " + tableName)
        addCondition(true)

        return connect.getOneLong(sqlStr.toString())
    }


    /**
     * 解析cursor为T对象
     */
    @Throws(Exception::class)
    public fun cursorToObj(cursor: Cursor): T {
        val obj = classT.newInstance()
        var ex: Exception? = null;
        for (i in 0 until cursor.columnCount) {
            try {
                val fi = tableData.fields[i]

                when (fi.type) {
                    String::class.java -> {
                        fi.set(obj, cursor.getString(i))
                    }
                    Integer::class.java,
                    Int::class.java -> {
                        fi.set(obj, cursor.getInt(i))
                    }
                    java.lang.Long::class.java,
                    Long::class.java -> {
                        fi.set(obj, cursor.getLong(i))
                    }
                    java.lang.Float::class.java,
                    java.lang.Double::class.java,
                    Float::class.java,
                    Double::class.java -> {
                        fi.set(obj, cursor.getDouble(i))
                    }
                    List::class.java, ArrayList::class.java -> {
                        val gen = fi.genericType
                        if (gen is ParameterizedType) {
                            var v = fi.get(obj);
                            if (v == null) {
                                v = ArrayList<Any>()
                                fi.set(obj, v)
                            }
                            Json.jsonToObj(
                                JSONArray(cursor.getString(i)),
                                v as ArrayList<Any?>,
                                gen.actualTypeArguments[0] as Class<*>
                            )
                        }
                    }
                    else -> {
                        //解析嵌套对象
                        //                        if (fi.getAnnotation(SqlJSON::class.java) != null) {
                        var v = fi.get(obj);
                        if (v == null) {
                            v = fi.type.newInstance()
                            fi.set(obj, v)
                        }
                        Json.jsonToObj(cursor.getString(i), v)
                        //                        }
                    }

                }
            } catch (e: Exception) {
                // TODO Auto-generated catch block
                ex = e;
                continue
            }

        }

        return obj
    }

    companion object {
        fun getFieldString(f: Field, obj: Any): Any? {
            try {
                if (f.getAnnotation(SqlJSON::class.java) != null) {
                    return Json.objToJson(f.get(obj))
                }
                return f.get(obj)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return ""
        }
    }

    /**
     * order的操作函数
     */
    class SqlOrderOp<T : Any>(private val obj: T, private val sql: SqlSession<T>) {
        private fun orderAdd(i: Long, ord: String): T {
            sql.order += " " + SqlSessionDat.getFieldName(i.toInt()) + " " + ord + ","
            return obj
        }

        val Comparable<*>?.asc: T
            get() =
                if (this is String)
                    orderAdd(this.toLong(), "asc")
                else if (this is Long)
                    orderAdd(this.toLong(), "asc")
                else if (this is Int)
                    orderAdd(this.toLong(), "asc")
                else
                    obj

        val Comparable<*>?.desc: T
            get() =
                if (this is String)
                    orderAdd(this.toLong(), "desc")
                else if (this is Long)
                    orderAdd(this.toLong(), "desc")
                else if (this is Int)
                    orderAdd(this.toLong(), "desc")
                else
                    obj

        val String?.asc: T
            get() = orderAdd(this!!.toLong(), "asc")

        val String?.desc: T
            get() = orderAdd(this!!.toLong(), "desc")

        val Int?.asc: T
            get() = orderAdd(this!!.toLong(), "asc")

        val Int?.desc: T
            get() = orderAdd(this!!.toLong(), "desc")

        val Long?.asc: T
            get() = orderAdd(this!!, "asc")

        val Long?.desc: T
            get() = orderAdd(this!!, "desc")

        val Double?.asc: T
            get() = orderAdd(this!!.toLong(), "asc")

        val Double?.desc: T
            get() = orderAdd(this!!.toLong(), "desc")
    }


    /**
     * update的操作函数
     */
    class SqlUpdateOp<T : Any>(private val obj: T, private val sql: SqlSession<T>) {

        private fun setAdd(i: Long, op: String, value: String) {
            sql.sqlStr.append(" " + SqlSessionDat.getFieldName(i.toInt()) + " " + op + " " + value + ",")
        }

        fun String?.set(r: String): T {
            setAdd(this!!.toLong(), "=", SqlData.sqlFilter(r))
            return obj
        }

        fun Int?.set(r: Int): T {
            setAdd(this!!.toLong(), "=", "" + r)
            return obj
        }

        fun Long?.set(r: Long): T {
            setAdd(this!!, "=", "" + r)
            return obj
        }

        fun Int?.inc(r: Int): T {
            setAdd(this!!.toLong(), "=" + SqlSessionDat.getFieldName(this), "+" + r)
            return obj
        }

        fun Long?.inc(r: Long): T {
            setAdd(this!!, "=" + SqlSessionDat.getFieldName(this.toInt()), "+" + r)
            return obj
        }

        fun Int?.dec(r: Int): T {
            setAdd(this!!.toLong(), "=" + SqlSessionDat.getFieldName(this), "-" + r)
            return obj
        }

        fun Long?.dec(r: Long): T {
            setAdd(this!!, "=" + SqlSessionDat.getFieldName(this.toInt()), "-" + r)
            return obj
        }


        fun Double?.set(r: Double): T {
            setAdd(this!!.toLong(), "=", "" + r)
            return obj
        }

        fun Float?.set(r: Float): T {
            setAdd(this!!.toLong(), "=", "" + r)
            return obj
        }


    }

    /**
     * where,and,or的操作函数
     */
    class SqlOp<T : Any>(
        private val obj: T,
        private val sql: SqlSession<T>,
        val addTableName: Boolean = false
    ) {

        private fun whereAdd(i: Long, op: String, value: String) {
            sql.where += " ";
            sql.where += SqlSessionDat.getFieldName(
                i.toInt(),
                addTableName
            ) + " " + op + " " + value + " "
        }

        fun String?.eq(r: String): SqlOp<T> {
            whereAdd(this!!.toLong(), "=", SqlData.sqlFilter(r))
            return this@SqlOp
        }


        fun String?.like(r: String): SqlOp<T> {
            whereAdd(this!!.toLong(), "like", SqlData.sqlFilter(r))
            return this@SqlOp
        }


        fun Comparable<*>?.`in`(r: List<Comparable<*>>): SqlOp<T> {
            if (this is String)
                this.`in`(r as List<String>)
            else if (this is Long)
                this.`in`(r as List<Long>)
            else if (this is Int)
                this.`in`(r as List<Int>)
            return this@SqlOp
        }


        fun String?.`in`(r: List<String>): SqlOp<T> {
            whereAdd(
                this!!.toLong(),
                "in",
                "(" + r.joinToString(",") { SqlData.sqlFilter(it) } + ")")
            return this@SqlOp
        }

        fun Int?.`in`(r: List<Int>): SqlOp<T> {
            whereAdd(this!!.toLong(), "in", "(" + r.joinToString(",") + ")")
            return this@SqlOp
        }

        fun Long?.`in`(r: List<Long>): SqlOp<T> {
            whereAdd(this!!, "in", "(" + r.joinToString(",") + ")")
            return this@SqlOp
        }

        fun String?.notIn(r: List<String>): SqlOp<T> {
            whereAdd(
                this!!.toLong(),
                "not int",
                "(" + r.joinToString(",") { SqlData.sqlFilter(it) } + ")")
            return this@SqlOp
        }

        fun Int?.notIn(r: List<Int>): SqlOp<T> {
            whereAdd(this!!.toLong(), "not int", "(" + r.joinToString(",") + ")")
            return this@SqlOp
        }

        fun Long?.notIn(r: List<Long>): SqlOp<T> {
            whereAdd(this!!, "not int", "(" + r.joinToString(",") + ")")
            return this@SqlOp
        }

        fun Int?.notEq(r: Int): SqlOp<T> {
            whereAdd(this!!.toLong(), "!=", "" + r)
            return this@SqlOp
        }

        fun Long?.notEq(r: Long): SqlOp<T> {
            whereAdd(this!!, "!=", "" + r)
            return this@SqlOp
        }

        fun Double?.notEq(r: Double): SqlOp<T> {
            whereAdd(this!!.toLong(), "!=", "" + r)
            return this@SqlOp
        }

        fun String?.notEq(r: String): SqlOp<T> {
            whereAdd(this!!.toLong(), "!=", SqlData.sqlFilter(r))
            return this@SqlOp
        }

        fun Int?.eq(r: Int): SqlOp<T> {
            whereAdd(this!!.toLong(), "=", "" + r)
            return this@SqlOp
        }

        fun <R> Comparable<R>?.eqF(r: R): SqlOp<T> {
            sql.where += " ";
            sql.where += SqlSessionDat.getFieldName(("" + this).toInt(), addTableName) +
                    " = " +
                    SqlSessionDat.getFieldName(("" + r).toInt(), addTableName) + " "
            return this@SqlOp
        }

        fun <R> Comparable<R>?.eq(r: R): SqlOp<T> {
            if (this is String)
                whereAdd(this.toLong(), "=", SqlData.sqlFilter(r as String))
            else if (this is Long)
                whereAdd(this.toLong(), "=", "" + r)
            else if (this is Int)
                whereAdd(this.toLong(), "=", "" + r)
            return this@SqlOp
        }

        fun Double?.eq(r: Double): SqlOp<T> {
            whereAdd(this!!.toLong(), "=", "" + r)
            return this@SqlOp
        }

        fun Long?.eq(r: Long): SqlOp<T> {
            whereAdd(this!!, "=", "" + r)
            return this@SqlOp
        }


        fun <R> Comparable<R>?.gr(r: R): SqlOp<T> {
            if (this is String)
                whereAdd(this.toLong(), ">", SqlData.sqlFilter(r as String))
            else if (this is Long)
                whereAdd(this.toLong(), ">", "" + r)
            else if (this is Int)
                whereAdd(this.toLong(), ">", "" + r)
            return this@SqlOp
        }

        fun Int?.gr(r: Int): SqlOp<T> {
            whereAdd(this!!.toLong(), ">", "" + r)
            return this@SqlOp
        }

        fun Double?.gr(r: Double): SqlOp<T> {
            whereAdd(this!!.toLong(), ">", "" + r)
            return this@SqlOp
        }

        fun String?.gr(r: String): SqlOp<T> {
            whereAdd(this!!.toLong(), ">", SqlData.sqlFilter(r))
            return this@SqlOp
        }

        fun Long?.gr(r: Long): SqlOp<T> {
            whereAdd(this!!, ">", "" + r)
            return this@SqlOp
        }

        fun <R> Comparable<R>?.le(r: R): SqlOp<T> {
            if (this is String)
                whereAdd(this.toLong(), "<", SqlData.sqlFilter(r as String))
            else if (this is Long)
                whereAdd(this.toLong(), "<", "" + r)
            else if (this is Int)
                whereAdd(this.toLong(), "<", "" + r)
            return this@SqlOp
        }

        fun Int?.le(r: Int): SqlOp<T> {
            whereAdd(this!!.toLong(), "<", "" + r)
            return this@SqlOp
        }

        fun Double?.le(r: Double): SqlOp<T> {
            whereAdd(this!!.toLong(), "<", "" + r)
            return this@SqlOp
        }

        fun String?.le(r: String): SqlOp<T> {
            whereAdd(this!!.toLong(), "<", SqlData.sqlFilter(r))
            return this@SqlOp
        }

        fun Long?.le(r: Long): SqlOp<T> {
            whereAdd(this!!, "<", "" + r)
            return this@SqlOp
        }


        fun Int?.grEq(r: Int): SqlOp<T> {
            whereAdd(this!!.toLong(), ">=", "" + r)
            return this@SqlOp
        }

        fun Double?.grEq(r: Double): SqlOp<T> {
            whereAdd(this!!.toLong(), ">=", "" + r)
            return this@SqlOp
        }

        fun String?.grEq(r: String): SqlOp<T> {
            whereAdd(this!!.toLong(), ">=", SqlData.sqlFilter(r))
            return this@SqlOp
        }

        fun Long?.grEq(r: Long): SqlOp<T> {
            whereAdd(this!!, ">=", "" + r)
            return this@SqlOp
        }

        fun Int?.leEq(r: Int): SqlOp<T> {
            whereAdd(this!!.toLong(), "<=", "" + r)
            return this@SqlOp
        }

        fun Double?.leEq(r: Double): SqlOp<T> {
            whereAdd(this!!.toLong(), "<=", "" + r)
            return this@SqlOp
        }

        fun String?.leEq(r: String): SqlOp<T> {
            whereAdd(this!!.toLong(), "<=", SqlData.sqlFilter(r))
            return this@SqlOp
        }

        fun Long?.leEq(r: Long): SqlOp<T> {
            whereAdd(this!!, "<=", "" + r)
            return this@SqlOp
        }


        val and: T
            get() {
                sql.where += " and "
                return obj;
            }

        fun and(func: SqlOp<T>.(res: T) -> SqlOp<T>): SqlOp<T> {
            sql.where += " and ("
            this.func(obj)
            sql.where += " ) "
            return this;
        }

        val or: T
            get() {
                sql.where += " or "
                return obj;
            }

        fun or(func: SqlOp<T>.(res: T) -> SqlOp<T>): SqlOp<T> {
            sql.where += " or ("
            this.func(obj)
            sql.where += " ) "
            return this;
        }

    }


    //////////////////////SqlSession end

}
