package net.rxaa.http

import android.app.Activity
import android.content.Context
import net.rxaa.util.ExceptionCode
import net.rxaa.util.MsgException
import net.rxaa.util.df
import net.rxaa.ext.FileExt
import net.rxaa.ext.plus
import java.io.File
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


/**
 * http下载
 * @param url 目标url
 * @param fileUrl 保存的文件名
 */
open class HttpDown(private var url: String, private var fileUrl: File? = null) {

    companion object {
        /**
         * 数据请求链接池
         */
        val pool by lazy { Executors.newFixedThreadPool(1) }

        /**
         * 在线程池中run
         */
        fun runPool(func: suspend () -> Unit) {
            df.runOnPool(pool, func)
        }

        fun getCacheMenu(): File {
            return FileExt.getCacheDir() + "/down/"
        }

    }

    open fun urlHash(): String {
        if (url.length >= 8)
            return "/" + url.hashCode() + url.substring(url.length - 8).replace("/", "")
        return "/" + url.hashCode() + url.replace("/", "")
    }

    protected var prog: (transferSize: Long, fileSize: Long) -> Unit = { _, _ -> }
    open fun progress(func: (transferSize: Long, fileSize: Long) -> Unit): HttpDown {
        prog = func
        return this
    }

    private var tempFile = true

    /**
     * 设置开启文件缓存与断点续传，默认开启
     */
    open fun temp(temp: Boolean): HttpDown {
        tempFile = temp
        return this
    }


    open fun FileMenu(): File {
        return fileUrl ?: getCacheMenu() + urlHash()
    }

    private var acti: Activity? = null

    /**
     * 关联一个activity,当activity关闭时,取消下载
     */
    open fun activity(act: Context): HttpDown {
        if (act is Activity)
            acti = act
        return this
    }

    @Volatile
    var isCancel = false

    /**
     * 取消下载
     */
    open fun cancel() {
        isCancel = true
    }

    //请求标识
    var httpTag: String? = null

    open fun onHttp(ht: HttpEx) {

    }


    private var onStart_: (ht: HttpEx) -> Unit = {}

    //http开始请求前回调，可在此修改http头信息
    fun onStart(func: (ht: HttpEx) -> Unit): HttpDown {
        onStart_ = func;
        return this
    }

    /**
     * 开始下载任务
     */
    suspend fun await() = suspendCoroutine<Unit> { cont ->
        start { ex ->
            if (ex != null)
                cont.resumeWithException(ex)
            else {
                cont.resume(Unit)
            }
        }
    }

    /**
     * 开始下载任务,回调函数中参数Exception的值为null表示下载成功,否则失败
     */
    open fun start(res: (e: Exception?) -> Unit): HttpDown {

        val http = try {
            HttpEx(url)
        } catch (e: Exception) {
            res(e)
            return this
        }
        onStart_(http)
        onHttp(http)
        isCancel = false
        runPool {
            var ex: Exception? = null
            if (!FileMenu().exists() || !tempFile) {
                try {
                    http.downloadFile(FileMenu(), tempFile, prog = { trans, size ->

                        prog(trans, size)

                        val act = acti
                        if (act != null && act.isFinishing) {
                            throw MsgException("取消下载", ExceptionCode.activityClosed.ordinal)
                        }

                        if (isCancel) {
                            throw MsgException("取消下载", ExceptionCode.cancelHttp.ordinal)
                        }
                    })
                } catch (e: Exception) {
                    ex = e
                }
            }
            df.runOnUi {
                res(ex)
            }
        }
        return this
    }
}