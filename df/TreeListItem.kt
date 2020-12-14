package rxaa.df

import android.content.Context
import android.util.SparseArray
import android.util.SparseIntArray
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

open class TreeList(
    val cont: Context,
    val recycler: RecyclerView,
) : TreeListNode(null) {

    init {
        listTree = this
    }

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
        listTree.adapter.notifyDataSetChanged()
    }

    fun fold() {
        isFold = true;
        listTree.adapter.notifyDataSetChanged()
    }

    fun <ListT, ViewT : CommView> bindSubList(
        list: MutableList<ListT>,
        onCreate: (viewType: Int) -> ViewT,
        onBind: (view: ViewT, dat: ListT, index: Int, node: TreeListNode) -> Unit
    ) {
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
            subs.add(node)

        }
        listTree.adapter.notifyDataSetChanged()

    }
}


class TreeAdapter(val list: TreeList) : RecyclerView.Adapter<RecyItemHolder>() {


    fun countItmes(node: TreeListNode): Int {
        var sum = node.datas.count();
        list.subList.notNull {
            it.forEach {
                if (!it.isFold) {
                    sum += countItmes(it)
                }
            }
        }
        return sum;
    }

    override fun getItemCount(): Int {
        var count = countItmes(list)

        return count
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyItemHolder {
        val v = list.onCreateView(0);

        setViewLayout(v)

        return RecyItemHolder(v)
    }

    override fun getItemViewType(position: Int): Int {

        return 0
    }


    override fun onBindViewHolder(holder: RecyItemHolder, position: Int) {

        var count = 0;
        for (index in 0 until list.datas.size) {
            count += countItmes(list.subList?.get(index) ?: continue)

        }

        val dat = list.datas.get(position) ?: return;
        val node = list.subList?.get(position) ?: return;
        list.onBindView(holder.view, dat, position, node);
    }

}
