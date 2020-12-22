package net.rxaa.util

import net.rxaa.ext.FileExt
import net.rxaa.ext.addMenu
import net.rxaa.ext.readAllText
import java.io.File

/**
 * 文件对象储存
 */
class FileObject<T : Any>(private val clas: Class<T>, private val fileName: File? = null) {

    init {

    }

    private var inst: T? = null;

    /**
     * 主数据
     */
    var dat: T
        get() {
            if (inst == null) {
                load()
            }
            return inst!!
        }
        set(para) {
            inst = para
        }


    /**
     * 文件目录
     */
    val file: File
        get() = fileName ?: FileExt.getInnerFileDir().addMenu(clas.name);
        //get() = fileName ?: df.getFileDir().addMenu(clas.name);

    /**
     * 重新读取文件
     */
    fun load() {
        synchronized(this) {
            val obj = clas.newInstance();
            val str = file.readAllText();
            if (str.isNotEmpty()) {
                Json.jsonToObj(str, obj)
            }
            inst = obj;
        }
    }

    /**
     * 保存dat至文件
     */
    fun save() {
        synchronized(this) {
            if (inst == null)
                return

            val str = Json.objToJson(inst);
            file.writeText(str);
        }
    }
}
