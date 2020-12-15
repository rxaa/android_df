package rxaa.df

import android.content.Context
import android.util.SparseArray
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.collections.ArrayList

open class TreeList(
    val cont: Context,
    val recycler: RecyclerView,
) : TreeListNode(null) {

    init {
        listTree = this
    }


    //记录列表显示的节点
    val displayList = ArrayList<TreeListNode>();

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
) {

    //view class
    lateinit var viewClass: Class<*>;

    //位于父节点的索引
    var parentI: Int = 0;


    //所有子成员的view构造
    var onCreateView: (viewType: Int) -> CommView = {
        throw Exception("Can not find onCreate")
    }

    var onBindView: (view: CommView, dat: Any, index: Int, node: TreeListNode) -> Unit =
        { view, d, i, n -> }

    lateinit var listTree: TreeList;

    lateinit var datas: MutableList<*>;

    /**
     * 是否折叠
     */
    var isFold = true

    var subList: MutableList<TreeListNode>? = null


    fun expand() {
        isFold = false;
        listTree.adapter.notifyDataSetChanged()
    }


    fun toggleFold() {

        isFold = !isFold
        if (isFold) {
            removeData()
        } else {
            addData()
        }

        listTree.adapter.notifyDataSetChanged()


    }

    fun fold() {
        isFold = true;
        listTree.adapter.notifyDataSetChanged()
    }


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

        listTree.displayList.subList(firstIndex, firstIndex + countExpand()).clear();

    }

    private fun addData() {
        val subList = subList ?: return;
        for (i in 0 until listTree.displayList.size) {
            val node = listTree.displayList[i]
            if (this == node) {
                listTree.displayList.addAll(i + 1, subList);
                break;
            }
        }
    }

    inline fun <ListT, reified ViewT : CommView> bindSubList(
        list: MutableList<ListT>,
        noinline onCreate: (viewType: Int) -> ViewT,
        noinline onBind: (view: ViewT, dat: ListT, index: Int, node: TreeListNode) -> Unit
    ) {
        viewClass = ViewT::class.java
        _bindSubList(list, onCreate, onBind)
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
        subList.notNull {
            it.clear()
        }.nope {
            subList = ArrayList<TreeListNode>();
        }
        list.forEachIndexed { index, dat ->
            val subs = subList ?: return@forEachIndexed

            val node = TreeListNode(
                this,
            )
            node.listTree = listTree
            node.parentI = index;

            subs.add(node)

            if (parent == null)
                listTree.displayList.add(node);
        }


        listTree.adapter.notifyDataSetChanged()

    }
}


class TreeAdapter(val list: TreeList) : RecyclerView.Adapter<RecyItemHolder>() {


    override fun getItemCount(): Int {

        return list.displayList.size
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyItemHolder {
        val node = list.viewTypeMap.get(viewType)
            ?: throw java.lang.Exception("can not find view type:" + viewType)

        val v = node.onCreateView(viewType);

        setViewLayout(v)

        return RecyItemHolder(v)
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

}
