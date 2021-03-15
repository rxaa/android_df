package net.rxaa.util

import net.rxaa.ext.FileExt
import net.rxaa.ext.addMenu
import net.rxaa.ext.readAllText
import java.io.File
import java.io.Serializable

/**
 * 文件对象储存
 */
open class FileObject<T : Serializable>(val clas: Class<T>, private val fileName: File? = null) {


    //对象实例

    @Volatile
    protected var inst: T? = null;

    /**
     * 编辑并保存对象
     */
    open fun edit(func: (dat: T) -> Unit) {
        synchronized(this) {
            val obj = inst ?: load()
            try {
                func(obj)
            } finally {
                save();
            }
        }
    }


    /**
     * 获取对象实例
     */
    var dat: T
        get() = inst ?: synchronized(this) {
            inst ?: load()
        }
        set(value) {
            synchronized(this) {
                inst = value
                save()
            }
        }


    /**
     * 文件目录
     */
    val file: File
        get() = fileName ?: FileExt.getInnerFileDir().addMenu(clas.name);
    //get() = fileName ?: df.getFileDir().addMenu(clas.name);

    /**
     * 重新读取文件对象
     */
    open fun load(): T {
        val obj = clas.newInstance();
        val str = file.readAllText();
        if (str.isNotEmpty()) {
            Json.jsonToObj(str, obj)
        }
        inst = obj;
        return obj
    }

    /**
     * 清空并保存
     */
    open fun clear() {
        synchronized(this) {
            val obj = clas.newInstance();
            inst = obj;
            save()
        }
    }

    /**
     * 保存文件对象
     */
    open fun save() {
        val str = Json.objToJson(inst ?: return);
        file.writeText(str);
    }
}
