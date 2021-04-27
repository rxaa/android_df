package net.rxaa.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import net.rxaa.util.*
import net.rxaa.ext.FileExt
import net.rxaa.ext.notNull
import net.rxaa.media.Pic
import java.io.Serializable
import java.util.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

open class ActivityEx : Activity() {

    open fun onCreateEx() {
    }

    open fun onDestoryEx() {
    }

    open fun onPauseEx() {
    }

    open fun onResumeEx() {
    }

    open fun onActivityResultEx(requestCode: Int, resultCode: Int, data: Intent?) {

    }

    /**
     * 界面初始化预先绘制回调(可以获取各个View高度)
     */
    open fun onPreDraw() {
    }


    fun <T : View> find(id: Int): T {
        return findViewById<T>(id) as T;
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)

        FileExt.catchLog { bindList.forEach { it() } }

    }

    open override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        onActivityResult(this, requestCode, resultCode, data);
        FileExt.catchLog { onActivityResultEx(requestCode, resultCode, data) }
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        FileExt.catchLog { bindList.forEach { it() } }
    }

    override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        super.setContentView(view, params)
        FileExt.catchLog { bindList.forEach { it() } }
    }

    fun getContext(): ActivityEx {
        return this
    }

    /**
     * 绑定的view
     */
    val bindList = ArrayList<() -> Unit>();

    val createList = ArrayList<() -> Unit>();


    /**
     * 延迟加载(在onCreate之后调用)
     */
    fun <T> create(func: () -> T): BindView<T> {
        return BindView(func, createList);
    }

    /**
     * 在setContentView之后调用,绑定view对象
     */
    fun <T> bind(func: () -> T): BindView<T> {
        return BindView(func, bindList);
    }


    /**
     * Set the Activity's content view to the given layout and return the associated binding.
     */
    fun <T> binding(resId: Int, func: (v: View) -> T): BindView<T> {
        return ActivityEx.binding(resId, this, func, createList)
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
    fun <T : View> T.render(func: T.() -> Unit): T {
        renderList[this] = {
            func(this)
        }
        return this
    }

    /**
     * 触发所有View的render函数
     */
    fun renderAll() {
        FileExt.catchLog { renderList.forEach { it.value() } }

    }


    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        onActivityRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    /**
     * 禁用:当内存不足被系统干掉时,自动关闭此actvity
     */
    fun disableKilled() {
        this.intent.putExtra(allowKilledStr, true);
    }

    final override fun onCreate(savedInstanceState: Bundle?) {
        isFirst = true;
        super.onCreate(savedInstanceState)
        procAct(this)
        if (isKilled(this))
            return
        FileExt.catchLog {
            createList.forEach { it() }

            onCreateEx()

            rootView = findViewById<View>(android.R.id.content)?.rootView
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
            removeAct(this)

        super.onDestroy()

        FileExt.catchLog { onDestoryEx() }

    }

    var isShow = false;
    var isFirst = true;
    final override fun onResume() {
        isShow = true;
        super.onResume()
        df.currentActivity = this

        FileExt.catchLog { onResumeEx() }

        isFirst = false;
    }


    final override fun onPause() {
        isShow = false
        super.onPause()
        FileExt.catchLog { onPauseEx() }
    }


    internal var rootView: View? = null

    var preDraw: ViewTreeObserver.OnPreDrawListener? = null


    companion object {

        val permissionCameraContinuation = SparseArray<Continuation<Boolean>>();

        private var permissionCode = 0;

        fun getReqCode(): Int {
            if (permissionCode >= 255) {
                permissionCode = 0;
            }
            return permissionCode++;
        }

        val actReqCode = 17654;
        val actResCode = 17653;
        val intentParaStr = "para_udu81276"
        val intentRetStr = "ret_udu83276"


        /**
         * 新建Intent并传参,func回调函数在setContentView之前调用,不要在此访问界面View
         */
        @JvmStatic
        inline fun <reified T : Activity> newIntent(
                cont: Context?,
                crossinline func: (T) -> Unit
        ): Intent {
            val inte = Intent(cont, T::class.java)
            addIntentPara(inte) { act ->
                func(act as T)
            }
            return inte
        }

        /**
         * Set the Activity's content view to the given layout and return the associated binding.
         */
        fun <T> binding(
                resId: Int,
                act: Activity,
                func: (v: View) -> T,
                createList: ArrayList<() -> Unit>
        ): BindView<T> {
            return BindView({
                val v = LayoutInflater.from(act).inflate(resId, null)
                act.setContentView(v)
                func(v)
            }, createList);
        }

        /**
         * 记录activity的初始化函数
         */
        internal val funMap = HashMap<Long, (Activity) -> Unit>()

        internal val paraStr = "startAct_para_udu89276"
        val allowKilledStr = "allow_killed_0ikj374h";


        fun onActivityRequestPermissionsResult(
                requestCode: Int,
                permissions: Array<out String>?,
                grantResults: IntArray
        ) {
            FileExt.catchLog {
                permissionCameraContinuation[requestCode].notNull {
                    permissionCameraContinuation.remove(requestCode)
                    it.resume(
                            grantResults.size > 0
                                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    )
                }
            }
        }


        fun onActivityResult(
                act: Activity, requestCode: Int,
                resultCode: Int, data: Intent?
        ) {

            if (requestCode == Pic.getFileTag) {
                if (data == null)
                    return;

                val res = Pic.getFileTable ?: return

                Pic.getFileTable = null

                if (resultCode != Activity.RESULT_OK)
                    return

                val uri = data.data

                val currentFilePath = RealPathUtil.getRealPathFromURI(act, uri)
                if (currentFilePath == null || currentFilePath!!.length < 1)
                    return

                FileExt.catchLog {
                    res.run(currentFilePath)
                }


                return
            }

            if (requestCode == Pic.cropFileTag) {
                val res = Pic.getFileTable ?: return

                Pic.getFileTable = null

                if (resultCode != Activity.RESULT_OK)
                    return

                FileExt.catchLog {
                    res.run(Pic.cropFile.toString())
                }

                return
            }

            if (requestCode == Pic.takePhotoTag) {
                val res = Pic.takePhotoFunc ?: return
                Pic.takePhotoFunc = null

                if (resultCode != Activity.RESULT_OK)
                    return

                FileExt.catchLog {
                    res.run(Pic.cameraFile.toString())
                }

                return
            }
        }


        @JvmStatic
        fun <T : Activity> createIntent(cont: Context, clas: Class<T>, func: Func1<T>): Intent {
            val inte = Intent(cont, clas)
            addIntentPara(inte) { act ->
                func.run(act as T)
            }
            return inte
        }

        @JvmStatic
        fun addIntentPara(intent: Intent, func: (Activity) -> Unit) {
            val para = intent.getLongExtra(paraStr, 0)

            // intent已有参数
            if (para != 0L) {
                val oldFunc = funMap[para]
                if (oldFunc == null) {
                    funMap.put(para, func)
                    return
                }

                funMap.put(para, { res ->
                    oldFunc(res)
                    func(res)
                })
                return
            }

            // 新参数
            val key = df.getID()
            funMap.put(key, func)
            intent.putExtra(paraStr, key)
        }

        /**
         * 判断Activity是否被强杀(内存不足等)

         * @param act
         * *
         * @return
         */
        @JvmStatic
        fun isKilled(act: Activity): Boolean {
            val para = act.intent.getLongExtra(paraStr, 0)
            if (para == 0L)
                return false

            if (funMap[para] == null) {
                df.currentActivity = null
                act.finish()
                return true
            }
            return false
        }

        @JvmStatic
        fun procAct(act: Activity) {
            df.currentActivity = act;
            val para = act.intent.getLongExtra(paraStr, 0)
            if (para == 0L)
                return

            val res = funMap[para]
            if (res != null) {
                FileExt.catchLog { res(act) }
            }

            if (act.intent.getBooleanExtra(allowKilledStr, false))
                removeAct(act)
        }

        @JvmStatic
        fun removeAct(act: Activity) {
            val para = act.intent.getLongExtra(paraStr, 0)
            if (para != 0L) {
                funMap.remove(para)
                act.intent.removeExtra(paraStr)
            }
        }


    }
}