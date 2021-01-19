package net.rxaa.http

import android.content.Context
import net.rxaa.util.Json
import net.rxaa.util.MsgException
import net.rxaa.util.df
import net.rxaa.ext.plus
import net.rxaa.ext.randomAccess
import net.rxaa.ext.readAll
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.Socket
import java.net.URL
import java.net.URLEncoder
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*
import java.util.zip.GZIPInputStream
import javax.net.ssl.*

class MultipartForm(val con: HttpURLConnection, val FixedLength: Boolean = true) {

    /**
     * 计算post的总大小
     */
    internal var size = 0;

    init {

    }

    /**
     * 反射修改socket send缓冲区(解决上传文件进度条一直显示100%问题)
     */
    internal fun setSendBufSize() {
        try {
            //https delegate
            if (con is HttpsURLConnection) {
                con.setDoOutput(true);
                con.setChunkedStreamingMode(0);
                return
            }


            val heF = con.javaClass.getDeclaredField("httpEngine")
            if (heF != null) {
                heF.isAccessible = true;
                val httpEngine = heF.get(con);
                if (httpEngine != null) {
                    val connectionF = httpEngine.javaClass.getDeclaredField("connection")
                    connectionF.isAccessible = true;
                    val connection = connectionF.get(httpEngine);
                    if (connection != null) {
                        val sockF = connection.javaClass.getDeclaredField("socket");
                        sockF.isAccessible = true;
                        val sock = sockF.get(connection) as Socket;
                        sock.sendBufferSize = bufferSize;
                    }

                }

            }
        } catch (e: Exception) {
//            Log.e("wwww", "eee", e)
        }
    }


    internal val textList = ArrayList<ByteArray>();
    var textStr = "";
    internal val fileList =
        ArrayList<Triple<ByteArray, File, (transferSize: Long, fileSize: Long) -> Unit>>();

    /**
     * 添加字符串字段
     */
    fun addText(name: String, value: String) {
        textStr += "$name=$value, "
        val text =
            "--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n$value\r\n".toByteArray()

        textList.add(text);

        size += text.size;
    }

    /**
     * 添加文件字段
     */
    fun addFile(
        name: String, file: File, prog: (transferSize: Long, fileSize: Long) -> Unit = { t, l -> }
    ) {

        val text =
            ("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"; filename=\"${file.name}\"\r\n" +
                    "Content-Transfer-Encoding: binary\r\n\r\n").toByteArray()

        fileList.add(Triple(text, file, prog));

        size += text.size + 2 + file.length().toInt();


    }

    internal fun send() {
        val end = "--" + boundary + "--\r\n"

        size += end.length;

        if (FixedLength) {
            con.setFixedLengthStreamingMode(size);//主要是这句，用来禁掉缓存，不过需要将上传数据的大小传进去
            con.useCaches = false;
        }

        //上传文本字段
        for (text in textList) {
            con.outputStream.write(text)
        }


        //上传文件
        val buffer = ByteArray(bufferSize)
        setSendBufSize();
        for ((text, file, prog) in fileList) {

            con.outputStream.write(text)

            val fileSize = file.length()
            file.inputStream().use { f ->
                var transferSize = 0L;

                f.readAll(buffer) { bytesRead ->
                    con.outputStream.write(buffer, 0, bytesRead);
                    //进度
                    transferSize += bytesRead;
                    prog(transferSize, fileSize)
                }
            }

            con.outputStream.write("\r\n".toByteArray())
        }


        con.outputStream.write(end.toByteArray())
    }

    companion object {
        val boundary = "QKjIgA6s5rVdpsMzD10plVZ7YhaJyjL"
        val bufferSize = 8 * 1024;


    }
}


class HttpEx(val url: String, timeOut: Int = 15 * 1000) {
    val conn = URL(url).openConnection() as HttpURLConnection
    var charset = "UTF-8";
    var bufferSize = 8 * 1024;

    init {
        conn.connectTimeout = timeOut;
//        val soc: Socket?;
//        conn.readTimeout = 60 * 1000
    }


    /**
     * 指定X509证书验证
     */
    fun setSSL(
        ssl: javax.net.ssl.SSLSocketFactory?,
        hostnameVerifier: HostnameVerifier = hostnameVerifierStrict
    ): HttpEx {
        (conn as HttpsURLConnection).hostnameVerifier = hostnameVerifier
        conn.sslSocketFactory = ssl
        return this
    }

    fun setContentType(type: String): HttpEx {
        conn.setRequestProperty("Content-Type", type);
        return this
    }

    fun setContentTypeJSON() {
        setContentType("application/json")
    }

    fun setContentTypeForm() {
        setContentType("application/x-www-form-urlencoded")
    }

    fun setHeader(key: String, value: String): HttpEx {
        conn.setRequestProperty(key, value);
        return this
    }

    fun getHeaders() = conn.requestProperties

    fun setKeepAlive(): HttpEx {
        conn.setRequestProperty("Connection", "keep-alive");
        return this
    }

