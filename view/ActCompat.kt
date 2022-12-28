package net.rxaa.view

import android.R
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import net.rxaa.util.*
import net.rxaa.ext.FileExt
import java.io.Serializable
import java.util.*


class ActCompatCompanion<ParaT, RetT> {
    fun newIntent(para: ParaT, func: (res: RetT) -> Unit) {

    }
}

/**
 *  公共基activity
 *  ParaT, RetT 分别表示入参类型与返回值类型
 */
@Suppress("UNCHECKED_CAST")
open class ActCompat<ParaT : Serializable, RetT : Serializable> : AppCompatActivity() {


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
     *  在当前线程启动协程
     */
    fun launch(func: suspend () -> Unit) {
        df.launchMain(func)
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

    fun <T : View> find(id: Int): T {
        return findViewById<T>(id) as T;
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        ActivityEx.onActivityRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        ActivityEx.onActivityResult(this, requestCode, resultCode, data);
        FileExt.catchLog { onActivityResultEx(requestCode, resultCode, data) }
    }


    fun getContext(): ActCompat<ParaT, RetT> {
        return this
    }


    val createList = ArrayList<() -> Unit>();


    /**
     * Set the Activity's content view to the given layout and return the associated binding.
     */
    fun <T> binding(resId: Int, func: (v: View) -> T): BindView<T> {
        return ActivityEx.binding(resId, this, func, createList)
    }


    /**
     * 延迟加载(在onCreate之后调用)
     */
    fun <T> create(func: () -> T): BindView<T> {
        return BindView(func, createList);
    }


    /**
     * 禁用:当内存不足被系统干掉时,自动关闭此actvity
     */
    fun disableKilled() {
        this.intent.putExtra(ActivityEx.allowKilledStr, true);
    }

    /**
     * 获取activity 入参
     */
    val para: ParaT?
        get() {
            return intent.getSerializableExtra(ActivityEx.intentParaStr) as ParaT?
        }


    /**
     * 设置activity onResult的返回值
     */
    private var _result: RetT? = null
    var result: RetT?
        get() {
            return _result
        }
        set(value) {
            _result = value
            if (value != null) {
                val intent = Intent()
                intent.putExtra(ActivityEx.intentRetStr, value as Serializable)
                setResult(ActivityEx.actResCode, intent)
            } else {
                setResult(ActivityEx.actResCode, null)
            }
        }

    /**
     * 是否为onCreate第一次触发
     */
    var isFirst = true

    var bundleOnCreate: Bundle? = null

    final override fun onCreate(savedInstanceState: Bundle?) {
        bundleOnCreate = savedInstanceState
        isFirst = true
        //启动转场动画
        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        super.onCreate(savedInstanceState)
        ActivityEx.procAct(this)
        if (ActivityEx.isKilled(this))
            return


        FileExt.catchLog {

            createList.forEach { it() }

            onCreateEx()

            rootView = findViewById<View>(R.id.content)?.rootView
            if (rootView == null) {
                return
            }

            preDraw = ViewTreeObserver.OnPreDrawListener {
                // TODO Auto-generated method stub
                rootView!!.viewTreeObserver.removeOnPreDrawListener(preDraw)

                FileExt.catchLog { onPreDraw() }

                true
            }
            rootView?.viewTreeObserver?.addOnPreDrawListener(preDraw)
        }
    }

    final override fun onDestroy() {

        if (df.currentActivity === this)
            df.currentActivity = null
        if (isFinishing)
            ActivityEx.removeAct(this)

        super.onDestroy()

        FileExt.catchLog { onDestoryEx() }

    }

    var isShow = false;
    final override fun onResume() {
        isShow = true;
        super.onResume()
        df.currentActivity = this

        FileExt.catchLog { onResumeEx() }

        isFirst = false;
    }


    final override fun onPause() {
        isShow = false;
        super.onPause()
        FileExt.catchLog { onPauseEx() }
    }

    final override fun onStop() {
        super.onStop()
        FileExt.catchLog { onStopEx() }
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
