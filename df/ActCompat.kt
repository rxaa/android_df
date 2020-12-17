package rxaa.df

import android.R
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.appcompat.app.AppCompatActivity
import java.util.*


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

    final override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        ActivityEx.onActivityResult(this, requestCode, resultCode, data);
        FileExt.catchLog { onActivityResultEx(requestCode, resultCode, data) }
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)

        FileExt.catchLog { bindList.forEach { it() } }

    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        FileExt.catchLog { bindList.forEach { it() } }
    }

    override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        super.setContentView(view, params)
        FileExt.catchLog { bindList.forEach { it() } }
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
     * Set the Activity's content view to the given layout and return the associated binding.
     */
    inline fun <reified T> dataBinding(resId: Int): BindView<T> {

        return BindView({

            val v = LayoutInflater.from(getContext()).inflate(resId, null)
            setContentView(v)
            //为同时兼容viewBinding与dataBinding，这里反射获取bind方法
            val m = T::class.java.getDeclaredMethod("bind", View::class.java)
            m.invoke(null, v) as T

//            val m = v.getDeclaredMethod("inflate", LayoutInflater::class.java)
//            val b = m.invoke(null, layoutInflater) as T;
//
//            val getR = v.methods
//            for (method in getR) {
//                if (method.returnType == View::class.java) {
//                    val v = method.invoke(b) as View
//                    setContentView(v)
//                    break;
//                }
//            }

//            if (getR[4].returnType == View::class.java) {
//
//            }
//            val rm = v.getMethod("getRoot", View::class.java);
//            val vi = b.root
//
//
//            setContentView(vi)
            //b
            // DataBindingUtil.setContentView<T>(this, resId)!!
        }, createList);
    }


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

    var bundleOnCreate: Bundle? = null

    final override fun onCreate(savedInstanceState: Bundle?) {
        bundleOnCreate = savedInstanceState;
        isFirst = true;
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
