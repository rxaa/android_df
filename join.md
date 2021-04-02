#多表连接查询

假设我们有两个表A与B为如下结构:

```kotlin
class A {
    @PrimaryKey
    @Autoincrement
    var id = 0;

    var filed1 = "";

    var field2 = "";

}

class B {
    @PrimaryKey
    @Autoincrement
    var id = 0;

    var filed1 = "";

    var field3 = "";
}

object DB {
    val conn by lazy {
        //参数为数据库路径,与版本号
        SqliteConnect("/sdcard/test.db", 1).open()
    }


    fun a() = SqlSession(A::class.java, conn)
    fun b() = SqlSession(B::class.java, conn)
}
```

将这两个表相连,首先需要创建一个新类:

```kotlin
class AB {
    @PrimaryKey
    @FieldTable(A::class) //与B表字段重复,需额外指定所属表名
    var id = 0;

    @FieldTable(A::class)//与B表字段重复,需额外指定所属表名
    var filed1 = "";

    var field2 = "";

    //与A表字段重复,需额外指定所属表名与字段名,并且使用别名b_id
    @FieldTable(B::class)
    @FieldName("id")
    var b_id = 0;

    //同上
    @FieldTable(B::class)
    @FieldName("filed1")
    var b_filed1 = "";

    var field3 = "";
}
```
联表最麻烦的就是要处理各种重名问题,需要通过类字段注解来说明清楚重名的字段.

然后就是写一条联表规则:

```kotlin
object DB {

	......

    fun a() = SqlSession(A::class.java, conn)
    fun b() = SqlSession(B::class.java, conn)

    //将a与b进行inner join链接,条件是 on a.id=b.id
    val joinAB = DB.a().innerJoin(B::class.java) { a, b -> a.id.eqF(b.id) }//注意是eqF不是eq

    //将innerJoin的返回值传给SqlSession的第三个参数
    fun a_b() = SqlSession(AB::class.java, conn, joinAB)
}
```

之后就可以像操作A和B一样,通过DB.a_b()来使用这个联表.

更多表的链接和上面类似,在innerJoin函数后面继续join.


```kotlin
class C {
    @PrimaryKey
    @Autoincrement
    var id = 0;

    var filed1 = "";

}

object DB {

	......

    //a inner join b on a.id=b.id left join c on a.id=c.id
    val joinABC = DB.a()
            .innerJoin(B::class.java) { a, b -> a.id.eqF(b.id) }
            .leftJoin(C::class.java) { a, b, c -> a.id.eqF(c.id) }

    fun a_b_c() = SqlSession(ABC::class.java, conn, joinABC)
}

```