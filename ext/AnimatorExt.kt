package net.rxaa.ext

import android.animation.ObjectAnimator
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Transformation

fun View.animator(timeMill: Long = 500, autoStart: Boolean = true, func: Animator.() -> Unit): Animator {
    val a = Animator(this, autoStart, timeMill);
    func(a);
    return a
}


fun ObjectAnimator.start(delay: Long) {
    this.startDelay = delay
    this.start()
}


class Animator(val vi: View, val autoStart: Boolean = true, val timeMill: Long = 500) {
    var wrap: WrapperView? = null

    fun translationX(toX: Float, time: Long = timeMill, start: Boolean = autoStart, onOk: (anim: ObjectAnimator) -> Unit = {}): ObjectAnimator {

        return _add("translationX", vi.translationX, toX, time, start, onOk)
    }

    fun translationY(toY: Float, time: Long = timeMill, start: Boolean = autoStart, onOk: (anim: ObjectAnimator) -> Unit = {}): ObjectAnimator {
        return _add("translationY", vi.translationY, toY, time, start, onOk)
    }

    fun rotation(toY: Float, time: Long = timeMill, start: Boolean = autoStart, onOk: (anim: ObjectAnimator) -> Unit = {}): ObjectAnimator {
        return _add("rotation", vi.rotation, toY, time, start, onOk)
    }

    fun alpha(toY: Float, time: Long = timeMill, start: Boolean = autoStart, onOk: (anim: ObjectAnimator) -> Unit = {}): ObjectAnimator {
        return _add("alpha", vi.alpha, toY, time, start, onOk)
    }

    fun scaleY(toY: Float, time: Long = timeMill, start: Boolean = autoStart, onOk: (anim: ObjectAnimator) -> Unit = {}): ObjectAnimator {
        return _add("scaleY", vi.scaleY, toY, time, start, onOk)
    }


    fun scaleX(toY: Float, time: Long = timeMill, start: Boolean = autoStart, onOk: (anim: ObjectAnimator) -> Unit = {}): ObjectAnimator {
        return _add("scaleX", vi.scaleX, toY, time, start, onOk)
    }

    fun width(toY: Int, time: Long = timeMill, auto: Boolean = autoStart, onOk: (anim: Animation) -> Unit = {}): Animation {
        val start = vi.width
        val offset = toY - start;
        val a = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                vi.layoutParams.width = start + (offset * interpolatedTime).toInt()
                vi.requestLayout()
            }
        }
        a.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationEnd(animation: Animation?) {
                FileExt.catchLog { onOk(a) }
            }

            override fun onAnimationStart(animation: Animation?) {
            }

            override fun onAnimationRepeat(animation: Animation?) {
            }
        })


        a.duration = time;
        if (auto)
            vi.startAnimation(a);
        return a;
    }


    fun top(toY: Int, time: Long = timeMill, start: Boolean = autoStart, onOk: (anim: ObjectAnimator) -> Unit = {}): ObjectAnimator {
        return _add("top", vi.top, toY, time, start, onOk)
    }

    fun y(toY: Float, time: Long = timeMill, start: Boolean = autoStart, onOk: (anim: ObjectAnimator) -> Unit = {}): ObjectAnimator {
        return _add("y", vi.getY(), toY, time, start, onOk)
    }

    fun height(toY: Int, time: Long = timeMill, auto: Boolean = autoStart, onOk: (anim: Animation) -> Unit = {}): Animation {
        val start = vi.height
        val offset = toY - start;
        val a = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                vi.layoutParams.height = start + (offset * interpolatedTime).toInt()
                vi.requestLayout()
            }
        }
        a.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationEnd(animation: Animation?) {
                FileExt.catchLog { onOk(a) }
            }

            override fun onAnimationStart(animation: Animation?) {
            }

            override fun onAnimationRepeat(animation: Animation?) {
            }
        })


        a.duration = time;
        if (auto)
            vi.startAnimation(a);
        return a;
    }

    fun topMargin(toTop: Int, time: Long = timeMill, auto: Boolean = autoStart, onOk: (anim: Animation) -> Unit = {}): Animation {
        val start = (vi.layoutParams as ViewGroup.MarginLayoutParams).topMargin;
        val offset = toTop - start;
        val a = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                (vi.layoutParams as ViewGroup.MarginLayoutParams).topMargin = start + (offset * interpolatedTime).toInt()
                vi.requestLayout()
            }
        }

        a.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationEnd(animation: Animation?) {
                FileExt.catchLog { onOk(a) }
            }

            override fun onAnimationStart(animation: Animation?) {
            }

            override fun onAnimationRepeat(animation: Animation?) {
            }
        })


        a.duration = time;
        if (auto)
            vi.startAnimation(a);
        return a;
    }

    private fun _add(member: String, fromX: Int, toX: Int, time: Long = timeMill, start: Boolean = autoStart, onOk: (anim: ObjectAnimator) -> Unit = {}): ObjectAnimator {
        val animSet = ObjectAnimator.ofInt(vi, member, fromX, toX);
        animSet.duration = time;
        animSet.addListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: android.animation.Animator?) {

            }

            override fun onAnimationEnd(animation: android.animation.Animator?) {
                onOk(animSet)
            }

            override fun onAnimationCancel(animation: android.animation.Animator?) {
            }

            override fun onAnimationStart(animation: android.animation.Animator?) {
            }
        })
        if (start)
            animSet.start();
        return animSet;
    }

    private fun _add(member: String, fromX: Float, toX: Float, time: Long = timeMill, start: Boolean = autoStart, onOk: (anim: ObjectAnimator) -> Unit = {}): ObjectAnimator {
        val animSet = ObjectAnimator.ofFloat(vi, member, fromX, toX);
        animSet.duration = time;
        animSet.addListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: android.animation.Animator?) {

            }

            override fun onAnimationEnd(animation: android.animation.Animator?) {
                onOk(animSet)
            }

            override fun onAnimationCancel(animation: android.animation.Animator?) {
            }

            override fun onAnimationStart(animation: android.animation.Animator?) {
            }
        })
        if (start)
            animSet.start();
        return animSet;
    }

}

class WrapperView(val mTarget: View) {
    fun getWidth(): Int {
        return mTarget.layoutParams.width;
    }

    fun setTopmargin(topmargin: Int) {
        (mTarget.layoutParams as ViewGroup.MarginLayoutParams).topMargin = topmargin
        mTarget.requestLayout()
    }

    fun getTopmargin(): Int {
        return (mTarget.layoutParams as ViewGroup.MarginLayoutParams).topMargin;
    }

    fun setWidth(width: Int) {
        mTarget.layoutParams.width = width;
        mTarget.requestLayout();
    }

    fun getHeight(): Int {
        return mTarget.layoutParams.height;
    }

    fun setHeight(height: Int) {
        mTarget.layoutParams.height = height;
        mTarget.requestLayout();
    }
}