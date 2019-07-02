package rxaa.df

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import java.util.*


open class ViewEx(private val cont: Context) {

    companion object {

    }

    var dialogStyle = 0;

    fun startActivity(inte: Intent) {
        cont.startActivity(inte)
    }

    private var _rootView: View? = null;
    private var _rootDialog: Dialog? = null;

    internal var viewType = 0;

    /**
     * 绑定的view list
     */
    val bindList = ArrayList<() -> Unit>();

    /**
     * 在setContentView之后调用,绑定view对象
     */
    fun <T> bind(func: () -> T): BindView<T> {
        return BindView(func, bindList);
    }

    var renderList: HashMap<View, () -> Unit>? = null;
    /**
     * 触发单个View的渲染函数
     */
    fun <T : View> T.render(): T {
        val renderList = renderList ?: return this

        val func = renderList[this];
        if (func != null)
            func();
        return this
    }

    /**
     * 设置view的渲染函数
     */
    fun <T : View> T.render(func: T.() -> Unit): T {
        if (renderList == null)
            renderList = HashMap<View, () -> Unit>()

        renderList!![this] = {
            func(this)
        }
        return this
    }

    /**
     * 触发所有View的render函数
     */
    fun renderAll() {
        renderList?.forEach { it.value() }
    }


    fun getView(): View {
        return _rootView as View
    }

    fun onClick(func: (v: View) -> Unit) {
        getView().onClick(func)
    }

    fun getContext(): Context {
        return cont;
    }

    fun setViewToTag(): View {
        getView().tag = this
        return getView()
    }

    fun <T:View> find(id: Int): T {
        return _rootView!!.findViewById<T>(id) as T;
    }

    @JvmOverloads
    fun setContentView(layoutResID: Int, parent: ViewGroup? = null) {
        _rootView = df.createView(getContext(), parent, layoutResID)
        bindList.forEach { it() }
    }

    fun setContentView(view: View?) {
        _rootView = view
        bindList.forEach { it() }
    }

    fun setContentView(view: View?, layoutResID: Int): View {
        _rootView = view ?: df.createView(getContext(), layoutResID)
        bindList.forEach { it() }
        return _rootView as View
    }

    val show: ViewEx
        get() {
            getView().visibility = View.VISIBLE
            return this
        }

    val hide: ViewEx
        get() {
            getView().visibility = View.INVISIBLE
            return this;
        }

    val gone: ViewEx
        get() {
            getView().visibility = View.GONE
            return this
        }


    open fun onDialogShow(dialog: Dialog) {

    }

    /**
     * 将此视图作为弹出框显示
     */
    fun showAsDialog(onClose: () -> Unit = {},
                     cancelAble: Boolean = true,
                     onPreShow: (dialog: Dialog) -> Unit = {}) {
        if (_rootDialog != null) {
            closeDialog()
        }

        if (dialogStyle > 0)
            _rootDialog = android.app.Dialog(getContext(), dialogStyle)
        else
            _rootDialog = android.app.Dialog(getContext())
        _rootDialog!!.setContentView(_rootView)
        _rootDialog!!.setCanceledOnTouchOutside(cancelAble)

        _rootDialog!!.setOnDismissListener {
            df.catchLog { onClose() }
        }

        df.catchLog {
            onDialogShow(_rootDialog!!)
        }
        onPreShow(_rootDialog!!);

        _rootDialog!!.show()
    }

    fun isDialogShowing(): Boolean {
        try {
            _rootDialog.notNull {
                return it.isShowing
            }
        } catch(e: Throwable) {
        }
        return false;
    }

    /**
     * 关闭此视图弹窗
     */
    fun closeDialog() {
        try {
            _rootDialog?.dismiss();
            _rootDialog = null
        } catch(e: Exception) {
        }
    }

}