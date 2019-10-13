package rxaa.df

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*


/**
 * ListView辅助类
 * 关联List<>数据, 并自动填充数据至ListView或LinearView或RecyclerView里
 * 使得调用者无需关心ListView或LinearView或RecyclerView之间的差异
 */
class ListViewEx<ListT>(cont: Context, groupView: ViewGroup) {

    /**
     * 关联的List数据
     */
    var data = ArrayList<ListT>()

    /**
     * 同RecyclerView, 创建View回调
     */
    var onCreateView: (viewType: Int) -> ViewEx = { type -> throw Exception("Unimplement onCreateView function") }
    /**
     * 同RecyclerView, 显示View item回调
     */
    var onBindView: (vi: ViewEx, position: Int) -> Unit = { vi: ViewEx, position: Int -> }
    /**
     * 同RecyclerView, 获取View类型回调
     */
    var getViewType: (position: Int) -> Int = { position -> 0 }


    /**
     * item点击事件
     */
    var onItemClick: ((index: Int, vi: View) -> Unit)? = null
    var onItemLongClick: ((index: Int, vi: View) -> Boolean)? = null

    internal var listView: AbsListView? = null
    internal var mCont: android.content.Context? = null
    internal var adapt: LiAdapter? = null
    internal var recyAdapter: RecyAdapter? = null;
    internal var linearView: LinearLayout? = null
    internal var _recyclerView: RecyclerView? = null


    init {
        if (groupView is ListView)
            initListViewEx(cont, groupView)
        else if (groupView is GridView)
            initGridView(cont, groupView)
        else if (groupView is LinearLayout)
            initLinearLayout(cont, groupView)
        else if (groupView is RecyclerView)
            initRecycler(cont, groupView)
        else {
            throw Exception("不支持此ViewGroup")
        }
    }


    /**
     * 关联一个view,当list size为0时显示,否则隐藏
     */
    var emptyView: View? = null;

    fun isListView(f: (lv: ListView) -> Unit): Boolean {
        if (listView is ListView) {
            f(listView as ListView)
            return true
        }
        return false;
    }

    fun isRecycler(f: (lv: RecyclerView) -> Unit): Boolean {
        if (_recyclerView != null) {
            f(_recyclerView as RecyclerView)
            return true
        }

        return false
    }

    internal val headViewType = -39583;
    internal val footViewType = -11583;
    /**
     * ListView head列表
     */
    internal val headViewList = ArrayList<ViewEx>();
    internal val footViewList = ArrayList<ViewEx>();

