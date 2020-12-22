package net.rxaa.df

import android.content.Context
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


/**
 * Tree RecyclerView
 */
open class TreeRecycler(
    val cont: Context,
    val recycler: RecyclerView,
) : TreeListNode(null, -1) {

    init {
        listTree = this
        isFold = false
    }


    /**
     * 是否开启动画
     */
    var enableAnimation = true

    //记录列表显示的节点
    val displayList = ArrayList<TreeListNode>();

    //view类型id对应的节点
    val viewTypeMap = SparseArray<TreeListNode>();

    val adapter = TreeAdapter(this);

    init {
        if (recycler.layoutManager == null) {
            recycler.layoutManager = LinearLayoutManager(cont)
        }

        recycler.adapter = adapter

    }


}


open class TreeListNode(
    val parent: TreeListNode?,
    //节点左边距，根节点从0开始，每个子节点+1
    val leftPadding: Int,
) {


    //view class
    lateinit var viewClass: Class<*>;

    //位于父节点的索引
    internal var parentI: Int = 0;

    /**
     * 获取位于父节点中datas的索引
     */
    fun indexInParent(): Int {
        return parentI
    }

    /**
     *
     */
    inline fun loopToRoot(func: (node: TreeListNode) -> Unit) {
        var p = parent
        while (p != listTree && p != null) {
            func(p)
            p = p.parent;
        }
    }

    fun showRotate(v: View) {
        if (isFold) {
            if (v.rotation != 0f)
                v.rotation = 0f;
        } else {
            if (v.rotation == 0f)
                v.rotation = 90f;
        }
    }

    fun doRotate(v: View) {
        if (isFold) {
            Animator(v).rotation(90f, 300).start()
        } else {
            Animator(v).rotation(00f, 300).start()
        }
    }

    //所有子成员的view构造
    internal var onCreateView: (viewType: Int) -> CommView = {
        throw Exception("Can not find onCreateView")
    }

    internal var onBindView: (view: CommView, dat: Any, index: Int, node: TreeListNode) -> Unit =
        { view, d, i, n -> }

    lateinit var listTree: TreeRecycler;

    internal lateinit var datas: MutableList<*>;

    /**
     * 是否折叠
     */
    var isFold = true

    /**
     * 子节点列表
     */
    internal var subList: MutableList<TreeListNode>? = null

    /**
     * 多种类型view集合
     */
    internal var typeViewList: ArrayList<TreeRecyvlerData>? = null;


    val isLoad: Boolean
        get() = !subList.isNullOrEmpty()

    /**
     * 展开节点
     */
    fun expand() {
        if (isFold) {
            isFold = false;
            addData()
        }
    }


    /**
     * 切换节点展开或折叠
     */
    fun toggleFold() {

        isFold = !isFold
        if (isFold) {
            removeData()
        } else {
            addData()
        }


    }

    /**
     * 折叠节点
     */
    fun fold() {
        if (!isFold) {
            isFold = true;
            removeData()
        }
    }


    /**
     * 计算子节点个数
     */
    private fun countExpand(): Int {
        val subList = subList ?: return 0;
        var count = subList.size
        for (n in subList) {
            if (!n.isFold) {
                n.isFold = true
                count += n.countExpand()
            }
        }

        return count
    }

    private fun removeData() {
        val subList = subList ?: return;
        if (subList.size < 1)
            return;
        val first = subList.get(0);
        var firstIndex = -1
        firstIndex = listTree.displayList.indexOf(first)

        if (firstIndex < 0)
            return;

        val count = countExpand();

        if (listTree.displayList.size < firstIndex + count) {
            FileExt.logException(MsgException("firstIndex${count} count:${count} out of range:" + listTree.displayList.size))
            return;
        }

        listTree.displayList.subList(firstIndex, firstIndex + count).clear();

        if (listTree.enableAnimation)
            listTree.adapter.notifyItemRangeRemoved(firstIndex, count)
        else
            listTree.adapter.notifyDataSetChanged()
    }

    private fun addData() {
        val subList = subList ?: return;
        for (i in 0 until listTree.displayList.size) {
            val node = listTree.displayList[i]
            if (this == node) {
                listTree.displayList.addAll(i + 1, subList);
                if (listTree.enableAnimation)
                    listTree.adapter.notifyItemRangeInserted(
                        i + 1, subList.size
                    )
                else
                    listTree.adapter.notifyDataSetChanged()
                break;
            }
        }

    }


    /**
     * 绑定节点数据（可以通过判断isLoad来避免重复绑定）
     * list:  绑定数据
     * onCreate 同RecyclerView, 创建View回调
     * onBind   同RecyclerView, 显示View item回调
     */
    inline fun <ListT, reified ViewT : CommView> bindSubList(
        list: MutableList<ListT>,
        noinline onCreate: (viewType: Int) -> ViewT,
        noinline onBind: (view: ViewT, dat: ListT, index: Int, node: TreeListNode) -> Unit
    ) {
        viewClass = ViewT::class.java
        _bindSubList(list, onCreate, onBind)
    }

    fun bindSubView(view: CommView) {

        val list = subList ?: ArrayList<TreeListNode>().also {
            subList = it;
        }
        val node = TreeListNode(
            this,
            leftPadding + 1,
        )
        node.listTree = listTree
        node.parentI = -1;

        list.add(node)

        if (parent == null)
            listTree.displayList.add(node);
    }

    fun <ListT, ViewT : CommView> _bindSubList(
        list: MutableList<ListT>,
        onCreate: (viewType: Int) -> ViewT,
        onBind: (view: ViewT, dat: ListT, index: Int, node: TreeListNode) -> Unit
    ) {

        listTree.viewTypeMap.put(ClassId.getId(viewClass), this)
        datas = list;
        onCreateView = onCreate;
        onBindView = onBind as (view: CommView, dat: Any, index: Int, node: TreeListNode) -> Unit;

        val isFirst = subList == null;

        subList.notNull {
            it.clear()
        }.nope {
            subList = ArrayList<TreeListNode>();
        }
        list.forEachIndexed { index, dat ->
            val subs = subList ?: return@forEachIndexed

            val node = TreeListNode(
                this,
                leftPadding + 1,
            )
            node.listTree = listTree
            node.parentI = index;

            subs.add(node)

            if (parent == null)
                listTree.displayList.add(node);
        }


        //listTree.adapter.notifyDataSetChanged()

    }
}

