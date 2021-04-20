package net.rxaa.view

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import net.rxaa.ext.nope
import net.rxaa.ext.notNull
import net.rxaa.util.df


class ViewBuffer {
    companion object {

        const val maxBufferSize: Int = 64;

        /**
         * 根据内存大小设置缓存数量，最多64
         */
        fun getBufferCount(): Int {
            val mi = ActivityManager.MemoryInfo()
            val am = df.context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.getMemoryInfo(mi);
            val mem = mi.availMem / 1024 / 1024 / 16;
            if (mem > maxBufferSize) {
                return maxBufferSize
            }
            if (mem <= 0) {
                return 1
            }
            return mem.toInt();
        }
    }

    /**
     *  缓存view item的子view
     */
    internal var viewBuffer: HashMap<Class<*>, ArrayList<CommView>>? = null

    /**
     * view item子view最大缓存数
     */
    val maxViewBuffer by lazy { getBufferCount() };

    fun getViewBuffer(clas: Class<*>): CommView? {
        val list = viewBuffer?.get(clas);
        if (list != null && list.size > 0) {
            return list.removeLast();
        }
        return null;
    }

    fun addViewBuffer(vi: ViewGroup) {
        for (i in 0 until vi.childCount) {
            vi.getChildAt(i).notNull {
                if (it is CommView) {
                    addViewToBuffer(it);
                }
            }

        }

    }

    fun addViewToBuffer(vEx: CommView) {
        val map = viewBuffer ?: HashMap();
        if (viewBuffer == null) {
            viewBuffer = map;
        }
        map.get(vEx.javaClass).notNull {
            addViewBuffer(vEx, it)
        }.nope {
            val list = ArrayList<CommView>();
            map.put(vEx.javaClass, list);
            addViewBuffer(vEx, list)
        }

    }

    fun addViewBuffer(vi: CommView, list: ArrayList<CommView>) {
        if (list.size < maxViewBuffer) {
            list.add(vi);
        }
    }

}