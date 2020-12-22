package net.rxaa.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Handler
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import net.rxaa.util.df
import net.rxaa.ext.FileExt
import java.util.*

class ViewPageEx : ViewPager {

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        // TODO Auto-generated constructor stub

    }


    constructor(context: Context) : super(context) {
        // TODO Auto-generated constructor stub
    }

    val count: Int
        get() = viewList.size

    private val viewList = ArrayList<View>()
    private val viewSelectList = ArrayList<() -> Unit>()
    private var radio: RadioGroup? = null
    private var radioId = ArrayList<Int>()
    private var radioView = ArrayList<RadioButton>()
    // 页切换事件
    var onPageSelect: ((index: Int) -> Unit)? = null
    private var onPageScrol: OnPageScrolled? = null
    private var onPageStateChanged: OnPageScrollStateChanged? = null

    // RadioGroup底部的滑块
    private var slideView: View? = null
    var currentIndex = 0
        private set

    /**
     * 自动播放时间
     */
    var autoPlayTtime = 0L

    /**
     * 无限滚动
     */
    var infinitySlide = false

    fun setInfinity(): ViewPageEx {
        infinitySlide = true
        return this
    }

    fun setAutoPlay(millTimes: Long): ViewPageEx {
        autoPlayTtime = millTimes
        return this
    }

    val lastRadio: RadioButton
        get() = radioView[radioView.size - 1]

    fun getView(index: Int): View {
        return viewList[index]
    }

    @JvmOverloads
    fun addViewId(id: Int, onSelected: () -> Unit = {}): View {
        val v = df.createView(context, id)
        viewList.add(v)
        viewSelectList.add(onSelected)
        init()
        return v
    }

    @JvmOverloads
    fun addListView(cont: Context, onSelected: () -> Unit = {}): ListView {
        val lv = newListView(cont)
        viewList.add(lv)
        viewSelectList.add(onSelected)
        init()
        return lv
    }

    @JvmOverloads
    fun addRecyclerView(cont: Context, onSelected: () -> Unit = {}): RecyclerView {
        val lv = RecyclerView(cont)
        viewList.add(lv)
        viewSelectList.add(onSelected)
        init()
        return lv
    }

    fun clear() {
        viewList.clear()
    }

    @JvmOverloads
    fun addViewByVi(v: View, onSelected: () -> Unit = {}): ViewPageEx {
        viewList.add(v)
        viewSelectList.add(onSelected)
        init()
        return this
    }


    /**
     * 触发current view onSelected事件
     */
    fun doSelect() {
        if (currentIndex < viewSelectList.size) {
            viewSelectList[currentIndex]();
        }
    }

    interface OnPageScrolled {
        @Throws(Exception::class)
        fun run(index: Int, pos: Float, arg2: Int)
    }


    interface OnPageScrollStateChanged {
        @Throws(Exception::class)
        fun run(state: Int)
    }

    override fun canScroll(v: View, checkV: Boolean, dx: Int, x: Int, y: Int): Boolean {
        if (v !== this && v is ViewPager) {
            return true
        }
        return super.canScroll(v, checkV, dx, x, y)
    }

    var onLongClick: (() -> Unit)? = null
    internal var clickX = 0f
    internal var clickY = 0f
    internal var clickTime: Long = 0

    val longClickTime = 600L
    var longPressRun: Runnable? = null;

    fun cancelLongPressRun() {
        clickTime = 0;
        df.removeOnUi(longPressRun);
    }

    override fun onInterceptTouchEvent(arg0: MotionEvent): Boolean {


        if (arg0.action == MotionEvent.ACTION_DOWN) {
            clickX = arg0.x
            clickY = arg0.y

            clickTime = System.currentTimeMillis()
            hand.removeCallbacks(roolAct)

            if (onLongClick != null) {
                longPressRun = df.runOnUi(longClickTime) {
                    if (clickTime > 0 && System.currentTimeMillis() - clickTime > longClickTime) {
                        if (context is Activity && (context as Activity).isFinishing) {

                        } else {
                            onLongClick!!()
                        }

                        cancelLongPressRun();
                    }
                }
            }

        } else if (arg0.action == MotionEvent.ACTION_MOVE) {
            if (df.px2dp(Math.abs(arg0.x - clickX)) > 3 || df.px2dp(Math.abs(arg0.y - clickY)) > 3)
                cancelLongPressRun();
        } else if (arg0.action == MotionEvent.ACTION_UP || arg0.action == MotionEvent.ACTION_CANCEL) {
            startAutoPlay()
            cancelLongPressRun();
        }
        return super.onInterceptTouchEvent(arg0)
    }

    override fun onTouchEvent(arg0: MotionEvent): Boolean {
        if (arg0.action == MotionEvent.ACTION_DOWN) {
            hand.removeCallbacks(roolAct)
        } else if (arg0.action == MotionEvent.ACTION_MOVE) {
            if (df.px2dp(Math.abs(arg0.x - clickX)) > 3 || df.px2dp(Math.abs(arg0.y - clickY)) > 3)
                cancelLongPressRun();
        } else if (arg0.action == MotionEvent.ACTION_UP || arg0.action == MotionEvent.ACTION_CANCEL) {
            startAutoPlay()
            cancelLongPressRun();
        }
        return super.onTouchEvent(arg0)
    }

    internal fun startAutoPlay() {
        if (autoPlayTtime > 0) {
            hand.removeCallbacks(roolAct)
            hand.postDelayed(roolAct, autoPlayTtime.toLong())
        }
    }

    override fun getCurrentItem(): Int {
        return currentIndex
    }

    override fun setCurrentItem(item: Int, smooth: Boolean) {
        if (adapter == null) {
            currentIndex = item
            return
        }
        var item = item

        if (infinitySlide && viewList.size > 1)
            item++

        super.setCurrentItem(item, smooth)
    }

    override fun setCurrentItem(item: Int) {
        setCurrentItem(item, true)
    }

    internal var hand = Handler()
    internal var roolAct: Runnable = Runnable {
        // TODO Auto-generated method stub
        if (count == 0)
            return@Runnable

        currentItem = (currentIndex + 1) % count
    }


    /**
     * 添加完所有view之后调用
     */
    private var noInit = true

    private fun init() {
        if (noInit) {
            noInit = false
            df.runOnUi {
                addOnPageChangeListener(onChange)
                adapter = pagerAdapter
                startAutoPlay()

                if (infinitySlide && viewList.size > 1) {
                    currentItem = currentIndex
                } else if (currentIndex == 0) {
                    doPageSelected(0)
                } else {
                    currentItem = currentIndex
                }

            }
        }
    }


    /**
     * 此方法会覆盖RadioGroup的OnCheckedChangeListener事件

     * @param rg 关联的RadioGroup(RadioButton数量要与view数量一致)
     * *
     * @param sv RadioGroup底部的滑块，不需要则传null
     */
    @JvmOverloads
    fun setRadio(rg: RadioGroup, sv: View? = null): ViewPageEx {
        radioId.clear()
        radioView.clear()

        radio = rg
        for (i in 0..radio!!.childCount - 1) {
            val v = radio!!.getChildAt(i)
            if (v is RadioButton) {
                radioId.add(v.getId())
                radioView.add(v)
            }
        }

        radio!!.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { arg0, arg1 ->
            // TODO Auto-generated method stub

            for (i in radioId.indices) {
                if (arg1 == radioId[i]) {

                    currentItem = i
                    return@OnCheckedChangeListener
                }
            }
        })

        slideView = sv

        init()
        return this
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (slideView != null) {
            slideView!!.post {
                // TODO Auto-generated method stub
                setImgPos(currentIndex, 0f)
            }
        }
    }

    private fun doPageSelected(i: Int) {
        FileExt.catchLog {
            if (viewSelectList.size == viewList.size)
                viewSelectList[i]();
            if (onPageSelect != null) {
                onPageSelect!!(i)
            }
        }
        if (i >= radioView.size)
            return

        radioView[i].isChecked = true
    }


    internal var onChange: OnPageChangeListener = object : OnPageChangeListener {

        override fun onPageSelected(arg0: Int) {
            var arg0 = arg0
            // TODO Auto-generated method stub

            if (infinitySlide && viewList.size > 1) {
                if (arg0 == 0) {
                    post { setCurrentItem(count - 1, false) }

                } else if (arg0 == viewList.size + 1) {
                    post { setCurrentItem(0, false) }
                }

                arg0--
                if (arg0 < 0)
                // arg0 = viewList.size() - 1;
                    return
                if (arg0 >= viewList.size)
                // arg0 = 0;
                    return
            }

            currentIndex = arg0

            doPageSelected(arg0)

            startAutoPlay()


        }

        override fun onPageScrolled(arg0: Int, arg1: Float, arg2: Int) {
            // TODO Auto-generated method stub

            setImgPos(arg0, arg1)
            if (onPageScrol != null) {
                try {
                    onPageScrol!!.run(arg0, arg1, arg2)
                } catch (e: Throwable) {
                    // TODO Auto-generated catch block
                    FileExt.logException(e, true)
                }

            }

        }

        override fun onPageScrollStateChanged(arg0: Int) {
            // TODO Auto-generated method stub

            if (onPageStateChanged != null) {
                try {
                    onPageStateChanged!!.run(arg0)
                } catch (e: Throwable) {
                    // TODO Auto-generated catch block
                    FileExt.logException(e, true)
                }

            }
        }
    }

    @SuppressLint("NewApi")
    internal fun setImgPos(i: Int, per: Float) {
        if (radio == null || slideView == null)
            return

        val allW = radio!!.width
        if (allW == 0)
            return

        val pW = slideView!!.width

        val avgW = allW / radioId.size
        val left = (avgW - pW) / 2
        val pos = (left.toFloat() + (avgW * i).toFloat() + avgW * per).toInt() + radio!!.left

        if (Build.VERSION.SDK_INT >= 11) {
            slideView!!.x = pos.toFloat()
        } else {

            val olp = slideView!!.layoutParams as MarginLayoutParams
            olp.leftMargin = pos
            slideView!!.layoutParams = olp

        }

    }

    internal var pagerAdapter: PagerAdapter = object : PagerAdapter() {

        override fun isViewFromObject(arg0: View, arg1: Any): Boolean {

            return arg0 === arg1
        }

        override fun getCount(): Int {

            if (infinitySlide && viewList.size > 1)
                return viewList.size + 2
            return viewList.size
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {

            if (infinitySlide && viewList.size > 1) {
                //				if (position == 0)
                //					// container.removeView(viewList.get(0));
                //					;
                //				else if (position == viewList.size() + 1)
                //					// container.removeView(viewList.get(viewList.size() - 1));
                //					;
                //				else
                //					container.removeView(viewList.get(position - 1));
            } else
                container.removeView(viewList[position])
        }


        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            var position = position

            if (infinitySlide && viewList.size > 1) {

                if (position == 0)
                    position = viewList.size - 1
                else if (position == viewList.size + 1)
                    position = 0
                else
                    position--


            }

            val v = viewList[position]

            container.removeView(v)
            container.addView(v)

            return v
        }

    }

    companion object {

        fun newListView(cont: Context): ListView {
            val lv = ListView(cont)
            lv.dividerHeight = 0
            lv.divider = null
            return lv
        }
    }

}