class TreeRecyvlerData(
    val datas: MutableList<Any>?,
    val onCreate: (viewType: Int) -> CommView,
    val onBind: (view: CommView, dat: Any, index: Int, node: TreeListNode) -> Unit,
    //val onMove: ((fromPosition: Int, toPosition: Int) -> Unit)?,
    val view: CommView?,
) {
    fun size(): Int {
        if (datas != null)
            return datas.size

        return 1;
    }


}


class TreeAdapter(val list: TreeRecycler) : RecyclerView.Adapter<RecyItemHolder>() {


    override fun getItemCount(): Int {

        return list.displayList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyItemHolder {
        FileExt.catchLog {
            val node = list.viewTypeMap.get(viewType)
                ?: throw MsgException("Can not find viewType:" + viewType)

            val v = node.onCreateView(viewType);

            setViewLayout(v)

            return RecyItemHolder(v)
        }

        return RecyItemHolder(CommView(list.cont))
    }

    override fun getItemViewType(position: Int): Int {
        FileExt.catchLog {
            val node = list.displayList.get(position);
            val parent = node.parent ?: return 0;
            return ClassId.getId(parent.viewClass)

        }
        return 0
    }


    override fun onBindViewHolder(holder: RecyItemHolder, position: Int) {
        FileExt.catchLog {
            val node = list.displayList.get(position);
            val parent = node.parent ?: return;
            val dat = parent.datas.get(node.parentI) ?: return;

            parent.onBindView(holder.view, dat, position, node);
        }

    }


    private val mInterpolator = LinearInterpolator()

    override fun onViewAttachedToWindow(holder: RecyItemHolder) {
        super.onViewAttachedToWindow(holder)
        addAnimation(holder)
    }

    private var mLastPosition = -1

    /**
     * 动画类型
     */
    private val mSelectAnimation: BaseAnimation = ScaleInAnimation()

    /**
     * 加载动画
     *
     * @param holder
     */
    private fun addAnimation(holder: RecyItemHolder) {
        if (list.enableAnimation) {
            if (holder.layoutPosition > mLastPosition) {
                val animation = mSelectAnimation
                for (anim in animation.getAnimators(holder.itemView)) {
                    anim.setDuration(300).start()
                    anim.interpolator = mInterpolator
                }
                mLastPosition = holder.layoutPosition
            }
        }
    }


}
