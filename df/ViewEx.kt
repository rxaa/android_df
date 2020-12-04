package rxaa.df

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import java.util.*


/**
 * 公共基础子View
 */
open class ViewEx(private val cont: Context) {

    companion object {
        private val viewTag = -29384;

        fun getFromTag(vi: View): ViewEx? {
            return vi.getTag(viewTag) as? ViewEx
        }
    }


    var dialogStyle = 0;

    fun startActivity(inte: Intent) {
        cont.startActivity(inte)
    }

    //该view所属的ListViewEx
    var listEx: ListViewEx<*>? = null;


    internal var _rootView: View? = null;
    internal var _rootDialog: Dialog? = null;

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


    class BindViewEx<out T>(val func: () -> T) : Lazy<T> {

        override fun isInitialized(): Boolean {
            return true
        }

        private val _value: T = func()

        override val value: T
            get() {
                return _value
            }
    }


    /**
     *  return View's associated binding.
     */
    fun <T : ViewDataBinding> dataBinding(resId: Int, view: View? = null): BindViewEx<T> {
        return BindViewEx {
            DataBindingUtil.bind<T>(
                setContentView(
                    view,
                    resId
                )
            )!!
        }
    }

    /**
     * 清除指定的viewGroup,并将其成员View加入缓存
     */
    fun clearView(view: ViewGroup) {
        listEx.notNull {
            it.addViewBuffer(view);
            view.removeAllViews()
        }.nope {
            view.removeAllViews()
        }
    }


    /**
     * view scroll回调
     */
    open fun onScroll() {

    }

    /**
     * 向指定的ViewGroup添加ViewEx
     * newViewFunc为ViewEx构造函数，只有当缓存中未找到该View时，才会重新创建
     *
     */
    inline fun <reified T : ViewEx> addView(view: ViewGroup, noinline newViewFunc: () -> T): T {
        return _addView(view, T::class.java, newViewFunc);
    }


    /**
     * 向指定的ViewGroup添加View
     */
    fun <T : ViewEx> _addView(view: ViewGroup, clas: Class<T>, newViewFunc: () -> T): T {
        listEx.notNull {

            val bufferV = it.getViewBuffer(clas);

            val v = if (bufferV != null)
                bufferV as T
            else
                newViewFunc()

            view.addView(v.getView());
            return v;
        }

        val v = newViewFunc();
        view.addView(v.getView());
        return v;
    }


    fun getView(): View {
        return _rootView as View
    }

    fun onClick(func: suspend (v: View) -> Unit) {
        getView().onClick(func)
    }

    fun getContext(): Context {
        return cont;
    }

    fun setViewToTag(): View {
        getView().tag = this
        return getView()
    }

    fun <T : View> find(id: Int): T {
        return _rootView!!.findViewById<T>(id) as T;
    }


    @JvmOverloads
    fun setContentView(layoutResID: Int, parent: ViewGroup? = null) {
        _rootView = df.createView(getContext(), parent, layoutResID)
        _rootView!!.setTag(viewTag, this);
        bindList.forEach { it() }
    }

    fun setContentView(view: View?) {
        _rootView = view
        _rootView!!.setTag(viewTag, this);
        bindList.forEach { it() }
    }

    fun setContentView(view: View?, layoutResID: Int, parent: View? = null): View {
        _rootView = view ?: if (parent != null && parent is ViewGroup) df.createView(
            getContext(),
            parent,
            layoutResID
        ) else df.createView(getContext(), layoutResID)
        _rootView!!.setTag(viewTag, this);
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
     * 防止被输入法遮挡,在dialog show之后设置才能生效
     * （同时需要设置activity属性android:windowSoftInputMode="adjustResize"）
     */
    fun dialogMatchParent() {
        _rootDialog.notNull { alert ->
            val dialogWin = alert.window ?: return;
            val lp = dialogWin.attributes;
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialogWin.attributes = lp;
        }
    }

    /**
     * 将此视图作为弹出框显示
     */
    fun showAsDialog(
        onClose: () -> Unit = {},
        cancelAble: Boolean = true,
        onPreShow: (dialog: Dialog) -> Unit = {}
    ) {
        if (_rootDialog != null) {
            closeDialog()
        }

        if (dialogStyle > 0)
            _rootDialog = android.app.Dialog(getContext(), dialogStyle)
        else
            _rootDialog = android.app.Dialog(getContext())
        _rootView.notNull {
            _rootDialog!!.setContentView(it)
        }
        _rootDialog!!.setCanceledOnTouchOutside(cancelAble)

        _rootDialog!!.setOnDismissListener {
            FileExt.catchLog { onClose() }
        }

        FileExt.catchLog {
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
        } catch (e: Throwable) {
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
        } catch (e: Exception) {
        }
    }

}