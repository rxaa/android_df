package net.rxaa.ext

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.CompoundButton
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import net.rxaa.util.df
import net.rxaa.view.CommView


val Int.resource: String
    get() = df.appContext!!.resources.getString(this);

fun RecyclerView.isScrollTop(): Boolean {
    return this.computeVerticalScrollOffset() <= 0
}

fun RecyclerView.isScrollBottom(): Boolean {
    return this.computeVerticalScrollExtent() +
            this.computeVerticalScrollOffset() >= this.computeVerticalScrollRange()
}

fun View.findParentCommView(): CommView? {
    var p: ViewParent? = this.parent;
    while (p != null) {
        if (p is CommView) {
            return p
        }
        p = p.parent
    }
    return null;
}

fun View.setPaddingTop(v: Int) {
    this.setPadding(this.paddingLeft, v, this.paddingRight, this.paddingBottom);
}

fun View.setPaddingLeft(v: Int) {
    this.setPadding(v, this.paddingTop, this.paddingRight, this.paddingBottom);
}

fun View.setTopStatusBar() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        setPaddingTop(this.paddingTop + getContext().getStatusBarHeight());
    }
}

fun Window.fullScreen(gravity: Int = Gravity.CENTER) {
    this.decorView.setPadding(0, 0, 0, 0)
    val lp = this.attributes
    lp.gravity = gravity;
    lp.width = WindowManager.LayoutParams.MATCH_PARENT
    lp.height = WindowManager.LayoutParams.WRAP_CONTENT
    this.attributes = lp
}

/**
 * 状态栏透明
 */
fun View.statusBarTransparentNotTop(act: Activity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        act.window.statusBarTransparent()
    else
        act.window.statusBarTransparentHalf()
}

/**
 * 状态栏透明
 */
fun View.statusBarTransparent(act: Activity) {
    this.statusBarTransparentNotTop(act);
    setTopStatusBar()
}

val <T : View> T.show: T
    get() {
        this.visibility = View.VISIBLE
        return this
    }

val <T : View> T.hide: T
    get() {
        this.visibility = View.INVISIBLE
        return this;
    }

val <T : View> T.gone: T
    get() {
        this.visibility = View.GONE
        return this
    }

var lastClickTime = 0L

fun <T : View> T.onClick(cb: suspend (v: T) -> Unit): T {
    this.setOnClickListener { _ ->
        if (System.currentTimeMillis() - lastClickTime < 120) {
            return@setOnClickListener
        }
        lastClickTime = System.currentTimeMillis()
        df.launchMain {
            cb(this)
        }
    }
    return this
}

fun <T : View> T.onLongClick(cb: suspend (v: T) -> Unit): T {
    this.setOnLongClickListener { v ->
        df.launchMain { cb(this) }
        return@setOnLongClickListener true;
    }
    return this
}


fun <T : View> T.onTouch(cb: (event: MotionEvent) -> Boolean): T {
    this.setOnTouchListener { _, motionEvent ->
        FileExt.catchLog { return@setOnTouchListener cb(motionEvent) }
        false
    }
    return this
}

fun EditText.onTextChange(func: suspend EditText.(old: String) -> Unit): EditText {
    this.addTextChangedListener(object : TextWatcher {
        var old = ""
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            df.launch { func(old) }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            old = s.toString()
        }

        override fun afterTextChanged(s: Editable?) {
            //            Log.i("wwwwwwwwwwwwwww", "afterTextChanged" + s.toString());
        }
    })
    return this
}

fun EditText.onEditor(onDone: suspend EditText.() -> Unit): EditText {
    this.setOnEditorActionListener({ textView, actionId, keyEvent ->

        when (actionId) {
            EditorInfo.IME_ACTION_SEND,
            EditorInfo.IME_ACTION_GO,
            EditorInfo.IME_ACTION_DONE,
            EditorInfo.IME_ACTION_SEARCH ->
                df.launch { onDone() }
        }
        true
    })
    return this
}

fun Context?.ui(func: () -> Unit) {
    df.runOnUi(func)
}

fun <T : CompoundButton> T.onCheckedChanged(cb: (isChecked: Boolean) -> Unit): T {
    this.setOnCheckedChangeListener { compoundButton, b ->
        FileExt.catchLog { cb(b) }
    }
    return this
}

fun Dialog?.setLocation(x: Int, y: Int, gravity: Int = Gravity.LEFT or Gravity.TOP) {
    if (this == null)
        return;
    val dialogWindow = this.window ?: return;
    val lp = dialogWindow.attributes
    dialogWindow.setGravity(gravity)
    lp.x = x
    lp.y = y
}

/**
 * 设置dialog与View的相对位置
 */
fun Dialog?.setLocationToView(
    v: View,
    x: Int,
    y: Int,
    gravity: Int = Gravity.LEFT or Gravity.TOP
) {
    if (this == null)
        return;
    val loc = intArrayOf(0, 0)
    v.getLocationInWindow(loc)

    val dialogWindow = this.window ?: return;
    val lp = dialogWindow.attributes
    dialogWindow.setGravity(gravity)
    lp.x = loc[0] + x
    lp.y = loc[1] + y
}

fun android.widget.AdapterView<*>?.onItemClick(cb: (index: Int) -> Unit) {
    this?.setOnItemClickListener { adapterView, view, i, l ->
        FileExt.catchLog { cb(i) }
    }
}

fun android.widget.AdapterView<*>?.onItemLongClick(cb: (index: Int) -> Boolean) {
    this?.setOnItemLongClickListener { adapterView, view, i, l ->
        FileExt.catchLog { return@setOnItemLongClickListener cb(i) }
        return@setOnItemLongClickListener false
    }
}
