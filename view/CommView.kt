package net.rxaa.view

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import net.rxaa.ext.FileExt
import net.rxaa.ext.nope
import net.rxaa.ext.notNull
import net.rxaa.util.df


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

    val cont
        get() = context

    fun startActivity(inte: Intent) {
        context.startActivity(inte)
    }

    //该view所属的ListViewEx
    var listEx: ViewBuffer? = null;

    internal var _rootDialog: Dialog? = null;

    internal var viewType = 0;

    /**
     *  在当前线程启动协程
     */
    fun launch(func: suspend () -> Unit) {
        df.launchMain(func)
    }

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
     * 清除指定的viewGroup,并将其成员View加入缓存
     */
    open fun clearView() {
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

    /**
     *
     */
    fun <T> create(func: () -> T): BindViewEx<T> {
        return BindViewEx {
            func()
        }
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

            v.listEx = listEx;
            view.addView(v);
            return v;
        }

        val v = newViewFunc();
        v.listEx = null
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

    lateinit var inflateView: View;


    /**
     * Set the View's content view to the given layout and return the associated binding.
     */
    fun <T> binding(
        resId: Int,
        func: (v: View) -> T,
    ): BindViewEx<T> {
        inflate(context, resId, this)
        getChildAt(childCount - 1).notNull {
            inflateView = it;
            if (it is LinearLayout) {
                layoutParams = it.layoutParams
            }

        }
        return BindViewEx {
            func(inflateView)
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

    fun requireContext(): Context {
        return context!!
    }


    private var mPopup: PopupWindow? = null

    open fun showAsPopUp(
        view: View,
        x: Int,
        y: Int,
        gravity: Int = Gravity.LEFT or Gravity.TOP,
        cancelAble: Boolean = true,
        onPreShow: (dialog: PopupWindow) -> Unit = {}
    ) {
        mPopup = PopupWindow(context).also { mPopup ->
            // 设置pop获取焦点，如果为false点击返回按钮会退出当前Activity，如果pop中有Editor的话，focusable必须要为true\
            mPopup.contentView = this;
            //mPopup.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT)
            // mPopup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
            mPopup.setBackgroundDrawable(null)
            mPopup.setFocusable(true)
            // 设置pop可点击，为false点击事件无效，默认为true
            mPopup.setTouchable(true)
            // 设置点击pop外侧消失，默认为false；在focusable为true时点击外侧始终消失
            mPopup.setOutsideTouchable(cancelAble)
            onPreShow(mPopup);

            val loc = intArrayOf(0, 0)
            view.getLocationInWindow(loc)
            mPopup.showAtLocation(
                view,
                gravity,
                x + loc[0],
                y + loc[1],
            )
        }


    }

    /**
     * 将此视图作为弹出框显示
     */
    open fun showAsDialog(
        act: Activity,
        onClose: () -> Unit = {},
        cancelAble: Boolean = true,
        onPreShow: (dialog: Dialog) -> Unit = {}
    ) {
        if (_rootDialog != null) {
            closeDialog()
        }

        if (dialogStyle > 0)
            _rootDialog = android.app.Dialog(act, dialogStyle)
        else
            _rootDialog = android.app.Dialog(act)
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

        try {
            mPopup?.dismiss()
            mPopup = null
        } catch (e: Exception) {

        }
    }

}