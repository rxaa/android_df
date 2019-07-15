package rxaa.df

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.charset.Charset
import java.util.*


object FileExt {

    fun setSpeakerphoneOn(isSpeakerphoneOn: Boolean) {
        val audioManager = df.appContext!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.isSpeakerphoneOn = isSpeakerphoneOn
        if (isSpeakerphoneOn) {
            audioManager.mode = AudioManager.MODE_NORMAL
        } else {
            audioManager.mode = AudioManager.MODE_IN_CALL
        }
    }

    internal val MIME_MapTable = arrayOf(
        //{后缀名，MIME类型}
        arrayOf(".3gp", "video/3gpp"),
        arrayOf(".apk", "application/vnd.android.package-archive"),
        arrayOf(".asf", "video/x-ms-asf"),
        arrayOf(".avi", "video/x-msvideo"),
        arrayOf(".bin", "application/octet-stream"),
        arrayOf(".bmp", "image/bmp"),
        arrayOf(".c", "text/plain"),
        arrayOf(".class", "application/octet-stream"),
        arrayOf(".conf", "text/plain"),
        arrayOf(".cpp", "text/plain"),
        arrayOf(".doc", "application/msword"),
        arrayOf(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
        arrayOf(".xls", "application/vnd.ms-excel"),
        arrayOf(".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        arrayOf(".exe", "application/octet-stream"),
        arrayOf(".gif", "image/gif"),
        arrayOf(".gtar", "application/x-gtar"),
        arrayOf(".gz", "application/x-gzip"),
        arrayOf(".h", "text/plain"),
        arrayOf(".htm", "text/html"),
        arrayOf(".html", "text/html"),
        arrayOf(".jar", "application/java-archive"),
        arrayOf(".java", "text/plain"),
        arrayOf(".jpeg", "image/jpeg"),
        arrayOf(".jpg", "image/jpeg"),
        arrayOf(".js", "application/x-javascript"),
        arrayOf(".log", "text/plain"),
        arrayOf(".m3u", "audio/x-mpegurl"),
        arrayOf(".m4a", "audio/mp4a-latm"),
        arrayOf(".m4b", "audio/mp4a-latm"),
        arrayOf(".m4p", "audio/mp4a-latm"),
        arrayOf(".m4u", "video/vnd.mpegurl"),
        arrayOf(".m4v", "video/x-m4v"),
        arrayOf(".mov", "video/quicktime"),
        arrayOf(".mp2", "audio/x-mpeg"),
        arrayOf(".mp3", "audio/x-mpeg"),
        arrayOf(".mp4", "video/mp4"),
        arrayOf(".mpc", "application/vnd.mpohun.certificate"),
        arrayOf(".mpe", "video/mpeg"),
        arrayOf(".mpeg", "video/mpeg"),
        arrayOf(".mpg", "video/mpeg"),
        arrayOf(".mpg4", "video/mp4"),
        arrayOf(".mpga", "audio/mpeg"),
        arrayOf(".msg", "application/vnd.ms-outlook"),
        arrayOf(".ogg", "audio/ogg"),
        arrayOf(".pdf", "application/pdf"),
        arrayOf(".png", "image/png"),
        arrayOf(".pps", "application/vnd.ms-powerpoint"),
        arrayOf(".ppt", "application/vnd.ms-powerpoint"),
        arrayOf(".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
        arrayOf(".prop", "text/plain"),
        arrayOf(".rc", "text/plain"),
        arrayOf(".rmvb", "audio/x-pn-realaudio"),
        arrayOf(".rtf", "application/rtf"),
        arrayOf(".sh", "text/plain"),
        arrayOf(".tar", "application/x-tar"),
        arrayOf(".tgz", "application/x-compressed"),
        arrayOf(".txt", "text/plain"),
        arrayOf(".wav", "audio/x-wav"),
        arrayOf(".wma", "audio/x-ms-wma"),
        arrayOf(".wmv", "audio/x-ms-wmv"),
        arrayOf(".wps", "application/vnd.ms-works"),
        arrayOf(".xml", "text/plain"),
        arrayOf(".z", "application/x-compress"),
        arrayOf(".zip", "application/x-zip-compressed"),
        arrayOf("", "*/*")
    )

    /**
     * 根据文件后缀名获得对应的MIME类型。

     * @param file
     */
    fun getMIMEType(file: File): String {

        var type = "*/*"
        val fName = file.name
        //获取后缀名前的分隔符"."在fName中的位置。
        val dotIndex = fName.lastIndexOf(".")
        if (dotIndex < 0) {
            return type
        }
        /* 获取文件的后缀名*/
        val end = fName.substring(dotIndex, fName.length).toLowerCase()
        if (end === "") return type
        //在MIME和文件类型的匹配表中找到对应的MIME类型。
        for (i in MIME_MapTable.indices) { //MIME_MapTable??在这里你一定有疑问，这个MIME_MapTable是什么？
            if (end == MIME_MapTable[i][0])
                type = MIME_MapTable[i][1]
        }
        return type
    }

    /**
     * 打开文件

     * @param fs
     */
    fun openFile(cont: Context, file: File): Boolean {
        try {
            val intent = Intent()
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            //设置intent的Action属性
            intent.action = Intent.ACTION_VIEW
            //获取文件file的MIME类型
            val type = getMIMEType(file)
            //设置intent的data和Type属性。
            intent.setDataAndType(/*uri*/Uri.fromFile(file), type)
            //跳转
            cont.startActivity(intent) //这里最好try一下，有可能会报错。 //比如说你的MIME类型是打开邮箱，但是你手机里面没装邮箱客户端，就会报错。
            return true
        } catch (e: Exception) {
            df.logException(e, false)
            df.msg("无法打开此类型文件!")
            return false
        }

    }


    @JvmStatic
    fun getDiskList(): ArrayList<File> {
        val fi = File("/mnt/")
        val fs = fi.listFiles()
        val al = ArrayList<File>()
        for (f in fs) {
            if (!f.canRead() || f.totalSpace < 1)
                continue

            if (f.name == "obb" || f.name == "asec" || f.name == "user")
                continue

            al.add(f)
        }

        if (al.size > 1)
            return al

        File("/storage/").listFiles().forEach {
            if (!it.canRead() || it.totalSpace < 1)
                return@forEach

            if (it.name.contains("emulated") || it.name == "self")
                return@forEach

            al.add(it)
        }
        return al
    }

    @JvmStatic
    fun getSuffix(name: String): String {
        if (name.isNullOrEmpty())
            return ""

        var suff: String? = null
        try {
            val i = name.lastIndexOf(".")
            suff = name.substring(i + 1)
        } catch (e: Exception) {
            return ""
        }

        return suff.toLowerCase()
    }

    @JvmStatic
    fun suffixIsPic(suff: String): Boolean {
        if (suff == "jpg" || suff == "jpeg" || suff == "png"
            || suff == "bmp" || suff == "gif" || suff == "tif"
        )
            return true

        return false
    }

    @JvmStatic
    fun suffixIsSound(suff: String): Boolean {
        if (suff == "mp3" || suff == "mid" || suff == "wav" || suff == "wma"
            || suff == "ape"
            || suff == "flac"
        )
            return true

        return false
    }

    @JvmStatic
    fun isPic(name: String): Boolean {
        val suff = getSuffix(name)
        return suffixIsPic(suff)
    }

    @JvmStatic
    fun getFileName(f: File?): String {
        if (f == null)
            return "null"

        if (f.isDirectory) {
            val path = f.path.toLowerCase()
            if (path == "/mnt/sdcard")
                return f.name + "(内置储存卡)"
            if (path == "/mnt/sdcard2")
                return f.name + "(外置储存卡)"
            if (path == "/mnt/sdcard1")
                return f.name + "(外置储存卡)"
            if (path == "/mnt/extsdcard")
                return f.name + "(外置储存卡)"
        }


        return f.name
    }


    @JvmStatic
    fun getFileSize(f: File?): String {
        if (f == null)
            return ""

        if (f.parent == "/mnt" || f.parent == "/storage")
            return f.freeSpace.toByteString() + " / " + f.totalSpace.toByteString()

        if (f.isDirectory)
            return ""

        return f.length().toByteString()
    }
}

inline fun <T> List<T>?.lastItem(func: (i: T) -> Unit): T? {
    if (this != null && this.size > 0) {
        val last = this[this.size - 1]
        func(last)
        return last;
    }
    return null;
}

inline fun <T> List<T>?.lastEach(func: (i: T) -> Unit) {
    if (this != null) {
        for (i in this.size - 1 downTo 0)
            func(this[i])
    }
}

inline fun <T> List<T>?.lastEachIndex(func: (dat: T, i: Int) -> Unit) {
    if (this != null) {
        for (i in this.size - 1 downTo 0)
            func(this[i], i)
    }
}


inline fun <T> List<T>?.firstItem(func: (i: T) -> Unit) {
    if (this != null && this.size > 0) {
        func(this[0])
    }
}


fun File?.add(name: String): File {
    return File(this?.path + name)
}

fun File?.addMenu(name: String): File {
    return File(this?.path + "/" + name)
}


inline fun File?.randomAccess(func: (rand: RandomAccessFile) -> Unit) {
    this.randomAccess(false, func)
}

inline fun File?.randomAccess(readOnly: Boolean, func: (rand: RandomAccessFile) -> Unit) {
    var mode = "rws"
    if (readOnly)
        mode = "r";
    val r = RandomAccessFile(this, mode)
    try {
        func(r)
    } finally {
        try {
            r.close()
        } catch (e: Throwable) {
        }
    }

}


operator fun File?.plus(name: String): File {
    return add(name)
}

/**
 * 统计文件夹里所有文件大小
 * @return long 单位为 byte
 */
fun File?.getAllFileSize(): Long {
    var size: Long = 0
    if (this == null)
        return 0
    this.walkBottomUp().forEach { size += it.length() }
    return size
}


fun File?.readAllText(charset: Charset = Charsets.UTF_8): String {
    if (this == null || !this.exists())
        return ""
    return this.readText(charset);
}

/**
 * 循环读取内容至buffer
 */
inline fun InputStream.readAll(
    buffer: ByteArray,
    byteOffset: Int = 0,
    byteCount: Int = buffer.size,
    func: (readByte: Int) -> Unit
) {
    var bytesRead = 0

    while (this.read(buffer, byteOffset, byteCount).apply { bytesRead = this } >= 0) {
        func(bytesRead)
    }
}

fun InputStream.readToString(charset: String = "UTF-8"): String {
    val byteOffset = 0;
    var bytesRead = 0;
    val buffer = ByteArray(4096);
    val baos = ByteArrayOutputStream()
    while (this.read(buffer, byteOffset, buffer.size).apply { bytesRead = this } >= 0) {
        baos.write(buffer, 0, bytesRead)
    }
    return baos.toString(charset)
}