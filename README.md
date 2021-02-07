#android上sqlite的orm

基于kotlin开发,利用各种高级lambda特性实现了类似LINQ的强类型,带智能感知的sql语句书写,且要比LINQ的抽象语法树解析高效的多.

## 用法: ##

**1.首先要配置kotlin,这个就不多说了.**

在Application的onCreate()里初始化：

```java
df.init(this);
```


**2.创建一个数据库链接**

```kotlin
object DB {
    /**
     * 本地数据库版本号
     */
    val dbVersion = 1

    //创建数据库链接实例,我们通过by lazy让它延迟加载(首次调用时再加载)
    val conn by lazy {
        //参数为数据库路径,与版本号
        SqliteConnect("/sdcard/test.db", dbVersion).open()
    }
    
}
```
以上我们通过SqliteConnect的构造函数创建了数据库链接,赋值给了conn,接下来就要创建一张表了.我们可以看到SqliteConnect对象里有个createTable函数,用它来建表,我们需要传递一个定义了表各个字段的class.

**3.在数据库中创建表**
首先来建一个表实体model类
```kotlin
class TestTable {

    //这张表的主键id(integer类型)
    @PrimaryKey //注解为主键
    @Autoincrement //自增长
    var id = 0;

    //一个时间戳字段
    @SqlIndex //加上索引
    var time = System.currentTimeMillis()

    //一个标题字段(text类型)
    var title = ""
}
```
我们可以看到上面这个model非常的简单,不用继承任何其他任何类,只用注解就完成了字段描述.

然后创建这个表:

```kotlin
 //直接把表的类名传给createTable函数就完成了表的创建,第二个参数传了true表示带上DROP TABLE IF EXISTS这个条件
    DB.conn.createTable(TestTable::class.java, true);

    //可以在第三个参数给表起一个名字"table1",不传则自动用TestTable这个类名做表名
    DB.conn.createTable(TestTable::class.java, false, "table1");
```
有一点需要提的是字段中的类型分别对应的sqlite中的类型:Int,Long,Integer对应了INTEGER类型,Float,Double,对应了REAL类型,其他则对应Text类型.

其他的字段注解还有：

```kotlin
//指定字段名
@FieldName("")

//指定表名
@TableName("")

//忽略的字段
@SqlIgnore

//唯一索引
@SqlUnique

//非空字段
@SqlNotNull

//指定缺省值
@SqlDefault("")

//字段为嵌套对象，以JSON格式储存
@SqlJSON
```

**4.增删改查**

SqliteConnect对象里带一个query函数支持写原生sql语句,暂不多说,我们主要关注的是强类型且带智能感知的sql语句写法.

为此我们要引入SqlSession这个类.其构造函数中有两个参数,分别要传递:表model类与数据库链接对象.

每次书写sql语句都要先构造一个SqlSession对象,为了方便使用我们把构造方法给封装一下:
```kotlin
object DB {
    /**
     * 本地数据库版本号
     */
    val dbVersion = 1

    //创建数据库链接实例,我们通过by lazy让它延迟(首次调用时再加载)
    val conn by lazy {
        //参数为数据库路径,与版本号
        SqliteConnect("/sdcard/test.db", dbVersion).open()
    }

     //此函数关联了TestTable这个表,与conn这个链接实例
    fun testTable() = SqlSession(TestTable::class.java, conn)
}
```

我们给DB里多封装了一个testTable函数,以后每次要写TestTable这个表的sql语句时都可以调用这个函数.

首先是insert语句:
```kotlin
    val t = TestTable();
    t.title = "一个标题"

    DB.testTable().insert(t)
```
就是这么简单,基本没什么好说的,调用了SqlSession的insert函数,默认情况下insert会忽略掉注解了Autoincrement字段的值,如果想手动指定id需要在insert第二个参数传false:
```kotlin
    val t2 = TestTable();
    t2.id = 100;//手动指定一个id
    DB.testTable().insert(t2, false)
```

**where查询语句**:
```kotlin
    //利用了.号连贯操作(生成了这样的sql语句:select * from testTable where title='标题')
    val list = DB.testTable().where { it.title.eq("标题") }.toArray();
```
这个就稍微有点复杂了需要解释下:where的参数接收了一个lambda函数,lambda的第一个参数其实是一个TestTable类的实例,所以当我们打it.时会自动感知出TestTable的所有字段成员.

所有的sql操作符都被转换为了orm中的函数调用,例如:eq对应=,le对应<,gr对应>,leEq对应<=,grEq对应>=,in和like之类的不变

最后我们调用toArray()函数将结果生成了一组ArrayList\<TestTable\>返回.

再看一些复杂的查询例子:
```kotlin
    //带上and和or:where id>1 and id<10 or title=''
    DB.testTable().where { it.id.gr(1).and.id.le(10).or.title.eq("") }.toArray();

    //where里加上括号嵌套: where id>1 and (id<10 or time=0) order by id asc,time desc limit 1,2
    DB.testTable().where { it.id.gr(1).and { it.id.le(10).or.time.eq(0) } }.order { it.id.asc.time.desc }.limit(1, 2).toArray();

    //分段组合sql语句
    val s = DB.testTable()
    s.where { it.id.gr(1).and { it.id.le(10).or.time.eq(0) } }
    s.and { it.id.eq(1) };
    s.or { it.title.`in`(arrayListOf("标题1", "标题2")) }//in是系统关键字所以要加上``号
    //最终结果:where id>1 and (id<10 or time=0) and id=1 or title in ('标题1', '标题2')
    val list2 = s.toArray();
```

**删除**,没什好说的就是把上面的where语句后面的toArray()函数换成delete();

**更新**

在以上的where条件之后带上update函数:
```kotlin
    //update testTable set title='标题2',time=time+1 where id=1
    DB.testTable().where { it.id.eq(1) }.update { it.title.set("标题2").time.inc(1) }
```

orm中已经对所有参数进行了注入过滤.

增删改查中出现错误会抛出异常.

**5.数据库升级**

SqliteConnect构造函数的第三个参数为升级回调函数,会在第一次数据库创建,和每次dbVersion版本号增加时调用:
```kotlin
val conn by lazy {
        //参数为数据库路径,与版本号
        SqliteConnect("/sdcard/test.db", dbVersion) {
            //为了简单起见,这里每次升级都清空数据重新创建表
            createTable(TestTable::class.java, true)

            //也可以通过判断oldVersion版本号来手动升级(oldVersion为0是表示数据库第一次创建)
            if (oldVersion > 0 && oldVersion < 10) {

            }
        }.open()
    }
```

**6.多表链接查询:**
[多表连接查询](join.md)