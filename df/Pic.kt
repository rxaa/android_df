package rxaa.df

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.widget.ImageView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object Pic {


    fun getRotateDrawable(b: Bitmap, angle: Float): Drawable {
        val drawable = object : BitmapDrawable(df.appContext!!.resources, b) {
            override fun draw(canvas: Canvas) {
                canvas.save()
                canvas.rotate(angle, b.width.toFloat() / 2, (b.height.toFloat() / 2))
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
        val resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.width, bitmap.height, matrix, true)
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
            val orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
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
            val orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
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
            val degree = Pic.readPictureDegree(pic)
            if (degree > 0) {
                var bmp = Pic.readBigBitmap(pic)
                bmp = Pic.rotaingImageView(degree, bmp)
                iv.setImageBitmap(bmp)
            } else
                iv.setImageBitmap(Pic.readBigBitmap(pic))
        } catch (e: Throwable) {
            df.msg("内存不足,图片读取失败!")
            df.logException(e, false)
        }

    }

    /**
     * 将图片添加到相册
     */
    @JvmStatic
    fun scanPhoto(ctx: Context, imgFileName: String) {
        val mediaScanIntent = Intent(
                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
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
            bmp = null;
            val degree = Pic.readPictureDegree(from)
            if (degree > 0) {
                bmp = Pic.readBigBitmap(from)
                bmp = Pic.rotaingImageView(degree, bmp)
            } else
                bmp = Pic.readBigBitmap(from)
        } catch(e: Exception) {
        }

        if (bmp == null) {
            throw MsgException("内存不足,图片读取失败!")
        }
        Pic.compressAndSaveBitmapToSDCard(bmp, to, quality)
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
                    resId)
            return BitmapFactory.decodeStream(`is`, null, opt)
        } catch (e: Throwable) {
            // TODO Auto-generated catch block

        }

        return null
    }

    @JvmStatic
    @JvmOverloads
    fun readBitmap(url: String, cfg: Bitmap.Config = Bitmap.Config.ARGB_8888): Bitmap? {
        try {
            val opt = BitmapFactory.Options()
            opt.inPreferredConfig = cfg
            // 获取资源图片
            return BitmapFactory.decodeFile(url, opt)
        } catch (e: Throwable) {
            // TODO Auto-generated catch block
        }

        return null
    }

    /**
     * 读取并压缩图片

     * @param url
     * *
     * @param width 压缩宽度
     * *
     * @return
     */
    @JvmStatic
    fun readBitmap(url: String, width: Int, cfg: Bitmap.Config = Bitmap.Config.ARGB_8888): Bitmap? {
        var width = width
        try {
            val size = readBmpSize(url)
            if (width < 1) {
                width = displaySize[0]
            }

            var i = 1
            while (i <= 1024) {
                if (width * i > size[0]) {
                    break
                }
                i *= 2
            }

            i /= 2

            val options = BitmapFactory.Options()
            options.inSampleSize = i
            options.inPreferredConfig = cfg
            val bmp = BitmapFactory.decodeFile(url, options)
            return bmp
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
    fun readBmpSize(url: String): IntArray {

        try {
            val bmpFactoryOptions = BitmapFactory.Options()
            bmpFactoryOptions.inJustDecodeBounds = true
            BitmapFactory.decodeFile(url, bmpFactoryOptions)
            return intArrayOf(bmpFactoryOptions.outWidth, bmpFactoryOptions.outHeight)

        } catch (e: Throwable) {
            // TODO Auto-generated catch block

        }

        return intArrayOf(0, 0)
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


    /**
     * 读取超大图片,根据屏幕分辩率自动压缩

     * @param url
     * *
     * @return
     */
    @JvmStatic
    fun readBigBitmap(url: String): Bitmap? {
        try {
            val w = getMinVal(readBmpSize(url))

            var sw = getMinVal(displaySize)
            if (sw < 360)
                sw = 360;

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
    fun compressAndSaveBitmapToSDCard(rawBitmap: Bitmap?,
                                      fileName: String, quality: Int) {
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
                rawBitmap.compress(Bitmap.CompressFormat.JPEG, quality,
                        fileOutputStream)
            }
            fileOutputStream.flush()
            fileOutputStream.close()
        } catch (e: Throwable) {
            df.logException(e)
        }

    }

    /**
     * 保存图片为png

     * @param rawBitmap
     * *
     * @param fileName
     */
    @JvmStatic
    fun compressAndSaveBitmapToSDCard(rawBitmap: Bitmap,
                                      fileName: String) {
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
                rawBitmap.compress(Bitmap.CompressFormat.PNG, 100,
                        fileOutputStream)
            }
            fileOutputStream.flush()
            fileOutputStream.close()
        } catch (e: Throwable) {
            df.logException(e)
        }

    }

    internal var getFileTable: Func1<String>? = null
    internal var takePhotoFunc: Func1<String>? = null
    internal val getFileTag = 33454
    internal val takePhotoTag = 33455
    internal val cropFileTag = 33456

    @JvmStatic
    val cameraFile: File
        get() = (df.getCacheDir() + "/camera.jpg")

    @JvmStatic
    val cropFile: File
        get() = (df.getCacheDir() + "/crop.png")

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
            intent.putExtra(MediaStore.EXTRA_OUTPUT,
                    Uri.fromFile(cameraFile))
            takePhotoFunc = res
            act.startActivityForResult(intent, takePhotoTag)

        } catch (e: Exception) {

            df.msg("没有摄像头!")
            df.logException(e, false)
            takePhotoFunc = null

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
        getFile(act, "image/*", res)
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
            intent.setDataAndType(Uri.fromFile(File(imageFile)), "image/*")
            intent.putExtra("crop", "true")
            //长宽比
            intent.putExtra("aspectX", 1)
            intent.putExtra("aspectY", 1)
            intent.putExtra("outputX", outX)
            intent.putExtra("outputY", outY)
            intent.putExtra("return-json", false)
            intent.putExtra("noFaceDetection", true) // no face detection
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(cropFile))
            intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString())
            // 新参数

            getFileTable = res
            act.startActivityForResult(intent, cropFileTag)
        } catch (e: Throwable) {
            df.logException(e)
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
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(cropFile))
            intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString())
            // 新参数

            getFileTable = res
            act.startActivityForResult(intent, cropFileTag)
        } catch (e: Throwable) {
            df.logException(e)
            getFileTable = null
        }

    }

    @JvmStatic
    fun getFile(act: Activity, filter: String, res: Func1<String>) {
        try {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = filter
            if (filter == "*/*")
                intent.addCategory(Intent.CATEGORY_OPENABLE)
            // 新参数
            getFileTable = res
            act.startActivityForResult(intent, getFileTag)
        } catch (e: Throwable) {
            df.logException(e)
            getFileTable = null
        }

    }
}
