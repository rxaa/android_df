package rxaa.df

import android.app.Activity
import android.content.Context
import java.util.*
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

open class HttpReq<T : Any>() {

    companion object {
        /**
         * 线程池
         */
        var poolHttp = Executors.newFixedThreadPool(1)

        /**
         * 上传线程池
         */
        var poolUpload = Executors.newFixedThreadPool(1)

        /**
         * http日志目录
         */
        var getHttpLogFile = { df.getFileDir() + "/http.txt" }
    }


    var Charset = "UTF-8"
    var url = "";
    var isPost = true;

    fun isGET(): HttpReq<T> {
        isPost = false;
        return this;
    }

    /**
     * http请求入参
     */
    var paras = HashMap<String, Any>();

    //扩展参数
    var extParas = "";

    /**
     * 重载-号，赋值给paras
     */
    operator fun <B : Any> String.minus(that: B) {
        paras[this] = that;
    }

    internal var failMsg = true;
    /**
     * 是否隐藏错误提示消息
     */
    fun hideMsg(): HttpReq<T> {
        failMsg = false;
        return this
    }

    fun showMsg(): HttpReq<T> {
        failMsg = true;
        return this
    }

    internal var showProg = true;
    /**
     * 是否隐藏进度条
     */
    fun hideProg(): HttpReq<T> {
        showProg = false;
        return this
    }

    fun showProg(): HttpReq<T> {
        showProg = true;
        return this
    }

    var progText = "加载中，请稍候...";

    /**
     * 设置进度条文本
     */
    fun progText(text: String): HttpReq<T> {
        progText = text;
        return this
    }

    private var acti: Activity? = null;
    /**
     * 关联一个activity,当activity关闭时,取消传输
     */
    fun activity(act: Context?): HttpReq<T> {
        if (act is Activity)
            acti = act;
        return this
    }

    @Volatile
    var isCancel = false;

    /**
     * 取消传输
     */
    open fun cancel() {
        isCancel = true;
    }


    private var progRecv: (transferSize: Long, fileSize: Long) -> Unit = { t, f -> };
    /**
     * 下载进度回调(此回调在线程池中运行,未转到主线程)
     */
    open fun progressRecv(func: (transferSize: Long, fileSize: Long) -> Unit): HttpReq<T> {
        progRecv = func;
        return this
    }

    private var progSend: (transferSize: Long, fileSize: Long) -> Unit = { t, f -> };
    /**
     * 上传进度回调(此回调在线程池中运行,未转到主线程)
     */
    open fun progressSend(func: (transferSize: Long, fileSize: Long) -> Unit): HttpReq<T> {
        progSend = func;
        return this
    }

    /**
     * 上传进度处理函数
     */
    val progressSendProc = fun(transferSize: Long, fileSize: Long) {
        progSend(transferSize, fileSize);
        isCancel()
    }


    internal fun isCancel() {
        acti.notNull {
            if (it.isFinishing)
                throw MsgException("取消传输,activity close", ExceptionCode.activityClosed, false)
        }

        if (isCancel)
            throw MsgException("取消传输", ExceptionCode.cancelHttp, false)
    }


    /**
     * 解析http返回内容
     */
    var parseResp = fun(resp: String, code: Int): T {
        throw MsgException("未实现parseResp")
    }


    /**
     * post内容格式
     */
    internal var postContent = { http: HttpEx ->
        http.setContentTypeJSON()
        Json.objToJson(paras);
    }

    /**
     * Multipart内容函数
     */
    private var postMultipartFunc: ((form: MultipartForm) -> Unit)? = null;

    /**
     * 设置上传Multipart的内容
     */
    open fun postMultipart(func: MultipartForm.(form: HttpReq<T>) -> Unit): HttpReq<T> {
        postMultipartFunc = { it.func(this) };
        return this
    }

    //http请求参数
    internal var onNewHttpEx = { HttpEx(url).setAcceptGZIP().setKeepAlive() }

    fun setOnNewHttpEx(func: () -> HttpEx): HttpReq<T> {
        onNewHttpEx = func;
        return this
    }

    /**
     * 拼接get请求url参数回调
     */
    var onGetUrl = {
        if (paras.size > 0)
            HttpEx.objToForm(paras, Charset).notEmpty {
                url += getUrlMark + it
            }
    }

    /**
     *是否开启传输日志
     */
    var httpLog = true;

    /**
     * get请求参数分隔符
     */
    var getUrlMark = "?";
    /**
     * 发起请求,同步获取结果
     */
    var request = fun(): T {
        isCancel()
        //拼接get url参数
        if (!isPost) {
            onGetUrl()
        }
        //https请求
        val http = onNewHttpEx()
        isCancel()

        postMultipartFunc.notNull {
            val multi = http.postMultipart(it)
            if (httpLog)
                df.writeLog("postMultipart:\r\n" + url + "\r\n" + multi.textStr, getHttpLogFile())
        }.nope {
            if (isPost) {
                val content = postContent(http)
                if (httpLog)
                    df.writeLog("post:\r\n" + url + "\r\n" + content, getHttpLogFile())
                http.post(content)
            } else {
                if (httpLog)
                    df.writeLog("get:\r\n" + url, getHttpLogFile())
                http.get()
            }
        }

        isCancel()
        val code = http.respCode();
        isCancel()
        val respCont = http.respContentProg(progRecv)
        if (httpLog)
            df.writeLog("resp code: " + code + "\r\n" + respCont, getHttpLogFile())

        return parseResp(respCont, code);
    }


    var beforRequest = {

    }

    var afterRequest = {

    }

    internal var failFun = { e: Throwable -> }
    /**
     * 请求失败回调
     */
    fun failed(res: (e: Throwable) -> Unit): HttpReq<T> {
        failFun = res;
        return this
    }

    /**
     * 捕获到异常
     */
    var onFailed = { e: Throwable -> }

    private fun getPool() = if (postMultipartFunc != null)
        poolUpload
    else
        poolHttp

    open fun runOnPool(func: (req: HttpReq<T>) -> Unit): HttpReq<T> {
        beforRequest();

        val pool = getPool()

        df.runOnPool(pool) {
            try {
                func(this);
            } catch (e: Throwable) {
                onFailed(e)
                failFun(e)
            }
        }
        return this
    }

    suspend open fun await(): T = suspendCoroutine<T> { cont ->
        beforRequest();

        val pool = getPool()
        df.runOnPool(pool) {
            try {
                val resp = request()
                df.runOnUi {
                    afterRequest();
                    cont.resume(resp)
                }

            } catch (e: Throwable) {
                onFailed(e)
                failFun(e)
                df.runOnUi {
                    cont.resumeWithException(e)
                }
            }
        }
    }


    /**
     * 异步获取结果
     */
    open fun ok(res: (e: T) -> Unit): HttpReq<T> {
        runOnPool {
            val resp = request()
            df.runOnUi {
                afterRequest();
                res(resp)
            }
        }
        return this
    }

}

