package net.rxaa.view

import android.animation.Animator
import android.animation.ObjectAnimator
import android.view.View

interface BaseAnimation {

    fun getAnimators(view: View): Array<Animator>

}
/**
 * https://github.com/CymChad/BaseRecyclerViewAdapterHelper
 */
class AlphaInAnimation @JvmOverloads constructor(private val mFrom: Float = DEFAULT_ALPHA_FROM) :
    BaseAnimation {

    override fun getAnimators(view: View): Array<Animator> {
        return arrayOf(ObjectAnimator.ofFloat(view, "alpha", mFrom, 1f))
    }

    companion object {

        private const val DEFAULT_ALPHA_FROM = 0f
    }
}

/**
 * https://github.com/CymChad/BaseRecyclerViewAdapterHelper
 */
class ScaleInAnimation @JvmOverloads constructor(private val mFrom: Float = DEFAULT_SCALE_FROM) :
    BaseAnimation {

    override fun getAnimators(view: View): Array<Animator> {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", mFrom, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", mFrom, 1f)
        return arrayOf(scaleX, scaleY)
    }

    companion object {

        private const val DEFAULT_SCALE_FROM = .5f
    }
}


class SlideInBottomAnimation : BaseAnimation {


    override fun getAnimators(view: View): Array<Animator> {
        return arrayOf(
            ObjectAnimator.ofFloat(
                view,
                "translationY",
                view.measuredHeight.toFloat(),
                0f
            )
        )
    }
}

class SlideInLeftAnimation : BaseAnimation {


    override fun getAnimators(view: View): Array<Animator> {
        return arrayOf(
            ObjectAnimator.ofFloat(
                view,
                "translationX",
                -view.rootView.width.toFloat(),
                0f
            )
        )
    }
}

class SlideInRightAnimation : BaseAnimation {


    override fun getAnimators(view: View): Array<Animator> {
        return arrayOf(
            ObjectAnimator.ofFloat(
                view,
                "translationX",
                view.rootView.width.toFloat(),
                0f
            )
        )
    }
}
