package net.rxaa.view

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.rxaa.util.df
import net.rxaa.ext.FileExt


fun RecyclerView.update() {
    this.adapter?.notifyDataSetChanged()
}


fun setViewLayout(view: CommView) {
    if (view.layoutParams == null) {
        view.layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}




/**
 * 绑定一个BaseView
 */
fun RecyclerView.bindView(
    cont: Context,
    view: CommView
) {
    val adapter = this.adapter ?: kotlin.run {
        initAdapter(this, cont)
    }
    setViewLayout(view)

    if (adapter is RecybindAdapter) {
        adapter.setOnMove(null)
        adapter.list.add(
            RecyclerData(
                null,
                { throw Exception("Can not create") },
                { view, dat, index -> },
                null,
                view
            )
        )
    }
}


/**
 * 绑定一个List
 */
fun <T, TV : CommView> RecyclerView.bindList(
    cont: Context,
    list: List<T>,
    onCreate: () -> TV,
    onBind: (view: TV, dat: T, index: Int) -> Unit,
    onMove: ((fromPosition: Int, toPosition: Int) -> Unit)? = null
) {
    val adapter = this.adapter ?: kotlin.run {
        initAdapter(this, cont)
    }
    if (adapter is RecybindAdapter) {
        adapter.setOnMove(onMove)
        adapter.list.add(
            RecyclerData(
                list as List<Any>,
                onCreate,
                onBind as (view: CommView, dat: Any, index: Int) -> Unit,
                onMove,
                null
            )
        )
    }
}


internal fun initAdapter(re: RecyclerView, cont: Context): RecybindAdapter {
    if (re.layoutManager == null) {
        re.layoutManager = LinearLayoutManager(cont)
    }
    val ada = RecybindAdapter(re)
    re.adapter = ada;
    return ada;
}

class RecyclerData(
    val list: List<Any>?,
    val onCreate: () -> CommView,
    val onBind: (view: CommView, dat: Any, index: Int) -> Unit,
    val onMove: ((fromPosition: Int, toPosition: Int) -> Unit)?,
    val view: CommView?
) {
    fun size(): Int {
        if (list != null)
            return list.size

        return 1;
    }


}


class RecybindAdapter(val recyvler: RecyclerView) : RecyclerView.Adapter<RecyItemHolder>() {
    val list = ArrayList<RecyclerData>();

    var itemTouch: ItemTouchHelper? = null;

    fun setOnMove(onMove: ((fromPosition: Int, toPosition: Int) -> Unit)?) {
        if (itemTouch == null && onMove != null) {
            val mItemTouchHelper = ItemTouchHelper(BindItemDragCallback(this))
            mItemTouchHelper.attachToRecyclerView(recyvler)
            itemTouch = mItemTouchHelper;
        }
    }

    inline fun findDat(position: Int, res: (dat: RecyclerData, i: Int, size: Int) -> Unit) {
        var sizeCount = 0;
        for (i in 0 until list.size) {
            val dat = list[i];
            sizeCount += dat.size();
            if (position < sizeCount) {
                res(dat, i, sizeCount - dat.size())
                return;
            }
        }
        throw Exception("can not find recyclerData")
    }

    override fun getItemCount(): Int {
        return list.sumBy { it.size() }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyItemHolder {
        FileExt.catchLog {
            val dat = list[viewType];
            if (dat.view != null)
                return RecyItemHolder(dat.view)
            else if (dat.list != null)
                return RecyItemHolder(dat.onCreate())
        }
        throw Exception("no view")
    }

    override fun getItemViewType(position: Int): Int {
        FileExt.catchLog {
            findDat(position) { dat, i, size ->
                return i;
            }
        }
        return 0
    }

    override fun onBindViewHolder(holder: RecyItemHolder, position: Int) {
        FileExt.catchLog {
            findDat(position) { dat, i, size ->
                val index = position - size;
                if (dat.list != null) {
                    dat.onBind(holder.view, dat.list[index], index)
                }
            }
        }
    }
}


class BindItemDragCallback(val recy: RecybindAdapter) :
    ItemTouchHelper.Callback() {

    /**
     * 滑动删除时回调
     * @param viewHolder 当前操作的Item对应的viewHolder
     * @param direction 方向
     */
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

    }

    /**
     * 长按拖拽可用（ 或者通过mItemTouchHelper.startDrag(vh)开始拖拽）
     * @return
     */
    override fun isLongPressDragEnabled(): Boolean {
        return true
    }


    /**
     * 拖拽初始化
     */
    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {

        val position = viewHolder.layoutPosition

        recy.findDat(position) { dat, i, size ->
            if (dat.onMove == null)
                return 0;
        }

        //表格四个拖拽方向
        if (recyclerView.getLayoutManager() is GridLayoutManager) {
            val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN or
                    ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
            val swipeFlags = 0
            return ItemTouchHelper.Callback.makeMovementFlags(dragFlags, swipeFlags)

        } else {
            //列表上下两个方向
            val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
            val swipeFlags = 0
            return ItemTouchHelper.Callback.makeMovementFlags(dragFlags, swipeFlags)
        }
    }

    /**
     * 开始拖拽回调
     */
    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {

        val position = viewHolder.layoutPosition

        //得到当拖拽的viewHolder的Position
        val fromPosition = viewHolder.adapterPosition
        //拿到当前拖拽到的item的viewHolder
        val toPosition = target.adapterPosition

        recy.findDat(fromPosition) { datF, i, size ->
            val datFrom = fromPosition - size
            recy.findDat(toPosition) { dat, i, size ->
                val datTo = toPosition - size
                if (dat.onMove == null || datF.onMove != dat.onMove)
                    return false;
                try {
                    //交换数据
                    df.swapData(dat.list as List<*>, datFrom, datTo)
                    recy.notifyItemMoved(fromPosition, toPosition);
                    dat.onMove!!(datFrom, datTo);
                } catch (e: Exception) {
                    FileExt.logException(e)
                    return false
                }
            }
        }
        return true;
    }


    /**
     * 长按选中Item开始拖拽设置背景色
     *
     * @param viewHolder
     * @param actionState
     */
    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
            viewHolder!!.itemView.alpha = 0.7f
            //viewHolder!!.itemView.setBackgroundColor(0x66EEEEEE.toInt())
        }
        super.onSelectedChanged(viewHolder, actionState)
    }

    /**
     * 手指松开的时候还原
     * @param recyclerView
     * @param viewHolder
     */
    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        viewHolder!!.itemView.alpha = 1f;
        //viewHolder.itemView.setBackgroundColor(0)
    }


}