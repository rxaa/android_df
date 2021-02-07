package net.rxaa.db

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import net.rxaa.ext.FileExt
import net.rxaa.ext.plus
import net.rxaa.util.df
import java.lang.reflect.Field
import java.util.*

class SqliteConnect(
    var dbName: String,
    var version: Int,
    var onUpgrade: SqliteConnect.() -> Unit = {}
) {
    var db: SQLiteDatabase? = null
    var helper: Helper? = null
    var oldVersion = 0;
    var dbLog = true;

    /**
     * 重新打开连接
     * @param name
     */
    @Synchronized
    fun open(): SqliteConnect {
        if (db != null && db!!.isOpen)
            return this
        close()
        reInit()
        return this
    }

    /**
     * 打开现有的链接
     */
    @Synchronized
    fun open(datab: SQLiteDatabase): SqliteConnect {
        db = datab;
        dbName = datab.path;
        version = datab.version;

        return this
    }


    fun close() {
        try {
            if (db != null)
                db!!.releaseReference();
        } catch (e: Exception) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
    }

    /**
     * 初始化数据库,并当需要时触发升级函数
     */
    @Synchronized
    fun reInit() {
        helper = Helper(dbName, version, this)
        db = helper!!.writableDatabase
        if (helper!!.needUpgrade) {
            FileExt.catchLog { onUpgrade(this) }
        }
    }

    /**
     * 更新数据库

     * @param sql  语句,参数可用?号占位
     * *
     * @param objs 参数表
     * *
     * @throws Exception
     */
    @Throws(Exception::class)
    fun update(sql: String, vararg objs: Any) {
        val t = System.currentTimeMillis();
        try {
            db!!.execSQL(sql, objs)
        } catch (e: Exception) {
            logFunc(sql + "\r\n" + e, true);
            throw e
        }
        val t2 = System.currentTimeMillis() - t;
        logFunc("Update ${t2}ms:\r\n" + sql, false);
    }

    var logFunc = fun(sql: String, err: Boolean) {
        if (dbLog) {
            if (err)
                FileExt.writeLog(sql, FileExt.getFileDir() + "/db/sqlError.txt")
            else
                FileExt.writeLog(sql, FileExt.getFileDir() + "/db/sql.txt")
        }

    }

    /**
     * 获取所有Cursor,
     * @param sql
     * *
     * @param objs
     * *
     * @return
     * *
     * @throws Exception
     */
    @Throws(Exception::class)
    inline fun query(sql: String, vararg objs: String, res: (c: Cursor) -> Unit) {
        val t = System.currentTimeMillis();
        try {
            val cur = db!!.rawQuery(sql, objs)
            try {
                while (cur.moveToNext()) {
                    res(cur)
                }
            } finally {
                cur.close()
            }
        } catch (e: Exception) {
            logFunc(sql + "\r\n" + e, true);
            throw e
        }

        val t2 = System.currentTimeMillis() - t;
        logFunc("Query ${t2}ms:\r\n" + sql, false);
    }


    /**
     * 判断表是否存在
     * @param table
     * *
     * @return
     */
    fun tableExist(table: String): Boolean {
        return getOneLong("SELECT COUNT(*) FROM sqlite_master where type='table' and name='$table'") > 0
    }

    /**
     * 判断表是否存在
     */
    fun tableExist(fields: Class<*>): Boolean {
        return getOneLong(
            "SELECT COUNT(*) FROM sqlite_master where type='table' and name='"
                    + SqlData.getTableName(fields, false) + "'"
        ) > 0
    }

    /**
     * 获取第一行第一列的值

     * @param sql
     * *
     * @param objs
     * *
     * @return
     */
    @Throws(Exception::class)
    fun getOneLong(sql: String, default: Long = 0): Long {
        query(sql) {
            if (it.count > 0)
                return it.getLong(0)
        }
        return default
    }

    /**
     * 获取第一行第一列的值

     * @param sql
     * *
     * @param objs
     * *
     * @return
     */
    @Throws(Exception::class)
    fun getOneString(sql: String, default: String = ""): String {
        query(sql) {
            if (it.count > 0)
                return it.getString(0)
        }
        return default
    }

    /**
     * 创建表
     * *
     * @param fields    字段model
     * *
     * @param drop      是否drop旧表
     * *
     * @throws Exception
     */
    @Synchronized
    @Throws(Exception::class)
    fun createTable(fields: Class<*>, drop: Boolean, name: String = "") {

        var tableName = name;
        if (name.length == 0)
            tableName = SqlData.getTableName(fields);

        if (drop)
            db!!.execSQL("DROP TABLE IF EXISTS $tableName;")

        val sqlStr = StringBuilder("CREATE TABLE IF NOT EXISTS $tableName (")

        val indexs = ArrayList<String>()

        df.getClassFields(fields) { f, _ ->
            val fName = SqlData.getFieldName(f, false)
            sqlStr.append("`$fName`" + getFieldType(f) + getFieldInfo(f) + ",")

            if (f.getAnnotation(SqlIndex::class.java) != null) {
                indexs.add(
                    "CREATE INDEX  IF NOT EXISTS  " + tableName.replace("`", "") + "_i_"
                            + fName + "  ON $tableName (`$fName` ASC);"
                )
            }
        }

        sqlStr.setLength(sqlStr.length - 1)
        sqlStr.append(");")

        db!!.execSQL(sqlStr.toString())
        for (index in indexs) {
            FileExt.catchLog { db!!.execSQL(index) }
        }
    }


    companion object {
        /**
         * 从字段注解中获取表字段信息

         * @param fields
         * *
         * @return
         */
        private fun getFieldInfo(fields: Field): String {
            var info = ""
            if (fields.getAnnotation(PrimaryKey::class.java) != null)
                info += "  PRIMARY KEY "

            if (fields.getAnnotation(Autoincrement::class.java) != null)
                info += " AUTOINCREMENT "

            if (fields.getAnnotation(SqlNotNull::class.java) != null)
                info += " NOT NULL "

            if (fields.getAnnotation(SqlUnique::class.java) != null)
                info += "  UNIQUE ON CONFLICT REPLACE "

            val def = fields.getAnnotation(SqlDefault::class.java)

            if (def != null) {
                info += " DEFAULT '" + def.value + "'"
            }

            return info
        }

        /**
         * 生成表字段的类型

         * @param f
         * *
         * @return
         */
        fun getFieldType(f: Field): String {
            if (f.type == Integer::class.java || f.type == java.lang.Long::class.java
                || f.type == Int::class.java || f.type == Long::class.java
            ) {
                return " INTEGER "
            } else if (f.type == java.lang.Float::class.java || f.type == java.lang.Double::class.java
                || f.type == Float::class.java || f.type == Double::class.java
            ) {
                return " REAL "
            }

            return " TEXT "
        }
    }

    class Helper(
        name: String,
        version: Int,
        var sqlite: SqliteConnect
    )// TODO Auto-generated constructor stub
        : SQLiteOpenHelper(df.appContext, name, null, version) {

        var needUpgrade = false

        // 数据库第一次被创建时onCreate会被调用
        override fun onCreate(db: SQLiteDatabase) {
            needUpgrade = true
        }

        // 如果DATABASE_VERSION值被改为2,系统发现现有数据库版本不同,即会调用onUpgrade
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // db.execSQL("ALTER TABLE person ADD COLUMN other STRING");
            needUpgrade = true
            sqlite.oldVersion = oldVersion;
        }
    }
}