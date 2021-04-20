package net.rxaa.view

import android.view.ViewGroup
import net.rxaa.ext.nope
import net.rxaa.ext.notNull

class ViewBuffer {
    /**
     *  缓存view item的子view
     */
    internal var viewBuffer: HashMap<Class<*>, ArrayList<CommView>>? = null

    /**
     * view item子view最大缓存数
     */
    val maxViewBuffer = 10;

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
                    addViewBuffer(it);
                }
            }

        }

    }

    fun addViewBuffer(vEx: CommView) {
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