    fun setAcceptGZIP(): HttpEx {
        conn.setRequestProperty("Accept-Encoding", "gzip");
        return this
    }

    fun get(): HttpEx {
        //        conn.setUseCaches(false);
        conn.requestMethod = "GET";
        conn.setRequestProperty("Charset", charset);
        return this
    }


    fun post(content: String): HttpEx {
        conn.requestMethod = "POST"
        conn.setRequestProperty("Charset", charset);
        conn.outputStream.write(content.toByteArray());// 输入参数
        return this
    }

    fun postJSON(obj: Any): HttpEx {
        setContentTypeJSON()
        post(Json.objToJson(obj))
        return this
    }

    fun postMultipart(cont: (form: MultipartForm) -> Unit): MultipartForm {
        conn.requestMethod = "POST"
        conn.setRequestProperty("Charset", charset);
        conn.setRequestProperty(
            "Content-Type",
            "multipart/form-data; boundary=" + MultipartForm.boundary
        );
        val mul = MultipartForm(conn)

        cont(mul)

        mul.send();
        return mul
    }

    fun downloadFile(
        file: File,
        tempFile: Boolean = true,
        prog: (transferSize: Long, fileSize: Long) -> Unit = { t, f -> },
        resp: () -> Boolean = { true }
    ) {

        val newFile = file + ".temp"
        val oldSize = newFile.length()

        if (tempFile && oldSize > 0) {
            // 设置断点续传的开始位置
            conn.setRequestProperty("Range", "bytes=" + oldSize + "-");
        }

        val parent = file.parentFile
        if (!parent.exists()) {
            parent.mkdirs();
        }

        var fileSize = conn.contentLength.toLong();
        val respType = conn.contentType;
        val respCode = conn.responseCode;
        val isHtml = respType != null && respType.indexOf("html") >= 0;

        if (!resp())
            return

        if (respCode == 404) {
            newFile.delete()
            throw MsgException("404,文件不存在!")
        }

        if ((respCode != 206 && respCode != 200)) {
            //所请求的范围无法满足,可能文件已修改
            if (respCode == 416) {
                newFile.delete()
            }
            throw MsgException("" + respCode + ",下载失败:" + conn.responseMessage)
        }

        //总传输长度
        var transferLen = 0L;
        newFile.randomAccess { rand ->

            // 服务器支持断点续传
            if (oldSize > 0 && (respCode == 206)) {
                transferLen = oldSize;
                fileSize += oldSize;
                rand.seek(oldSize);
            }

            var now = 0L;
            respContent { bytes, size ->
                rand.write(bytes, 0, size)
                transferLen += size
                if (System.currentTimeMillis() - now > 100) {
                    prog(transferLen, fileSize)
                    now = System.currentTimeMillis();
                }
            }
        }

        if (!newFile.renameTo(file))
            throw MsgException("文件重命名失败!");
    }

    fun respCode(): Int {
        return conn.responseCode
    }

    fun respContentType(): String? {
        return conn.contentType
    }

    fun respContentLength(): Int {
        return conn.contentLength
    }

    fun respMsg(): String? {
        return conn.responseMessage
    }


    /**
     * 获取content字符串
     */
    fun respContent(): String {
        val baos = ByteArrayOutputStream()
        respContent { bytes, size ->
            baos.write(bytes, 0, size)
        };
        return baos.toString(charset)
    }

    /**
     * 获取content字符串,与传输进度
     */
    fun respContentProg(prog: (transferSize: Long, fileSize: Long) -> Unit = { t, f -> }): String {
        val baos = ByteArrayOutputStream()
        val fileSize = respContentLength().toLong();
        //总传输长度
        var transferLen = 0L;
        respContent { bytes, size ->
            baos.write(bytes, 0, size)
            transferLen += size;
            prog(transferLen, fileSize)
        };
        return baos.toString(charset)
    }

    /**
     * 读取content的原始byte内容
     */
    fun respContent(func: (bytes: ByteArray, size: Int) -> Unit) {

        val content_encode = conn.contentEncoding
        val buffer = ByteArray(bufferSize)


        var inputStream = if (respCode() >= 400) conn.errorStream else conn.inputStream

        if (null != content_encode && content_encode.equals("gzip")) {
            inputStream = GZIPInputStream(inputStream)
        }
        inputStream.readAll(buffer) { func(buffer, it) }
    }

