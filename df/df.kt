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
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

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


    /**
     *  在当前线程启动协程
     */
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
    fun runOnPool(pool: ExecutorService, func: suspend () -> Unit) {
        pool.execute({
            df.launch { func() }
        })
    }


    val handl = Handler(Looper.getMainLooper());

    /**
     * 切换到主线程
     */
    suspend fun <T> runToUI(func: suspend () -> T) = suspendCoroutine<T> { conti ->
        handl.post(Runnable {
            df.launch {
                try {
                    conti.resume(func())
                } catch (e: Exception) {
                    conti.resumeWithException(e)
                }
            }
        })
    }

    /**
     * 切换到线程池
     */
    suspend fun runToPool(pool: ExecutorService) = suspendCoroutine<Unit> { conti ->
        pool.execute({
            df.catchLog {
                conti.resume(Unit)
            }
        })
    }

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

    @JvmStatic
    fun runOnUiCheck(func: () -> Unit): Runnable {
        val run = Runnable {
            df.catchLog { func() }
        }

        val lo = Looper.myLooper()
        //检测是否已经在主线程
        if (lo == null || lo != Looper.getMainLooper()) {
            handl.post(run)
        } else {
            run.run()
        }
        return run;
    }


    suspend fun delay(time: Long) = suspendCoroutine<Unit> {
        val run = Runnable {
            df.catchLog { it.resume(Unit) }
        }
        handl.postDelayed(run, time);
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
            val sdCardExist =
                Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED   //判断sd卡是否存在
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
    inline fun <T> getClassFields(
        clas: Class<T>,
        hasFinal: Boolean = false,
        func: (field: Field, i: Int) -> Unit
    ) {
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
            if (file.length() > 2 * 1024 * 1024) {
                file.delete()
            }

            file.appendText("------$now------\r\n$text\r\n\r\n")

        } catch (ex: Exception) {
            ex.printStackTrace()
            return false
        }

        return true
    };


    fun swapData(datas: List<*>, fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(datas, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(datas, i, i - 1)
            }
        }
    }

    /**
     * 向file目录写日志
     */
    @JvmStatic
    @JvmOverloads
    fun writeLog(text: String, file: File = getLogFile()): Boolean {
        return writeLogFunc(text, file)
    }

    fun numberFix2(num: Long): String {
        if (num < 10)
            return "0" + num;
        return "" + num + "";
    }

    fun timeToStr2(mss: Long, trimMilli: Boolean = true): String {
        val days = mss / (1000 * 60 * 60 * 24);
        val hours = (mss % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
        val minutes = (mss % (1000 * 60 * 60)) / (1000 * 60);
        val seconds = (mss % (1000 * 60)) / 1000;

        var ret = "";
        if (hours > 0)
            ret += "" + hours + ":";
        ret += numberFix2(minutes) + ":";

        ret += numberFix2(seconds) + "";

        if (!trimMilli) {
            val mill = (mss % (1000L));
            ret += "." + numberFix2(mill);
        }

        return ret;
    }

    var getItem: (key: String) -> String? = {
        null
    }
    var setItem: (key: String, value: String) -> Unit = { k, v ->

    }

    fun timeStrToLong(time: String): Long {
        try {
            if (time.length < 1)
                return 0;
            var sec = time.split('.');
            if (sec.size < 1)
                return 0;

            var millsec = 0L;
            if (sec.size == 2) {
                if (sec[1].length > 3)
                    millsec = sec[1].substring(0, 3).toLong();
                else if (sec[0].length == 2)
                    millsec = sec[1].toLong() * 10;
                else if (sec[0].length == 1)
                    millsec = sec[1].toLong() * 100;
            }

            var hour = sec[0].split(':');
            if (hour.size < 1)
                return millsec;

            if (hour.size > 2) {
                millsec += (hour[hour.size - 3]).toLong() * 60 * 60 * 1000;
            }

            if (hour.size > 1) {
                millsec += (hour[hour.size - 2]).toLong() * 60 * 1000;
            }

            if (hour.size > 0) {
                millsec += (hour[hour.size - 1]).toLong() * 1000;
            }
            return millsec;
        } catch (e: Exception) {
            return 0;
        }
    }

    /**
     * 弹出对话框
     */
    @JvmStatic
    @JvmOverloads
    fun msgDialog(cont: String?, title: String? = "", onOk: () -> Unit = {}) {

        df.actStack.lastItem { act ->
            val run = {
                val bu = AlertDialog.Builder(act)
                bu.setCancelable(false)
                title.notEmpty {
                    bu.setTitle(title)
                }

                if (cont != null)
                    bu.setMessage(cont)

                val alert = bu.setPositiveButton(
                    dfStr.ok,
                    DialogInterface.OnClickListener { dialogInterface, i ->
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
     * 弹出对话框
     */
    suspend fun msgDialogAwait(cont: String?, title: String? = "") =
        suspendCoroutine<Boolean> { conti ->
            df.actStack.lastItem { act ->
                df.runOnUiCheck {
                    val bu = AlertDialog.Builder(act)
                    bu.setCancelable(false)
                    title.notEmpty {
                        bu.setTitle(title)
                    }

                    if (cont != null)
                        bu.setMessage(cont)

                    val alert = bu.setPositiveButton(
                        dfStr.ok,
                        DialogInterface.OnClickListener { dialogInterface, i ->
                            df.catchLog { conti.resume(true) }
                        }).setNegativeButton(
                        dfStr.cancel,
                        DialogInterface.OnClickListener { dialogInterface, i ->
                            df.catchLog { conti.resume(false) }
                        })
                        .create()
                    alert.show()
                }
            }.isNull {
                msg(cont)
                conti.resume(false)
            }
        }

    /**
     * 将异常写入日志
     */
    @JvmStatic
    @JvmOverloads
    fun logException(arg1: Throwable, msgDialog: Boolean = true, msg: String = "") {
        Log.e("wwwwwwwwwwwwww" + msg, "error", arg1)
        df.writeLog(msg + "--------\r\n" + getStackTraceInfo(arg1))
        if (msgDialog) {
            if (arg1 is MsgException) {
                if (arg1.showAble)
                    df.msg(arg1.message)
            } else {
                msgDialog(arg1.message, dfStr.error)
            }

        }
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


    val randStr = "0123456789abcdefghijklmnopqrstuvwxyz"

    fun getRandStr(len: Int): String {
        var ret = "";
        for (i in 0 until len) {
            ret += randStr[(Math.random() * randStr.length).toInt()]
        }
        return ret
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
        val imm =
            df.appContext!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        vi.setFocusable(true);
        vi.requestFocus();
        if (df.appContext!!.resources.configuration.keyboard == Configuration.KEYBOARD_NOKEYS)
            imm.showSoftInput(vi, 0)
    }

    /**
     * 判断输入法是否打开
     */
    @JvmStatic
    fun imeIsOpen(): Boolean {
        val imm =
            df.appContext!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return imm.isActive
    }

    /**
     * 关闭输入法
     */
    @JvmStatic
    fun imeClose(vi: View) {
        val imm =
            df.appContext!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(vi.windowToken, 0)
    }

    @JvmStatic
    fun imeClose() {
        val imm =
            df.appContext!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(null, 0)
    }


    @JvmStatic
    fun setOnClick(vi: View, click: Func0) {
        vi.setOnClickListener { v ->
            df.catchLog { click.run() }
        }
    }

    /// <summary>
    /// get file extension(not include .)
    /// </summary>
    /// <param name="name"></param>
    /// <returns></returns>
    @JvmStatic
    fun getFileExt(name: String): String {
        val ext = name.lastIndexOf('.');
        if (ext < 0)
            return "";
        val aLastName = name.substring(ext + 1, (name.length - ext - 1));
        return aLastName.toLowerCase()
    }

    @JvmStatic
    fun isEmpty(str: String?): Boolean {
        return str == null || str == ""
    }

    /// <summary>
    /// get an unrepeat file name
    /// </summary>
    /// <param name="file">file origin</param>
    /// <param name="extName">new extension</param>
    /// <returns></returns>
    @JvmStatic
    fun getFile2(file: String, extName: String = ""): String {
        if (file == "")
            return "";
        val extI = file.lastIndexOf('.');
        var name = file;
        if (extI >= 0)
            name = file.substring(0, extI);

        val ext = if (extName == "")
            getFileExt(file)
        else
            extName


        var newName = name + "." + ext;
        for (i in 2..1000) {
            if (File(newName).exists()) {
                newName = name + "_" + i + "." + ext;
            } else {
                return newName;
            }
        }
        return name + "_new." + ext;
    }

    inline fun retry(time: Int, function: () -> Unit) {
        for(i in 0 until time){
            try {
                function();
                break;
            } catch (e: Throwable) {

            }
        }


    }
}