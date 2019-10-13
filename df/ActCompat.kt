package rxaa.df

import android.R
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.appcompat.app.AppCompatActivity
import java.util.*


private val instAct = HashMap<String, ActCompat?>()

val <T : ActCompat> Class<T>.inst: T?
    get() = instAct[this.name] as T?



open class ActCompat : AppCompatActivity() {
    @Throws(Exception::class)
    open fun onCreateEx() {
    }

    @Throws(Exception::class)
    open fun onDestoryEx() {
    }

    @Throws(Exception::class)
    open fun onPauseEx() {
    }

    @Throws(Exception::class)
    open fun onStopEx() {
    }

    @Throws(Exception::class)
    open fun onResumeEx() {
    }

    /**
     * 界面初始化预先绘制回调(可以获取各个View高度)
     */
    @Throws(Exception::class)
    open fun onPreDraw() {
    }

    @Throws(Exception::class)
    open fun onActivityResultEx(requestCode: Int, resultCode: Int, data: Intent?) {

    }

    fun <T:View> find(id: Int): T {
        return findViewById<T>(id) as T;
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        ActivityEx.onActivityRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    final override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        ActivityEx.onActivityResult(this, requestCode, resultCode, data);
        df.catchLog { onActivityResultEx(requestCode, resultCode, data) }
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)

        df.catchLog { bindList.forEach { it() } }

    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        df.catchLog { bindList.forEach { it() } }
    }

    override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        super.setContentView(view, params)
        df.catchLog { bindList.forEach { it() } }
    }

    fun getContext(): ActCompat {
        return this
    }

    /**
     * 绑定的view
     */
    val bindList = ArrayList<() -> Unit>();

    val createList = ArrayList<() -> Unit>();

    /**
     * 在setContentView之后调用,绑定view对象
     */
    fun <T> bind(func: () -> T): BindView<T> {
        return BindView(func, bindList);
    }

    /**
     * 延迟加载(在onCreate之后调用)
     */
    fun <T> create(func: () -> T): BindView<T> {
        return BindView(func, createList);
    }

    val renderList = HashMap<View, () -> Unit>();

    /**
     * 触发单个View的渲染函数
     */
    fun <T : View> T.render(): T {
        val func = renderList[this];
        if (func != null)
            func();
        return this
    }

    /**
     * 设置view的渲染函数
     */
    fun <T : View> T.render(func: T.(vi: T) -> Unit): T {
        renderList[this] = {
            func(this)
        }
        return this
    }

    /**
     * 触发所有View的render函数
     */
    fun renderAll() {
        df.catchLog { renderList.forEach { it.value() } }

    }


    /**
     * 禁用:当内存不足被系统干掉时,自动关闭此actvity
     */
    fun disableKilled() {
        this.intent.putExtra(ActivityEx.allowKilledStr, true);
    }

    /**
     * 是否为onCreate第一次触发
     */
    var isFirst = true;

    final override fun onCreate(savedInstanceState: Bundle?) {
        isFirst = true;
        super.onCreate(savedInstanceState)
        ActivityEx.procAct(this)
        if (ActivityEx.isKilled(this))
            return

        instAct[this.javaClass.name] = this;
        df.catchLog {

            createList.forEach { it() }

            onCreateEx()

            rootView = findViewById<View>(R.id.content)?.rootView
            if (rootView == null) {
                return
            }

            preDraw = ViewTreeObserver.OnPreDrawListener {
                // TODO Auto-generated method stub
                rootView!!.viewTreeObserver.removeOnPreDrawListener(preDraw)

                df.catchLog { onPreDraw() }

                true
            }
            rootView?.viewTreeObserver?.addOnPreDrawListener(preDraw)
        }
    }

    final override fun onDestroy() {
        instAct[this.javaClass.name] = null

        if (df.currentActivity === this)
            df.currentActivity = null
        if (isFinishing)
            ActivityEx.removeAct(this)

        super.onDestroy()

        df.catchLog { onDestoryEx() }

    }


    var isShow = false;
    final override fun onResume() {
        isShow = true;
        super.onResume()
        df.currentActivity = this

        df.catchLog { onResumeEx() }

        isFirst = false;
    }


    final override fun onPause() {
        isShow = false;
        super.onPause()
        df.catchLog { onPauseEx() }
    }

    final override fun onStop() {
        super.onStop()
        df.catchLog { onStopEx() }
    }


    internal var rootView: View? = null

    var preDraw: ViewTreeObserver.OnPreDrawListener? = null

//    /**
//     * 初始化intent参数中包含引用类型object（无法序列化，内存不足时会重启activity清理掉所有全局变量，只保留intentExtra）
//     *  为true时将在检测到object被清理掉后自动关闭activity
//     */
//    var hasObjectIntentPara = false

    val stringExtra = StringExtra(this)
    val shortExtra = ShortExtra(this)
    val intExtra = IntExtra(this)
    val longExtra = LongExtra(this)
    val floatExtra = FloatExtra(this)
    val doubleExtra = DoubleExtra(this)
}
