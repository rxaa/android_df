package rxaa.df

import android.app.Activity
import android.content.Context
import java.io.File
import java.util.concurrent.Executors

open class HttpDown {
    var url: String = ""

    constructor(url: String) {
        this.url = url;
    }

    companion object {
        /**
         * 数据请求链接池
         */
        val pool = Executors.newFixedThreadPool(2)

        /**
         * 在线程池中run
         */
        fun runPool(func: suspend () -> Unit) {
            df.runOnPool(pool, func)
        }

        fun getCacheMenu(): File {
            return df.getCacheDir() + "/down/"
        }

    }

    open fun urlHash(): String {
        if (url.length >= 8)
            return "/" + url.hashCode() + url.substring(url.length - 8).replace("/", "")
        return "/" + url.hashCode() + url.replace("/", "")
    }

    protected var prog: (transferSize: Long, fileSize: Long) -> Unit = { t, f -> };
    open fun progress(func: (transferSize: Long, fileSize: Long) -> Unit): HttpDown {
        prog = func;
        return this
    }

    private var tempFile = true;
    open fun temp(temp: Boolean): HttpDown {
        tempFile = temp;
        return this
    }


    open fun FileMenu(): File {
        return getCacheMenu() + urlHash();
    }

    private var acti: Activity? = null;
    /**
     * 关联一个activity,当activity关闭时,取消下载
     */
    open fun activity(act: Context): HttpDown {
        if (act is Activity)
            acti = act;
        return this
    }

    @Volatile
    var isCancel = false;

    /**
     * 取消下载
     */
    open fun cancel() {
        isCancel = true;
    }

    /**
     * 开始下载任务,回调函数中参数Exception的值为null表示下载成功,否则失败
     */
    open fun start(res: (e: Exception?) -> Unit): HttpDown {


        val http = try {
            HttpEx(url);
        } catch (e: Exception) {
            res(e)
            return this;
        }
        isCancel = false;
        runPool {
            var ex: Exception? = null;
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
                    ex = e;
                }
            }
            df.runOnUi {
                res(ex)
            }
        }
        return this
    }
}