package rxaa.df

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.size
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding


/**
 * 公共子View
 */
open class CommView : LinearLayout {


    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        // TODO Auto-generated constructor stub
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        // TODO Auto-generated constructor stub
    }


    constructor(context: Context) : super(context) {
        // TODO Auto-generated constructor stub

    }

    var dialogStyle = 0;

    fun startActivity(inte: Intent) {
        context.startActivity(inte)
    }

    //该view所属的ListViewEx
    var listEx: ListViewEx<*>? = null;

    internal var _rootDialog: Dialog? = null;

    internal var viewType = 0;


    /**
     * 清除指定的viewGroup,并将其成员View加入缓存
     */
    open fun ViewGroup.clearView() {
        listEx.notNull {
            it.addViewBuffer(this);
            this.removeAllViews()
        }.nope {
            this.removeAllViews()
        }
    }

    /**
     * view scroll回调
     */
    open fun onScroll() {

    }

    fun <T : View> find(id: Int): T {
        return findViewById<T>(id) as T;
    }

    /**
     * 向指定的ViewGroup添加ViewEx
     * newViewFunc为ViewEx构造函数，只有当缓存中未找到该View时，才会重新创建
     *
     */
    inline fun <reified T : CommView> ViewGroup.addView(noinline newViewFunc: () -> T): T {
        return _addView(this, T::class.java, newViewFunc);
    }

    /**
     * 向指定的ViewGroup添加View
     */
    fun <T : CommView> _addView(view: ViewGroup, clas: Class<T>, newViewFunc: () -> T): T {
        listEx.notNull {

            val bufferV = it.getViewBuffer(clas);

            val v = if (bufferV != null)
                bufferV as T
            else
                newViewFunc()

            view.addView(v);
            return v;
        }

        val v = newViewFunc();
        view.addView(v);
        return v;
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

    private lateinit var inflateView: View;

    /**
     *  通过resId创建view并返回其关联的binding.
     */
    fun <T : ViewDataBinding> dataBinding(resId: Int): BindViewEx<T> {

        inflate(context, resId, this)
        getChildAt(size - 1).notNull {
            inflateView = it;
        }

        return BindViewEx {
            DataBindingUtil.bind<T>(
                inflateView
            )!!
        }
    }


    open fun onDialogShow(dialog: Dialog) {

    }

    /**
     * 防止被输入法遮挡,在dialog show之后设置才能生效
     * （同时需要设置activity属性android:windowSoftInputMode="adjustResize"）
     */
    open fun dialogMatchParent() {
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
    open fun showAsDialog(
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
        _rootDialog!!.setContentView(this)
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

    open fun isDialogShowing(): Boolean {
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
    open fun closeDialog() {
        try {
            _rootDialog?.dismiss();
            _rootDialog = null
        } catch (e: Exception) {
        }
    }

}