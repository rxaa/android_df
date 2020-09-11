package rxaa.df

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

fun String?.isEmpty(): Boolean {
    if (this == null)
        return true;
    if (this.length == 0)
        return true;
    return false;
}

val Int.resource: String
    get() = df.appContext!!.resources.getString(this);

fun RecyclerView.isScrollTop(): Boolean {
    return this.computeVerticalScrollOffset() <= 0
}

fun RecyclerView.isScrollBottom(): Boolean {
    return this.computeVerticalScrollExtent() +
            this.computeVerticalScrollOffset() >= this.computeVerticalScrollRange()
}


fun View.setPaddingTop(v: Int) {
    this.setPadding(this.paddingLeft, v, this.paddingRight, this.paddingBottom);
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

fun <T : View> T.onClick(cb: suspend (v: T) -> Unit): T {
    this.setOnClickListener({ v ->
        df.launch {
            cb(this)
        }
    })
    return this
}

fun <T : View> T.onLongClick(cb: suspend (v: T) -> Unit): T {
    this.setOnLongClickListener({ v ->
        df.launch { cb(this) }
        return@setOnLongClickListener true;
    })
    return this
}


fun <T : View> T.onTouch(cb: (event: MotionEvent) -> Boolean): T {
    this.setOnTouchListener { view, motionEvent ->
        df.catchLog { return@setOnTouchListener cb(motionEvent) }
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
        df.catchLog { cb(b) }
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

fun android.widget.AdapterView<*>?.onItemClick(cb: (index: Int) -> Unit) {
    this?.setOnItemClickListener { adapterView, view, i, l ->
        df.catchLog { cb(i) }
    }
}

fun android.widget.AdapterView<*>?.onItemLongClick(cb: (index: Int) -> Boolean) {
    this?.setOnItemLongClickListener { adapterView, view, i, l ->
        df.catchLog { return@setOnItemLongClickListener cb(i) }
        return@setOnItemLongClickListener false
    }
}

/**
 * 转换为带单位的字节大小
 */
fun Long.toByteString(): String {
    if (this <= 1024) {
        return "" + this + " Byte"
    }

    if (this <= 1024 * 1024) {
        return "" + this / 1024 + " KB"
    }

    if (this <= 1024 * 1024 * 1024) {
        return "%.1f MB".format(this.toDouble() / 1024.0 / 1024.0)
    }

    return "%.1f GB".format(this.toDouble() / 1024.0 / 1024.0 / 1024.0)
}

/**
 * 转换为带单位的字节大小
 */
fun Int.toByteString(): String {
    return this.toLong().toByteString()
}

/**
 * 转换为至少两位字串
 */
fun Int.to2String(): String {
    if (this < 10)
        return "0" + this
    return this.toString();
}

/**
 * 从字串中抽取数字
 */
fun String?.getNumber(): String {
    if (this == null || this.length == 0)
        return ""
    var ret = "";
    this.forEach {
        if (it in '0'..'9')
            ret += it
    }

    return ret
}

fun String?.getInt(default: Int = 0): Int {
    if (this == null || this.length == 0)
        return default
    try {
        return this.toDouble().toInt()
    } catch (e: Exception) {
    }

    return default
}

fun String?.getLong(default: Long = 0): Long {
    if (this == null || this.length == 0)
        return default
    try {
        return this.toDouble().toLong()
    } catch (e: Exception) {
    }
    return default
}

fun String?.getDouble(default: Double = 0.0): Double {
    if (this == null || this.length == 0)
        return default
    try {
        return this.toDouble()
    } catch (e: Exception) {
    }
    return default
}
