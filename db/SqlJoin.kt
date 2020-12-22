package net.rxaa.db

interface ISqlJoin {

    val session: SqlSession<*>;


}

class SqlJoin3<T1 : Any, T2 : Any, T3 : Any>(override val session: SqlSession<T1>, val session2: SqlSession<T2>, val session3: SqlSession<T3>) : ISqlJoin {

}

class SqlJoin2<T1 : Any, T2 : Any>(override val session: SqlSession<T1>, val session2: SqlSession<T2>) : ISqlJoin {
    /**
     *  连表查询
     * @param table3 join的表名
     * @param func on 条件表达式
     * @returns {SqlBuilder}
     */
    private fun <T3 : Any> makeJoin(
            table3: Class<T3>,
            func: SqlSession.SqlOp<T1>.(a1: T1, a2: T2, a3: T3) -> Unit,
            joinStr: String): SqlJoin3<T1, T2, T3> {
        if (session.where.isEmpty()) {
            session.where = session.tableName;
        }
        val right = SqlSession(table3, session.connect);
        val op = SqlSession.SqlOp(session.tableData.classInst, session, true)
        session.where += joinStr + right.tableName + " on "
        op.func(session.tableData.classInst, session2.tableData.classInst, right.tableData.classInst);
        return SqlJoin3(session, session2, right);
    }

    /**
     *  内连表查询
     * @param right join的表名
     * @param func on 条件表达式
     * @returns {SqlBuilder}
     */
    fun <T3 : Any> innerJoin(right: Class<T3>, func: SqlSession.SqlOp<T1>.(a1: T1, a2: T2, a3: T3) -> Unit): SqlJoin3<T1, T2, T3> {
        return this.makeJoin(right, func, " inner join ");
    }

    /**
     *  左连表查询
     * @param right join的表名
     * @param func on 条件表达式
     * @returns {SqlBuilder}
     */
    fun <T3 : Any> leftJoin(right: Class<T3>, func: SqlSession.SqlOp<T1>.(a1: T1, a2: T2, a3: T3) -> Unit): SqlJoin3<T1, T2, T3> {
        return this.makeJoin(right, func, " left join ");
    }

    /**
     *  右连表查询
     * @param right join的表名
     * @param func on 条件表达式
     * @returns {SqlBuilder}
     */
    fun <T3 : Any> rightJoin(right: Class<T3>, func: SqlSession.SqlOp<T1>.(a1: T1, a2: T2, a3: T3) -> Unit): SqlJoin3<T1, T2, T3> {
        return this.makeJoin(right, func, " right join ");
    }
}
