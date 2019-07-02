package rxaa.df

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.Configuration
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.*
import java.util.concurrent.ExecutorService

@SuppressLint("StaticFieldLeak")
object df {
    /**
     * 全局context
     */
    @JvmStatic
    var appContext: Context? = null;
    /**
     * 当前顶层activity
     */
    @JvmStatic
    var currentActivity: Activity? = null;
    /**
     * 屏幕密度
     */
    @JvmStatic
    var density = 1f

    @JvmStatic
    val actStack = ArrayList<Activity>();


    fun launch(block: suspend () -> Unit) {
        GlobalScope.launch(Dispatchers.Unconfined) {
            catchLog {
                block();
            }
        }
    }


    @JvmStatic
    fun regApp(app: Application) {
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {

            override fun onActivityStopped(activity: Activity) {}

            override fun onActivityStarted(activity: Activity) {}

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle?) {}

            override fun onActivityResumed(activity: Activity) {}

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityDestroyed(activity: Activity) {
                for (i in df.actStack.size - 1 downTo 0) {
                    if (df.actStack[i] === activity) {
                        df.actStack.removeAt(i)
                        break
                    }

                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                df.actStack.add(activity)
            }
        })
    }

    /**
     * 初始化appContext，在Application onCreate 调用
     */
    @JvmStatic
    fun init(cont: Context) {
        appContext = cont.applicationContext
        density = cont.resources.displayMetrics.density
    }

    @JvmStatic
    inline fun <reified T : Any> copy(obj: T): T {
        val t = T::class.java.newInstance();
        df.getClassFields(T::class.java) { filed, i ->
            filed.set(t, filed.get(obj));
        }
        return t;
    }


    inline fun joinStr(size: Int, op: String, func: (i: Int) -> String): String {
        val sb = StringBuilder();
        for (i in 0..size - 1) {
            sb.append(func(i))
            sb.append(op)
        }
        if (sb.isNotEmpty())
            sb.setLength(sb.length - op.length)

        return sb.toString();
    }

    /**
     * 捕获未处理异常
     */
    @JvmStatic
    fun uncaughtExceptionLog(msg: Boolean = true) {
        Thread.setDefaultUncaughtExceptionHandler({ thread, throwable ->

            df.logException(throwable, msg)

            val lo = Looper.myLooper()
            //// 处于主线程中,只能干掉当前的程序
            if (lo != null && lo == Looper.getMainLooper()) {
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        })

    }

    @JvmStatic
    fun findView(v: View?, id: Int): View? {
        return v?.findViewById(id)
    }

    @JvmStatic
    fun findView(v: Activity?, id: Int): View? {
        return v?.findViewById(id)
    }

    @JvmStatic
    fun findView(v: Dialog?, id: Int): View? {
        return v?.findViewById(id)
    }


    @JvmStatic
    fun runOnPool(pool: ExecutorService, func: () -> Unit) {
        pool.execute({
            df.catchLog { func() }
        })
    }


    val handl = Handler(Looper.getMainLooper());

    /**
     * 在主线程中运行
     */
    @JvmStatic
    fun runOnUi(func: () -> Unit): Runnable {
        val run = Runnable {
            df.catchLog { func() }
        }
        handl.post(run)
        return run;
    }

    /**
     * 在主线程中延时(毫秒)运行
     */
    @JvmStatic
    fun runOnUi(time: Long, func: () -> Unit): Runnable {
        val run = Runnable {
            df.catchLog { func() }
        }
        handl.postDelayed(run, time);
        return run;
    }

    /**
     * 移除runOnUi设置的延时函数
     */
    @JvmStatic
    fun removeOnUi(func: Runnable?) {
        handl.removeCallbacks(func);
    }


    /**
     * 捕获所有异常加入日志,并弹窗
     */
    @JvmStatic
    inline fun catchLog(func: () -> Unit) {
        try {
            func()
        } catch (e: Throwable) {
            logException(e)
        }
    }


    @JvmStatic
    inline fun catchLogNoMsg(func: () -> Unit) {
        try {
            func()
        } catch (e: Throwable) {
            logException(e, false)
        }
    }

    /**
     * 弹出toast消息
     */
    @JvmStatic
    fun msg(str: String?) {
        msg(str, false)
    }

    /**
     * 弹出toast消息
     */
    @JvmStatic
    fun msg(str: String?, longTime: Boolean) {
        if (df.appContext == null)
            return

        val lo = Looper.myLooper()
        if (lo == null || lo != Looper.getMainLooper()) {
            Handler(Looper.getMainLooper()).post {
                if (longTime)
                    Toast.makeText(df.appContext, str + "", Toast.LENGTH_LONG).show()
                else
                    Toast.makeText(df.appContext, str + "", Toast.LENGTH_SHORT).show()
            }
        } else {
            if (longTime)
                Toast.makeText(df.appContext, str + "", Toast.LENGTH_LONG).show()
            else
                Toast.makeText(df.appContext, str + "", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 获取app内部目录
     */
    @JvmStatic
    fun getInnerFileDir(): File {
        return appContext!!.filesDir
    }

    /**
     * 获取app的默认目录
     */
    @JvmStatic
    fun getFileDir(): File {
        try {
            val file = appContext?.getExternalFilesDir(null)
            if (file != null && file.exists())
                return file;

            val f2 = appContext?.filesDir
            if (f2 != null)
                return f2;

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return getInnerFileDir();
    }

    @JvmStatic
    fun getExternalDir(): File {
        try {
            val sdCardExist = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED   //判断sd卡是否存在
            if (sdCardExist) {
                return Environment.getExternalStorageDirectory()//获取跟目录
            }
        } catch (e: Exception) {

        }

        return getFileDir()
    }

    /**
     * 遍历指定类型的所有字段
     */
    @JvmStatic
    @JvmOverloads
    inline fun <T> getClassFields(clas: Class<T>, hasFinal: Boolean = false, func: (field: Field, i: Int) -> Unit) {
        var i = -1;
        for (f in clas.declaredFields) {
            if (Modifier.isStatic(f.modifiers))
                continue

            if (Modifier.isFinal(f.modifiers) && !hasFinal)
                continue;

            f.isAccessible = true
            i++;
            func(f, i);
        }
    }


    @JvmStatic
    fun getCacheDir(): File {
        try {
            val file = appContext?.externalCacheDir
            if (file != null && file.exists())
                return file;

            val f2 = appContext?.cacheDir
            if (f2 != null)
                return f2;

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return File("/sdcard/")
    }

    @JvmStatic
    fun createDir(menu: File): File {
        if (!menu.exists())
            menu.mkdirs()
        return menu
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    @JvmStatic
    fun dp2px(dpValue: Float): Int {

        return (dpValue * density + 0.5f).toInt()
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    @JvmStatic
    fun px2dp(dpValue: Float): Float {

        return (dpValue - 0.5f) / density
    }

    /**
     * 获取调用栈信息
     */
    @JvmStatic
    fun getStackTraceInfo(arg1: Throwable?): String {
        if (arg1 == null)
            return ""
        try {
            val ele = arg1.stackTrace

            var res = "\r\n#####" + arg1.toString() + "#####:"

            for (e in ele) {
                if (e.className.indexOf("java.") == 0 || e.className.indexOf("android.") == 0)
                    continue
                res += "\r\n" + e.toString()
            }

            val sub = arg1.cause
            if (sub !== arg1 && sub != null)
                res += getStackTraceInfo(sub)

            return res
        } catch (e: Exception) {
            e.printStackTrace()
            return "";
        }
    }

    @JvmStatic
    fun getLogFile(): File {
        return getFileDir() + "/err.log";
    }

    @JvmStatic
    val now: String
        get() {
            val c = Calendar.getInstance()
            return c.getString();
        }


    @JvmStatic
    fun calendar(): Calendar {
        return Calendar.getInstance()
    }

    @JvmStatic
    fun calendar(millis: Long): Calendar {
        val c = Calendar.getInstance()
        c.timeInMillis = millis;
        return c
    }


    /**
     * 写日志函数
     */
    @JvmStatic
    var writeLogFunc = fun(text: String, file: File): Boolean {
        try {
            if (file.length() > 2 * 1024 * 1024)
                file.delete()

            file.appendText("------$now------\r\n$text\r\n\r\n")

        } catch (ex: Exception) {
            ex.printStackTrace()
            return false
        }

        return true
    };

    /**
     * 向file目录写日志
     */
    @JvmStatic
    @JvmOverloads
    fun writeLog(text: String, file: File = getLogFile()): Boolean {
        return writeLogFunc(text, file)
    }

    /**
     * 弹出对话框
     */
    @JvmStatic
    @JvmOverloads
    fun msgDialog(cont: String?, title: String? = "消息", onOk: () -> Unit = {}) {

        df.actStack.lastItem {
            val run = {
                val bu = AlertDialog.Builder(df.currentActivity)
                bu.setCancelable(false)
                if (title != null)
                    bu.setTitle(title)
                if (cont != null)
                    bu.setMessage(cont)

                val alert = bu.setPositiveButton("确定", DialogInterface.OnClickListener { dialogInterface, i ->
                    df.catchLog { onOk() }
                }).create()
                alert.show()
            }

            val lo = Looper.myLooper()
            if (lo == null || lo != Looper.getMainLooper()) {
                Handler(Looper.getMainLooper()).post(run)
            } else
                run()
            return
        }
        msg(cont)
    }

    /**
     * 将异常写入日志
     */
    @JvmStatic
    @JvmOverloads
    fun logException(arg1: Throwable, msgDialog: Boolean = true, msg: String = "") {
        Log.e("wwwwwwwwwwwwww" + msg, "error", arg1)
        df.writeLog(msg + "--------\r\n" + getStackTraceInfo(arg1))
        if (msgDialog)
            msgDialog(arg1.message)
    }

    @JvmStatic
    fun createView(id: Int): View {
        val factory = LayoutInflater.from(appContext)
        return factory.inflate(id, null)
    }

    @JvmStatic
    fun createView(cont: Context, id: Int): View {
        val factory = LayoutInflater.from(cont)
        return factory.inflate(id, null, false)
    }

    @JvmStatic
    fun createView(cont: Context, parent: ViewGroup?, id: Int): View {
        val factory = LayoutInflater.from(cont)
        return factory.inflate(id, parent, false)
    }


    @JvmStatic
    fun getID(): Long {
        return System.currentTimeMillis().shl(16).plus((Math.random() * 65535).toLong())
    }

    /**
     * 打开输入法
     */
    @JvmStatic
    fun imeOpen(vi: View) {
        val imm = df.appContext!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        if (df.appContext!!.resources.configuration.keyboard == Configuration.KEYBOARD_NOKEYS)
            imm.showSoftInput(vi, 0)
    }

    /**
     * 判断输入法是否打开
     */
    @JvmStatic
    fun imeIsOpen(): Boolean {
        val imm = df.appContext!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return imm.isActive
    }

    /**
     * 关闭输入法
     */
    @JvmStatic
    fun imeClose(vi: View) {
        val imm = df.appContext!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(vi.windowToken, 0)
    }

    @JvmStatic
    fun imeClose() {
        val imm = df.appContext!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(null, 0)
    }


    @JvmStatic
    fun setOnClick(vi: View, click: Func0) {
        vi.setOnClickListener { v ->
            df.catchLog { click.run() }
        }
    }
}