package net.rxaa.media

import android.graphics.Bitmap
import android.widget.ImageView
import net.rxaa.util.df
import java.io.File
import java.util.*


/**
 *  图片缓存
 */
class BmpBuffer(
    /**
     * 图片内存缓存个数
     */
    var bufferCount: Int = 10,

    /**
     * 自定义图片读取函数
     */
    customReadBitmap: ((menu: String, size: Int) -> Bitmap?)? = null
) {

    var readBitmapFunc = fun(menu: String, size: Int): Bitmap? {
        // 未命中
        var bmpBuffer = Pic.readBigBitmap(menu, size);

//        //旋转
        val degree = Pic.readPictureDegree(menu)
        if (degree > 0) {
            bmpBuffer = Pic.rotaingImageView(degree, bmpBuffer)
        }
        return bmpBuffer;
    }

    init {
        if (customReadBitmap != null) {
            readBitmapFunc = customReadBitmap;
        }
    }

    /**
     * 普通图片缓存
     */
    private val imgBuffer = WeakHashMap<String, Bitmap>()

    /**
     * 清空图片缓存
     */
    fun clear() {
        imgBuffer.clear()
    }

    inline fun get(
        url: String, cache: Boolean = true,
        showBigPic: Boolean = false,
        size: Int = 0,
        res: (Bitmap) -> Unit
    ): Boolean {

        val bmp = readBitmap(url, cache, showBigPic, size);
        if (bmp != null) {
            res(bmp)
            return (true)
        }
        return (false)
    }

    fun setImage(url: File, img: ImageView, cache: Boolean = true, showBigPic: Boolean = false) {
        setImage(url.toString(), img, cache, showBigPic);
    }

    fun setImage(url: File, img: ImageView, maxWidth: Float, cache: Boolean = true) {
        var w = df.dp2px(maxWidth)

        get(url.toString(), cache, false, w) {

            if (it.width < w)
                w = it.width
            val lay = img.layoutParams
            lay.width = w;
            lay.height = w * it.height / it.width
            img.layoutParams = lay
            img.setImageBitmap(it)
        }
    }

    fun setImage(url: String, img: ImageView, cache: Boolean = true, showBigPic: Boolean = false) {
        get(url, cache, showBigPic) {
            //        //旋转
//            val degree = Pic.readPictureDegree(url)
//            if (degree > 0) {
//                val draw = Pic.getRotateDrawable(it, degree.toFloat());
//                img.setImageDrawable(draw)
//            } else {
            img.setImageBitmap(it)
//            }
        }
    }

    fun setImageRound(url: String, img: ImageView, roundPx: Float, cache: Boolean = true) {
        get(url, cache) {
            img.setImageBitmap(Bmp.getRoundedCornerBitmap(it, roundPx))
        }
    }

    fun readBigPic(menu: String): Bitmap? {
        // 未命中
        var bmpBuffer = Pic.readOriBitmap(menu);
//        //旋转
        val degree = Pic.readPictureDegree(menu)
        if (degree > 0) {
            bmpBuffer = Pic.rotaingImageView(degree, bmpBuffer)
        }
        return bmpBuffer;
    }


    /**
     * 读取并处理图片
     * @param menu
     * *
     * @param imgView
     * *
     * @param round
     * *
     * @param buffer
     * *
     * @return
     */
    fun readBitmap(menu: String, buffer: Boolean, showBigPic: Boolean = false, size: Int): Bitmap? {

        var bmpBuffer = imgBuffer[menu]
        if (bmpBuffer != null) {
            return bmpBuffer;
        }

        // 未命中
        bmpBuffer = if (showBigPic)
            readBigPic(menu);
        else
            readBitmapFunc(menu, size);

        if (buffer && bmpBuffer != null) {
            if (imgBuffer.size > bufferCount)
                imgBuffer.clear()

            imgBuffer.put(menu, bmpBuffer)
        }

        return bmpBuffer
    }
}