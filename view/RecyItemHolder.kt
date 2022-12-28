package net.rxaa.view

import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import net.rxaa.util.df
import net.rxaa.ext.FileExt
import net.rxaa.ext.onClick


class RecyItemHolder(val view: CommView) : RecyclerView.ViewHolder(view) {


}

class RecyAdapter(val list: ListViewEx<*>) : RecyclerView.Adapter<RecyItemHolder>() {


    override fun getItemCount(): Int {
        return list.count() + list.headViewList.size + list.footViewList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyItemHolder {
        FileExt.catchLog {
            if (viewType <= list.headViewType)
                return RecyItemHolder(list.headViewList[list.headViewType - viewType])
            else if (viewType <= list.footViewType)
                return RecyItemHolder(list.footViewList[list.footViewType - viewType])
        }
        val v = list.onCreateView(viewType)
        
        setViewLayout(v)

        v.listEx = list.buffer
        return RecyItemHolder(v)
    }

    override fun getItemViewType(position: Int): Int {
        FileExt.catchLog {
            if (list.headViewList.size > 0 && position < list.headViewList.size) {
                return list.headViewType - position;
            } else if (list.footViewList.size > 0 && position >= list.data.size + list.headViewList.size) {
                val sub = (position - list.data.size - list.headViewList.size)
                return list.footViewType - sub;
            }

            return list.getViewType(position - list.headViewList.size)
        }
        return 0
    }

    override fun onBindViewHolder(holder: RecyItemHolder, position: Int) {
        FileExt.catchLog {
            if (position >= list.headViewList.size && position < list.headViewList.size + list.data.size) {
                val index = position - list.headViewList.size;
                list.onBindView(holder.view, index)
                if (list.onItemClick != null) {
                    holder.view.onClick {
                        list.onItemClick!!(index, holder.view)
                    }
                }
            } else {

            }
        }
    }

}

class ItemDragCallback(
    val lve: ListViewEx<*>,
    val onMove: (fromPosition: Int, toPosition: Int) -> Unit
) :
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
        //headView 不用交换
        if (lve.headViewList.size > 0 && position < lve.headViewList.size) {
            return 0
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
        //headView不用交换
        if (lve.headViewList.size > 0 && position < lve.headViewList.size) {
            return false
        }

        //得到当拖拽的viewHolder的Position
        val fromPosition = viewHolder.adapterPosition
        //拿到当前拖拽到的item的viewHolder
        val toPosition = target.adapterPosition

        if (toPosition < lve.headViewList.size) {
            return false
        }

        //数据位置需要减去headView数量
        val datFrom = fromPosition - lve.headViewList.size
        val datTo = toPosition - lve.headViewList.size

        try {
            //交换数据
            df.swapData(lve.data, datFrom, datTo)
            lve.recyAdapter!!.notifyItemMoved(fromPosition, toPosition);
            onMove(datFrom, datTo);
        } catch (e: Exception) {
            FileExt.logException(e)
            return false
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