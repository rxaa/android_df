package rxaa.df

import android.graphics.Bitmap
import android.widget.ImageView
import java.io.File
import java.util.*


class BmpBuffer(
        /**
         * 图片内存缓存个数
         */
        var bufferCount: Int = 20,

        /**
         * 自定义图片读取函数
         */
        customReadBitmap: ((menu: String) -> Bitmap?)? = null
) {

    var readBitmapFunc = fun(menu: String): Bitmap? {
        // 未命中
        var bmpBuffer = Pic.readBigBitmap(menu);

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

    inline fun get(url: String, cache: Boolean = true, res: (Bitmap) -> Unit): Boolean {
        val bmp = readBitmap(url, cache);
        if (bmp != null) {
            res(bmp)
            return (true)
        }
        return (false)
    }

    fun setImage(url: File, img: ImageView, cache: Boolean = true) {
        setImage(url.toString(), img, cache);
    }

    fun setImage(url: String, img: ImageView, cache: Boolean = true) {
        get(url, cache) {
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
    fun readBitmap(menu: String, buffer: Boolean): Bitmap? {

        var bmpBuffer = imgBuffer[menu]
        if (bmpBuffer != null) {
            return bmpBuffer;
        }

        // 未命中
        bmpBuffer = readBitmapFunc(menu);

        if (buffer && bmpBuffer != null) {
            if (imgBuffer.size > bufferCount)
                imgBuffer.clear()

            imgBuffer.put(menu, bmpBuffer)
        }

        return bmpBuffer
    }
}