    companion object {

        /**
         * 禁用证书域名验证
         */
        @JvmStatic
        val hostnameVerifierDisable: HostnameVerifier = HostnameVerifier { s, sslSession ->
            true;
        }

        /**
         * 验证证书域名
         */
        @JvmStatic
        val hostnameVerifierStrict: HostnameVerifier = HostnameVerifier lam@{ s, sslSession ->
            val hv = HttpsURLConnection.getDefaultHostnameVerifier();
            return@lam hv.verify(s, sslSession);
        }
        val connectionTimeOut = 15 * 1000;
        val cookieMap = HashMap<String, ArrayList<String>>();

        var count = 0;

        /**
         * 禁用HTTPS证书验证
         */
        @Deprecated(message = "忽略证书验证会造成HTTPS中间人攻击")
        fun disableSSL() {
            val context = SSLContext.getInstance("TLS");
            //javax.net.ssl.X509TrustManager ()
            context.init(null, arrayOf(TrustAllManager()), null);
            HttpsURLConnection.setDefaultSSLSocketFactory(context.socketFactory);
            HttpsURLConnection.setDefaultHostnameVerifier({ s, sslSession -> true });
        }

        @JvmStatic
        fun getUrlHost(url: String): String {
            val pos = url.indexOf('/', 7)
            if (pos >= 0)
                return url.substring(0, pos);
            return url;
        }

        /**
         * 加载X.509证书
         */
        @JvmStatic
        fun loadX509(crtFile: InputStream): SSLContext? {
            try {
                val cf = CertificateFactory.getInstance("X.509");
                val ca = cf.generateCertificate(crtFile) as X509Certificate;

                //                val keystore = KeyStore.getInstance("PKCS12");//android平台上支持的keystore type好像只有PKCS12，不支持JKS
                val defautKey = KeyStore.getDefaultType()
                val keystore = KeyStore.getInstance(defautKey);
                keystore.load(null, null);
                //                keystore.setCertificateEntry("trust", ca);//ca为别名
                keystore.setCertificateEntry("ca", ca);

                val tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
                val tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
                tmf.init(keystore);

                // Create an SSLContext that uses our TrustManager
                val context = SSLContext.getInstance("TLS");
                context.init(null, tmf.trustManagers, null);
                return context;
            } finally {
                crtFile.close()
            }
        }

        @JvmStatic
        fun loadX509(content: Context, file: String): SSLContext? {
            //getAssets().open("load-der.crt");
            return loadX509(content.assets.open(file))
        }


        @JvmStatic
        @JvmOverloads
        fun <T, R> objToForm(list: Map<T, R>, charset: String = "UTF-8"): String {
            val newUrl = StringBuilder();

            for ((key, v) in list) {
                newUrl.append("" + key + "=" + URLEncoder.encode("" + v, charset) + "&")
            }
            if (newUrl.length > 0)
                newUrl.setLength(newUrl.length - 1)

            return newUrl.toString();
        }


        @JvmStatic
        @JvmOverloads
        fun <T> objToForm(list: Array<Pair<String, T>>, charset: String = "UTF-8"): String {
            return objToForm(list.asIterable(), charset);
        }

        @JvmStatic
        @JvmOverloads
        fun <T> objToForm(list: Iterable<Pair<String, T>>, charset: String = "UTF-8"): String {
            val newUrl = StringBuilder();

            for ((key, v) in list) {
                newUrl.append("" + key + "=" + URLEncoder.encode("" + v, charset) + "&")
            }
            if (newUrl.length > 0)
                newUrl.setLength(newUrl.length - 1)

            return newUrl.toString();
        }

        @JvmStatic
        @JvmOverloads
        fun objToForm(list: Any, charset: String = "UTF-8"): String {
            var newUrl = "";

            if (list != null) {
                df.getClassFields(list.javaClass, true) { field, i ->
                    newUrl += "" + field.name + "=" + URLEncoder.encode(
                        "" + field.get(list),
                        charset
                    ) + "&"
                }
            }

            if (newUrl.length > 0)
                return newUrl.substring(0, newUrl.length - 1);
            return newUrl
        }

        @JvmStatic
        @JvmOverloads
        fun <T> urlEncode(list: Map<String, T>, charset: String = "UTF-8"): String {
            var newUrl = "";

            for ((key, v) in list) {
                newUrl += "" + key + "=" + URLEncoder.encode("" + v, charset) + "&"
            }
            if (newUrl.length > 0)
                newUrl = newUrl.substring(0, newUrl.length - 1);
            return newUrl
        }

        @JvmStatic
        @JvmOverloads
        fun <T> urlEncode(list: Array<Pair<String, T>>, charset: String = "UTF-8"): String {
            return urlEncode(list.asIterable(), charset)
        }

        @JvmStatic
        @JvmOverloads
        fun <T> urlEncode(list: Iterable<Pair<String, T>>, charset: String = "UTF-8"): String {
            val params = StringBuffer()
            for (i in list) {
                params.append(i.first)
                params.append("=")
                params.append(URLEncoder.encode("" + i.second, charset))
                params.append("&")
            }

            if (params.length > 0)
                params.setLength(params.length - 1);

            return params.toString();
        }

    }

}


class TrustAllManager : X509TrustManager {
    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
    }

    override fun checkClientTrusted(arg: Array<out X509Certificate>?, arg1: String?) {

    }

    override fun getAcceptedIssuers(): Array<out X509Certificate>? {
        // TODO Auto-generated method stub
        return null;
    }
}