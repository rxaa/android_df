package net.rxaa.media

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.ImageView
import androidx.core.content.ContextCompat
import net.rxaa.ext.FileExt
import net.rxaa.ext.plus
import net.rxaa.util.Func1
import net.rxaa.util.MsgException
import net.rxaa.util.df
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import kotlin.collections.HashMap
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object Pic {

    val animDrawableBuf = HashMap<String, ArrayList<Drawable>>()

    /**
     * 新建360度旋转动画
     */
    fun new360AnimationDrawable(id: Int, count: Int): AnimationDrawable? {
        val key = "" + id + "_" + count;
        val arr = animDrawableBuf.get(key) ?: ArrayList<Drawable>()


        if (arr.size < count) {
            val loadPic = (ContextCompat.getDrawable(
                    df.appContext!!,
                    id
            ) as BitmapDrawable).bitmap

            for (i in 0..count) {
                arr.add(getRotateDrawable(loadPic, (i * (360.0 / count)).toFloat()))
            }

            animDrawableBuf.set(key, arr)
        }

        val anim = AnimationDrawable()

        arr.forEach {
            anim.addFrame(it, 25)
        }
        anim.start()
        return anim
    }

    fun rotatDrawable(drawable: Drawable, angle: Float): Drawable {
        //创建一个Matrix对象
        val matrix = Matrix()
        //由darwable创建一个bitmap对象
        var bitmap = (drawable as BitmapDrawable).bitmap
        //设置旋转角度
        matrix.setRotate(angle, (bitmap.width / 2).toFloat(), (bitmap.height / 2).toFloat())
        //以bitmap跟matrix一起创建一个新的旋转以后的bitmap
        bitmap = Bitmap.createBitmap(
                bitmap, 0, 0,
                bitmap.width,
                bitmap.height,
                matrix, true
        )
        //bitmap转化为drawable对象
        return BitmapDrawable(df.appContext!!.resources, bitmap)
    }

    fun getRotateDrawable(b: Bitmap, angle: Float): Drawable {
        val drawable = object : BitmapDrawable(df.appContext!!.resources, b) {
            override fun draw(canvas: Canvas) {
                canvas.save()
                val w = canvas.clipBounds.right - canvas.clipBounds.left
                val h = canvas.clipBounds.bottom - canvas.clipBounds.top
                canvas.rotate(
                        angle,
                        w.toFloat() / 2,
                        (h.toFloat() / 2)
                )
                super.draw(canvas)
                canvas.restore()
            }


        }
        return drawable
    }

    /*
   * 旋转图片
   * @param angle
   * @param bitmap
   * @return Bitmap
   */
    @JvmStatic
    fun rotaingImageView(angle: Int, bitmap: Bitmap?): Bitmap? {
        if (bitmap == null)
            return null;
        //旋转图片 动作
        val matrix = android.graphics.Matrix()
        matrix.postRotate(angle.toFloat())
        // 创建新的图片
        val resizedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0,
                bitmap.width, bitmap.height, matrix, true
        )
        return resizedBitmap
    }

    /**
     * 读取图片Orientation属性

     * @param path 图片绝对路径
     * *
     * @return degree旋转的角度
     */
    @JvmStatic
    fun readPictureOrientation(path: String): Int {
        try {
            val exifInterface = ExifInterface(path)
            val orientation =
                    exifInterface.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                    )
            return orientation
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return 0
    }

    /**
     * 读取图片属性：旋转的角度

     * @param path 图片绝对路径
     * *
     * @return degree旋转的角度
     */
    @JvmStatic
    fun readPictureDegree(path: String): Int {
        var degree = 0
        try {
            val exifInterface = ExifInterface(path)
            val orientation =
                    exifInterface.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                    )
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> degree = 90
                ExifInterface.ORIENTATION_ROTATE_180 -> degree = 180
                ExifInterface.ORIENTATION_ROTATE_270 -> degree = 270
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return degree
    }

    @JvmStatic
    fun showBigPic(pic: File, iv: ImageView) {
        showBigPic(pic.toString(), iv)
    }

    @JvmStatic
    fun showBigPic(pic: String, iv: ImageView) {
        try {
            val degree = readPictureDegree(pic)
            if (degree > 0) {
                var bmp = readBigBitmap(pic)
                bmp = rotaingImageView(degree, bmp)
                iv.setImageBitmap(bmp)
            } else
                iv.setImageBitmap(readBigBitmap(pic))
        } catch (e: Throwable) {
            df.msg("内存不足,图片读取失败!")
            FileExt.logException(e, false)
        }

    }

    /**
     * 将图片添加到相册
     */
    @JvmStatic
    fun scanPhoto(ctx: Context, imgFileName: String) {
        val mediaScanIntent = Intent(
                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE
        )
        val file = File(imgFileName)
        val contentUri = Uri.fromFile(file)
        mediaScanIntent.data = contentUri
        ctx.sendBroadcast(mediaScanIntent)
    }

    /**
     * 获取视频第一帧
     */
    fun getVideoFirstFrame(file: String): Bitmap? {
        val meda = MediaMetadataRetriever();
        meda.setDataSource(file);
        return meda.frameAtTime;
    }

    /**
     * 自动压缩图片
     * @param from
     * *
     * @param to
     */
    @JvmStatic
    @JvmOverloads
    fun compressAuto(from: String, to: String, quality: Int = 70): Bitmap? {
        var bmp: Bitmap? = null
        try {
            val degree = readPictureDegree(from)
            if (degree > 0) {
                bmp = readBigBitmap(from)
                bmp = rotaingImageView(degree, bmp)
            } else
                bmp = readBigBitmap(from)
        } catch (e: Exception) {
        }

        if (bmp == null) {
            throw MsgException("内存不足,图片读取失败!")
        }
        compressAndSaveBitmapToSDCard(bmp, to, quality)
        return bmp;
    }


    /**
     * 获取屏幕长高像素

     * @return
     */
    val displaySize: IntArray
        get() {
            val dm = df.appContext!!.resources.displayMetrics
            return intArrayOf(dm.widthPixels, dm.heightPixels)
        }

    /**
     * 读取资源id为32位ARGB位图

     * @param resId
     * *
     * @return
     */
    @JvmStatic
    @JvmOverloads
    fun readBitmap(resId: Int, cfg: Bitmap.Config = Bitmap.Config.ARGB_8888): Bitmap? {
        try {
            val opt = BitmapFactory.Options()
            opt.inPreferredConfig = cfg
            // 获取资源图片
            val `is` = df.appContext!!.resources.openRawResource(
                    resId
            )
            return BitmapFactory.decodeStream(`is`, null, opt)
        } catch (e: Throwable) {
            // TODO Auto-generated catch block

        }

        return null
    }


    /**
     * 读取图片的长宽像素

     * @param url 图片路径
     * *
     * @return
     */
    fun readBmpSize(url: String): BitmapFactory.Options {
        val bmpFactoryOptions = BitmapFactory.Options()
        bmpFactoryOptions.inJustDecodeBounds = true
        try {
            BitmapFactory.decodeFile(url, bmpFactoryOptions)
            return bmpFactoryOptions

        } catch (e: Throwable) {
            // TODO Auto-generated catch block
        }

        return bmpFactoryOptions
    }

    fun readBmpSize(id: Int): BitmapFactory.Options {
        val bmpFactoryOptions = BitmapFactory.Options()
        bmpFactoryOptions.inJustDecodeBounds = true
        try {

            BitmapFactory.decodeResource(df.appContext!!.resources, id, bmpFactoryOptions)
            return bmpFactoryOptions

        } catch (e: Throwable) {
            // TODO Auto-generated catch block
        }

        return bmpFactoryOptions
    }

    @JvmStatic
    fun getMinVal(v1: Int, v2: Int): Int {
        return if (v1 < v2) v1 else v2
    }

    @JvmStatic
    fun getMinVal(list: IntArray): Int {
        var `val` = list[0]
        for (i in 1..list.size - 1) {
            if (list[i] < `val`)
                `val` = list[i]
        }

        return `val`
    }


    //显示原始大图
    fun readOriBitmap(url: String): Bitmap? {
        try {
            val type = readBmpSize(url)

            var i = 1
            for (outCount in 0..6) {
                try {
                    val options = BitmapFactory.Options()
                    options.inSampleSize = i
                    if (type.outMimeType.contains("jpeg"))
                        options.inPreferredConfig = Bitmap.Config.RGB_565
                    else
                        options.inPreferredConfig = Bitmap.Config.ARGB_8888
                    val bmp = BitmapFactory.decodeFile(url, options)
                    return bmp
                } catch (e: OutOfMemoryError) {
                    i *= 2
                }

            }
        } catch (e: Throwable) {
            // TODO Auto-generated catch block

        }

        return null
    }

    /**
     *  读取大图，并适量压缩以减少内存占用
     *  minSize： 图片压缩最小像素大小
     */
    @JvmStatic
    @JvmOverloads
    fun readBigBitmap(resId: Int, minSize: Int = 0): Bitmap? {
        try {
            val type = readBmpSize(resId)
            val w = getMinVal(type.outHeight, type.outWidth)

            var sw = if (minSize > 0)
                minSize
            else {
                getMinVal(displaySize)
            }


            var i = 1
            while (i <= 1024) {
                if (sw * i > w) {
                    break
                }
                i *= 2
            }

            i /= 2
            if (i <= 0)
                i = 1;


            for (outCount in 0..6) {
                try {
                    val options = BitmapFactory.Options()
                    options.inSampleSize = i
                    if (type.outMimeType.contains("jpeg"))
                        options.inPreferredConfig = Bitmap.Config.RGB_565
                    else
                        options.inPreferredConfig = Bitmap.Config.ARGB_8888
                    val bmp =
                            BitmapFactory.decodeResource(df.appContext!!.resources, resId, options)
                    return bmp
                } catch (e: OutOfMemoryError) {
                    i *= 2
                }

            }
        } catch (e: Throwable) {
            // TODO Auto-generated catch block

        }

        return null
    }

    /**
     * 读取超大图片,根据屏幕分辩率自动压缩

     * @param url
     * *
     * @return
     */
    @JvmStatic
    @JvmOverloads
    fun readBigBitmap(url: String, minSize: Int = 0): Bitmap? {
        try {
            val type = readBmpSize(url)
            val w = getMinVal(type.outHeight, type.outWidth)

            var sw = if (minSize > 0)
                minSize
            else {
                getMinVal(displaySize)
            }


            var i = 1
            while (i <= 1024) {
                if (sw * i > w) {
                    break
                }
                i *= 2
            }

            i /= 2
            if (i <= 0)
                i = 1;


            for (outCount in 0..6) {
                try {
                    val options = BitmapFactory.Options()
                    options.inSampleSize = i
                    if (type.outMimeType.contains("jpeg"))
                        options.inPreferredConfig = Bitmap.Config.RGB_565
                    else
                        options.inPreferredConfig = Bitmap.Config.ARGB_8888
                    val bmp = BitmapFactory.decodeFile(url, options)
                    return bmp
                } catch (e: OutOfMemoryError) {
                    i *= 2
                }

            }
        } catch (e: Throwable) {
            // TODO Auto-generated catch block

        }

        return null
    }


    /**
     * 获取图片大小

     * @param img
     * *
     * @return
     */
    @JvmStatic
    fun getImgSize(img: ImageView): IntArray {
        if (img.width > 0)
            return intArrayOf(img.width, img.height)
        val da = img.drawable
        if (da != null) {
            return intArrayOf(da.intrinsicWidth, da.intrinsicHeight)
        }

        return intArrayOf(0, 0)
    }

    /**
     * 压缩并保存图片为jpg

     * @param rawBitmap 待保存的图片
     * *
     * @param fileName  文件路径
     * *
     * @param quality   图像质量,0-100
     */
    @JvmStatic
    fun compressAndSaveBitmapToSDCard(
            rawBitmap: Bitmap?,
            fileName: String, quality: Int
    ) {
        if (rawBitmap == null)
            return;

        val saveFilePaht = fileName

        try {
            val saveFile = File(saveFilePaht)
            if (!saveFile.parentFile.exists()) {
                saveFile.parentFile.mkdirs();
            }
            if (!saveFile.exists()) {
                saveFile.createNewFile()
            }
            val fileOutputStream = FileOutputStream(saveFile)
            if (fileOutputStream != null) {
                // imageBitmap.compress(format, quality, stream);
                // 把位图的压缩信息写入到一个指定的输出流中
                // 第一个参数format为压缩的格式
                // 第二个参数quality为图像压缩比的值,0-100.0 意味着小尺寸压缩,100意味着高质量压缩
                // 第三个参数stream为输出流
                rawBitmap.compress(
                        Bitmap.CompressFormat.JPEG, quality,
                        fileOutputStream
                )
            }
            fileOutputStream.flush()
            fileOutputStream.close()
        } catch (e: Throwable) {
            FileExt.logException(e)
        }

    }

    /**
     * 保存图片为png

     * @param rawBitmap
     * *
     * @param fileName
     */
    @JvmStatic
    fun compressAndSaveBitmapToSDCard(
            rawBitmap: Bitmap,
            fileName: String
    ) {
        val saveFilePaht = fileName

        try {
            val saveFile = File(saveFilePaht)
            if (!saveFile.parentFile.exists()) {
                saveFile.parentFile.mkdirs();
            }
            if (!saveFile.exists()) {
                saveFile.createNewFile()
            }
            val fileOutputStream = FileOutputStream(saveFile)
            if (fileOutputStream != null) {
                // imageBitmap.compress(format, quality, stream);
                // 把位图的压缩信息写入到一个指定的输出流中
                // 第一个参数format为压缩的格式
                // 第二个参数quality为图像压缩比的值,0-100.0 意味着小尺寸压缩,100意味着高质量压缩
                // 第三个参数stream为输出流
                rawBitmap.compress(
                        Bitmap.CompressFormat.PNG, 100,
                        fileOutputStream
                )
            }
            fileOutputStream.flush()
            fileOutputStream.close()
        } catch (e: Throwable) {
            FileExt.logException(e)
        }

    }

    internal var getFileTable: Func1<String>? = null
    internal var takePhotoFunc: Func1<String>? = null
    internal val getFileTag = 33454
    internal val takePhotoTag = 33455
    internal val cropFileTag = 33456

    @JvmStatic
    val cameraFile: File
        get() = (FileExt.getInnerCacheDir() + "/camera.jpg")

    @JvmStatic
    val cropFile: File
        get() = (FileExt.getInnerCacheDir() + "/crop.png")

    /**
     * 拍照

     * @param act
     * *
     * @param res 结果回调
     */
    @JvmStatic
    fun takePhoto(act: Activity, res: Func1<String>) {
        try {
            // mCurrentPhotoFile = new File(PHOTO_DIR, mFileName);
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE, null)
            intent.putExtra(
                    MediaStore.EXTRA_OUTPUT,
                    FileExt.getFileUri(cameraFile)
            )
            takePhotoFunc = res
            act.startActivityForResult(intent, takePhotoTag)

        } catch (e: Exception) {

            df.msg("摄像头启动失败!")
            FileExt.logException(e, false)
            takePhotoFunc = null

        }

    }

    suspend fun awaitTakePhoto(act: Activity) = suspendCoroutine<String> { conti ->
        takePhoto(act) { res ->
            conti.resume(res)
        }
    }

    /**
     * 获取本地图像

     * @param act
     * *
     * @param res 结果回调
     */
    @JvmStatic
    fun getImageFile(act: Activity, res: Func1<String>) {
        getFile(act, "image/*", null, res)
    }

    suspend fun awaitGetImageFile(act: Activity): String {
        return awaitGetFile(act, "image/*")
    }

    /**
     * 裁剪图片

     * @param act
     * *
     * @param outX
     * *
     * @param outY
     * *
     * @param imageFile 被裁剪的图片
     * *
     * @param res
     */
    @JvmStatic
    fun cropImageFile(act: Activity, outX: Int, outY: Int, imageFile: String, res: Func1<String>) {
        try {
            val intent = Intent("com.android.camera.action.CROP")
            intent.setDataAndType(FileExt.getFileUri(File(imageFile)), "image/*")
            intent.putExtra("crop", "true")
            //长宽比
            intent.putExtra("aspectX", 1)
            intent.putExtra("aspectY", 1)
            intent.putExtra("outputX", outX)
            intent.putExtra("outputY", outY)
            intent.putExtra("return-json", false)
            intent.putExtra("noFaceDetection", true) // no face detection
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cropFile)
            intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            // 新参数

            getFileTable = res
            act.startActivityForResult(intent, cropFileTag)
        } catch (e: Throwable) {
            FileExt.logException(e)
            getFileTable = null
        }

    }

    /**
     * 从相册获取图片并裁剪

     * @param act
     * *
     * @param res
     */
    @JvmStatic
    fun getImageCrop(act: Activity, outX: Int, outY: Int, res: Func1<String>) {

        try {
            val intent = Intent(Intent.ACTION_PICK, null)
            intent.type = "image/*"
            intent.putExtra("crop", "true")
            //长宽比
            intent.putExtra("aspectX", 1)
            intent.putExtra("aspectY", 1)
            intent.putExtra("outputX", outX)
            intent.putExtra("outputY", outY)
            intent.putExtra("return-json", false)
            intent.putExtra("noFaceDetection", true) // no face detection
            intent.putExtra(MediaStore.EXTRA_OUTPUT, FileExt.getFileUri(cropFile))
            intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            // 新参数

            getFileTable = res
            act.startActivityForResult(intent, cropFileTag)
        } catch (e: Throwable) {
            FileExt.logException(e)
            getFileTable = null
        }

    }

    suspend fun awaitGetFile(act: Activity, filter: String = "*/*", mediaType: Array<String>? = null) = suspendCoroutine<String> { conti ->
        getFile(act, filter, mediaType) { res ->
            conti.resume(res)
        }
    }

    @JvmStatic
    fun getFile(act: Activity, filter: String = "*/*", mediaType: Array<String>? = null, res: Func1<String>) {
        try {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = filter
            if (filter == "*/*")
                intent.addCategory(Intent.CATEGORY_OPENABLE)
            // 新参数
            getFileTable = res
            if (mediaType != null && mediaType.isNotEmpty()) {
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mediaType)
            }
            act.startActivityForResult(intent, getFileTag)
        } catch (e: Throwable) {
            FileExt.logException(e)
            getFileTable = null
        }

    }
}