    fun addFooter(view: ViewEx): ListViewEx<ListT> {
        isListView {
            it.addHeaderView(view.getView())
        }

        _recyclerView.notNull {
            footViewList.add(view);
            view.getView().layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        linearView.notNull {
            footViewList.add(view);
            it.addView(view.getView())
        }
        return this
    }

    /**
     * 同ListView,向List添加一个head
     */
    fun addHeader(view: ViewEx): ListViewEx<ListT> {
        isListView {
            it.addHeaderView(view.getView())
        }
        _recyclerView.notNull {
            headViewList.add(view);
            view.getView().layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        linearView.notNull {
            headViewList.add(view);
            it.addView(view.getView())
        }
        return this
    }

    /**
     * ListView
     */
    fun removeFooter(view: ViewEx): ListViewEx<ListT> {
        isListView {
            it.removeFooterView(view.getView())
        }
        _recyclerView.notNull {
            footViewList.remove(view)
        }
        linearView.notNull {
            footViewList.remove(view)
            it.removeView(view.getView())
        }

        return this
    }

    /**
     * 同ListView
     */
    fun removeHeader(view: ViewEx): ListViewEx<ListT> {
        isListView {
            it.removeHeaderView(view.getView())
        }
        _recyclerView.notNull {
            headViewList.remove(view)
        }
        linearView.notNull {
            headViewList.remove(view)
            it.removeView(view.getView())
        }
        return this
    }

    fun removeAllHeader(): ListViewEx<ListT> {
        isListView {
            it.removeAllViews()
        }
        _recyclerView.notNull {
            headViewList.clear();
        }
        linearView.notNull { linear ->
            headViewList.forEach {
                linear.removeView(it.getView())
            }
            headViewList.clear();
        }
        return this
    }


    /**
     * 添加头
     */
    fun addHeader(view: View): ListViewEx<ListT> {
        val listView = listView;
        if (listView !== null && listView is ListView) {
            listView.addHeaderView(view)
        }
        linearView.notNull {
            it.addView(view)
        }
        return this
    }


    private fun initRecycler(cont: Context, re: RecyclerView) {
        if (re.layoutManager == null) {
            re.layoutManager = LinearLayoutManager(cont)
        }
        _recyclerView = re;
        recyAdapter = RecyAdapter(this);
        re.adapter = recyAdapter
        mCont = cont;
    }

    private fun initLinearLayout(cont: Context, ll: LinearLayout) {
        linearView = ll
        mCont = cont
    }

    private fun initGridView(cont: Context, listViewid: GridView) {
        // TODO Auto-generated constructor stub
        listView = listViewid
        adapt = LiAdapter()
        listViewid.adapter = adapt
        mCont = cont

        listViewid.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, i, l ->
            try {
                if (i >= 0 && i < data.size && onItemClick != null)
                    onItemClick!!(i, view)
            } catch (e: Throwable) {
                // TODO Auto-generated catch block
                df.logException(e, true)
            }
        }

        listView!!.onItemLongClickListener = AdapterView.OnItemLongClickListener { arg0, arg1, arg2, arg3 ->
            // TODO Auto-generated method stub
            try {
                if (arg2 >= 0 && arg2 < data.size
                    && onItemLongClick != null
                )
                    return@OnItemLongClickListener onItemLongClick!!(arg2, arg1)
            } catch (e: Throwable) {
                // TODO Auto-generated catch block
                df.logException(e, true)
            }

            false
        }
    }


    private fun initListViewEx(cont: Context, listViewid: ListView) {
        // TODO Auto-generated constructor stub
        listView = listViewid
        val tv = TextView(df.appContext)
        listViewid.addFooterView(tv)

        adapt = LiAdapter()
        listViewid.adapter = adapt

        listViewid.removeFooterView(tv)
        mCont = cont

        listView!!.onItemClickListener = AdapterView.OnItemClickListener { arg0, arg1, arg2, arg3 ->
            var arg2 = arg2
            // TODO Auto-generated method stub
            try {
                arg2 -= listViewid.headerViewsCount
                if (arg2 >= 0 && arg2 < data.size
                    && onItemClick != null
                )
                    onItemClick!!(arg2, arg1)
            } catch (e: Throwable) {
                // TODO Auto-generated catch block
                df.logException(e, true)
            }
        }

        listView!!.onItemLongClickListener = AdapterView.OnItemLongClickListener { arg0, arg1, arg2, arg3 ->
            var arg2 = arg2
            // TODO Auto-generated method stub
            try {
                arg2 -= listViewid.headerViewsCount
                if (arg2 >= 0 && arg2 < data.size
                    && onItemLongClick != null
                )
                    return@OnItemLongClickListener onItemLongClick!!(arg2, arg1)
            } catch (e: Throwable) {
                // TODO Auto-generated catch block
                df.logException(e, true)
            }

            false
        }
    }

    fun getContext(): Context? {
        return mCont
    }

    private fun showListItem(vi: View?, index: Int): View? {
        try {

            val v = run {
                val ve = if (vi == null) {
                    val type = getViewType(index)
                    onCreateView(type).apply {
                        viewType = type
                        setViewToTag()
                    }
                } else {
                    vi.tag as ViewEx
                }

                onBindView(ve, index)
                ve.getView()
            }

            if (v != null) {
                if (onItemClick != null) {
                    v.onClick {
                        onItemClick!!(index, v)
                    }
                }

                if (onItemLongClick != null) {
                    v.setOnLongClickListener(View.OnLongClickListener {
                        // TODO Auto-generated method stub
                        try {
                            return@OnLongClickListener onItemLongClick!!(index, v)
                        } catch (e: Exception) {
                            // TODO Auto-generated catch block
                            df.logException(e, true)
                        }
                        false
                    })

                }
            }

            return v
        } catch (e: Throwable) {
            // TODO Auto-generated catch block
            df.logException(e, true)
        }

        return vi
    }


    /**
     * 添加一条数据
     */
    fun add(data: ListT?) {
        if (data == null)
            return;

        this.data.add(data)
        if (linearView != null) {
            val v = showListItem(null, this.data.size - 1)
            linearView!!.addView(v)
        }
    }

    /**
     * 添加一列数据
     * @param data 数据
     * @param order 是否正序插入
     *
     */
    @JvmOverloads
    fun add(data: List<ListT>?, order: Boolean = true) {
        if (data == null)
            return

        if (order) {
            for (l in data) {
                add(l)
            }
        } else {
            for (i in data.indices) {
                add(data[data.size - i - 1])
            }
        }

        update()
    }

    /**
     * 向listView添加一组数据

     * @param index 插入位置
     * *
     * @param data  数据
     * *
     * @param order 是否正序插入
     */
    fun add(index: Int, data: List<ListT>?, order: Boolean = true) {
        if (data == null)
            return

        if (data.size < 1)
            return

        val ni = ArrayList<ListT>()
        for (i in 0..index - 1) {
            ni.add(get(i))
        }

        if (order) {
            for (i in data.indices) {
                ni.add(data[i])
            }
        } else {
            for (i in data.indices) {
                ni.add(data[data.size - i - 1])
            }
        }

        for (i in index..size() - 1) {
            ni.add(get(i))
        }
        this.data = ni

        if (linearView != null) {
            for (i in data.indices) {
                val v = showListItem(null, index + i)
                linearView!!.addView(v, index + i)
            }
        }

        update()
    }

    /**
     * 像指定位置添加一条数据
     */
    fun add(i: Int, data: ListT) {
        this.data.add(i, data)
        if (linearView != null) {
            val v = showListItem(null, i)
            linearView!!.addView(v, i)
        }
        update()
    }


    fun clear() {
        data.clear()
        if (linearView != null) {
            linearView!!.removeAllViews()
        }
        update()
    }

    fun size(): Int {
        return data.size
    }

    fun count(): Int {
        return data.size
    }

    /**
     * 启用recyclerView拖拽
     */
    fun enableDrag(onMove: (fromPosition: Int, toPosition: Int) -> Unit = { f, t -> }) {
        _recyclerView.notNull { recycler ->
            val mItemTouchHelper = ItemTouchHelper(ItemDragCallback(this, onMove))
            mItemTouchHelper.attachToRecyclerView(recycler)
        }
    }

    /**
     * 移除指定位置view
     */
    fun del(i: Int) {
        data.removeAt(i)
        if (linearView != null) {
            linearView!!.removeViewAt(i)
        }
        update()
    }

    /**
     * 移除指定位置view
     */
    fun removeAt(i: Int) {
        del(i)
    }

    /**
     * 移除指定位置view
     */
    fun delete(i: Int) {
        del(i)
    }


    fun getData(i: Int): ListT {
        return data[i]
    }

    operator fun get(i: Int): ListT {
        return data[i]
    }

    /**
     * 获取list最后一条数据

     * @return
     */
    val last: ListT
        get() {
            val i = data.size - 1
            return data[i]
        }

    /**
     * 触发view更新(LinearLayout需要调用updateAll更新所有行)
     */
    fun update() {
        if (adapt != null)
            adapt!!.notifyDataSetChanged()

        if (recyAdapter != null)
            recyAdapter!!.notifyDataSetChanged()

        onUpdate();

        emptyView.notNull {
            if (this.size() < 1)
                it.show;
            else {
                it.gone;
            }
        }
    }

    /**
     * 更新指定行view,用于LinearLayout
     */
    fun update(i: Int) {
        if (linearView != null) {
            showListItem(linearView!!.getChildAt(i), i)
        }
        update()
    }

    /**
     * 使用制定数据更新指定行
     */
    fun update(i: Int, dat: ListT) {
        data[i] = dat

        if (linearView != null) {
            showListItem(linearView!!.getChildAt(i), i)
        }
        update()
    }


    /**
     * 更新所有行,用于LinearLayout
     */
    fun updateAll() {

        if (linearView != null) {
            data.forEachIndexed { index, listT ->
                showListItem(linearView!!.getChildAt(index), index)
            }

        }

        update()
        onUpdate();
    }

    /**
     * 调用update()触发
     */
    var onUpdate = {};


    internal fun getViewByType(view: ViewEx, wtype: Int): ViewEx {
        if (view.viewType == wtype)
            return view;

        return onCreateView(wtype).apply {
            viewType = wtype;
            setViewToTag()
        }
    }

    inner class LiAdapter : BaseAdapter() {
        override fun getView(index: Int, arg1: View?, arg2: ViewGroup): View? {
            try {
                val type = getViewType(index);
                val vi = if (arg1 == null) {

                    onCreateView!!(type).apply {
                        viewType = type;
                        setViewToTag()
                    }
                } else {
                    getViewByType(arg1.tag as ViewEx, type)
                }

                onBindView(vi, index)
                return vi.getView()
            } catch (e: Throwable) {
                df.logException(e, true)
            }
            return arg1
        }

        override fun getItemViewType(position: Int): Int {
            return getViewType(position)
        }

        override fun getItemId(arg0: Int): Long {
            // TODO Auto-generated method stub
            return arg0.toLong()
        }

        override fun getItem(arg0: Int): Any {
            // TODO Auto-generated method stub
            return arg0
        }

        override fun getCount(): Int {
            // TODO Auto-generated method stub
            return data.size
        }
    }
}
