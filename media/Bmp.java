package net.rxaa.media;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build.VERSION;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

import net.rxaa.util.df;
import net.rxaa.ext.FileExt;

@SuppressLint("NewApi")
public class Bmp {

    /**
     * @param sentBitmap
     * @param radius     模糊程度
     * @return
     */
    public static Bitmap blur(Bitmap sentBitmap, int radius) {
        if (sentBitmap == null)
            return null;


        if (VERSION.SDK_INT > 16) {
            return blurSys(df.getAppContext(), sentBitmap, radius);
        }

        return fastblur(sentBitmap, radius);
    }


    /**
     * 生成全圆角图片
     *
     * @param imagePath
     * @param width
     * @param height
     * @return
     */

    public static Bitmap toOvalBitmap(String imagePath, int width, int height) {
        if (imagePath == null || imagePath.length() == 0)
            return null;

        try {
            Bitmap bitmap = null;
            bitmap = Pic.readBigBitmap(imagePath, Pic.getMinVal(width, height));
            if (bitmap == null)
                return null;

            return toOvalBitmap(bitmap, width, height);
        } catch (Throwable e) {
            // TODO Auto-generated catch block
        }

        return null;
    }

    /**
     * 生成半圆角图片
     *
     * @param imagePath
     * @return
     */

    public static Bitmap toRoundedCorner(String imagePath, float roundPx) {
        if (imagePath == null || imagePath.length() == 0)
            return null;

        try {
            Bitmap bitmap = null;
            bitmap =  Pic.readBigBitmap(imagePath);
            if (bitmap == null)
                return null;

            return getRoundedCornerBitmap(bitmap, roundPx);
        } catch (Throwable e) {
            // TODO Auto-generated catch block
        }

        return null;
    }

    // 转换成圆角,并且取中间部分
    public static Bitmap getRoundedCornerBitmapCenter(Bitmap bitmap, float roundPx, int w, int h) {
        try {
            Bitmap output = Bitmap.createBitmap(w, h, Config.ARGB_8888);
            Canvas canvas = new Canvas(output);
            final int color = 0xff424242;
            final Paint paint = new Paint();

            double rateDst = (double) w / (double) h;
            double rateSrc = (double) bitmap.getWidth() / (double) bitmap.getHeight();


            Rect rectSrc = null;

            if (rateSrc > rateDst) {
                int newW = (int) (rateDst * (double) bitmap.getHeight());
                int start = (bitmap.getWidth() - newW) / 2;
                rectSrc = new Rect(start, 0, start + newW, bitmap.getHeight());
            } else {
                int newH = (int) ((double) bitmap.getWidth() / rateDst);
                int start = ((bitmap.getHeight() - newH) / 2);
                rectSrc = new Rect(0, start, bitmap.getWidth(), start + newH);
            }


            final Rect rectDst = new Rect(0, 0, w, h);
            final RectF rectF = new RectF(rectDst);
            paint.setAntiAlias(true);
            canvas.drawARGB(0, 0, 0, 0);
            paint.setColor(color);
            paint.setAntiAlias(true);
            paint.setDither(true);
            paint.setFilterBitmap(true);
            canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
            canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
            paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));

            canvas.drawBitmap(bitmap, rectSrc, rectDst, paint);
            return output;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 转换成半圆角；
    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, float roundPx) {
        Bitmap output = null;
        if (bitmap != null) {
            try {
                output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(),
                        Config.ARGB_8888);
                Canvas canvas = new Canvas(output);
                final int color = 0xff424242;
                final Paint paint = new Paint();
                final Rect rect = new Rect(0, 0, bitmap.getWidth(),
                        bitmap.getHeight());
                final RectF rectF = new RectF(rect);

                paint.setAntiAlias(true);
                paint.setDither(true);
                canvas.drawARGB(0, 0, 0, 0);
                paint.setColor(color);
                canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
                paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
                canvas.drawBitmap(bitmap, rect, rect, paint);
            } catch (Throwable e) {
                // TODO Auto-generated catch block
                return null;
            }
        }
        return output;
    }


    /**
     * 图片转为全圆角
     *
     * @param bitmap
     * @param width
     * @param height
     * @return
     */
    public static Bitmap toOvalBitmap(Bitmap bitmap, int width, int height) {
        try {
            // 计算缩放率，新尺寸除原始尺寸
            float scaleWidth = ((float) width) / bitmap.getWidth();
            float scaleHeight = ((float) height) / bitmap.getHeight();
            // 创建操作图片用的matrix对象
            Matrix matrix = new Matrix();
            // 缩放图片动作
            matrix.postScale(scaleWidth, scaleHeight);

            final RectF rectF = new RectF(0, 0, width, height);

            Bitmap output = Bitmap
                    .createBitmap(width, height, Config.ARGB_8888);

            final Paint paint = new Paint();

            // paint.setAntiAlias(true);
            paint.setColor(Color.BLACK);

            Canvas canvas = new Canvas(output);
            canvas.setDrawFilter(new PaintFlagsDrawFilter(0,
                    Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
            canvas.drawOval(rectF, paint);
            paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
            canvas.drawBitmap(bitmap, matrix, paint);

            return output;
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            return null;
        }
    }

    static Bitmap blurSys(Context context, Bitmap sentBitmap, int radius) {
        try {
            Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);

            final RenderScript rs = RenderScript.create(context);
            final Allocation input = Allocation.createFromBitmap(rs,
                    sentBitmap, Allocation.MipmapControl.MIPMAP_NONE,
                    Allocation.USAGE_SCRIPT);
            final Allocation output = Allocation.createTyped(rs,
                    input.getType());
            final ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs,
                    Element.U8_4(rs));
            script.setRadius(radius /* e.g. 3.f */);
            script.setInput(input);
            script.forEach(output);
            output.copyTo(bitmap);
            return bitmap;
        } catch (Throwable e) {
            // TODO Auto-generated catch block
        }
        return null;
    }

    static Bitmap fastblur(Bitmap sentBitmap, int radius) {

        try {
            Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);

            if (radius < 1) {
                return (null);
            }

            int w = bitmap.getWidth();
            int h = bitmap.getHeight();

            int[] pix = new int[w * h];
            bitmap.getPixels(pix, 0, w, 0, 0, w, h);

            int wm = w - 1;
            int hm = h - 1;
            int wh = w * h;
            int div = radius + radius + 1;

            int r[] = new int[wh];
            int g[] = new int[wh];
            int b[] = new int[wh];
            int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
            int vmin[] = new int[Math.max(w, h)];

            int divsum = (div + 1) >> 1;
            divsum *= divsum;
            int dv[] = new int[256 * divsum];
            for (i = 0; i < 256 * divsum; i++) {
                dv[i] = (i / divsum);
            }

            yw = yi = 0;

            int[][] stack = new int[div][3];
            int stackpointer;
            int stackstart;
            int[] sir;
            int rbs;
            int r1 = radius + 1;
            int routsum, goutsum, boutsum;
            int rinsum, ginsum, binsum;

            for (y = 0; y < h; y++) {
                rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
                for (i = -radius; i <= radius; i++) {
                    p = pix[yi + Math.min(wm, Math.max(i, 0))];
                    sir = stack[i + radius];
                    sir[0] = (p & 0xff0000) >> 16;
                    sir[1] = (p & 0x00ff00) >> 8;
                    sir[2] = (p & 0x0000ff);
                    rbs = r1 - Math.abs(i);
                    rsum += sir[0] * rbs;
                    gsum += sir[1] * rbs;
                    bsum += sir[2] * rbs;
                    if (i > 0) {
                        rinsum += sir[0];
                        ginsum += sir[1];
                        binsum += sir[2];
                    } else {
                        routsum += sir[0];
                        goutsum += sir[1];
                        boutsum += sir[2];
                    }
                }
                stackpointer = radius;

                for (x = 0; x < w; x++) {

                    r[yi] = dv[rsum];
                    g[yi] = dv[gsum];
                    b[yi] = dv[bsum];

                    rsum -= routsum;
                    gsum -= goutsum;
                    bsum -= boutsum;

                    stackstart = stackpointer - radius + div;
                    sir = stack[stackstart % div];

                    routsum -= sir[0];
                    goutsum -= sir[1];
                    boutsum -= sir[2];

                    if (y == 0) {
                        vmin[x] = Math.min(x + radius + 1, wm);
                    }
                    p = pix[yw + vmin[x]];

                    sir[0] = (p & 0xff0000) >> 16;
                    sir[1] = (p & 0x00ff00) >> 8;
                    sir[2] = (p & 0x0000ff);

                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];

                    rsum += rinsum;
                    gsum += ginsum;
                    bsum += binsum;

                    stackpointer = (stackpointer + 1) % div;
                    sir = stack[(stackpointer) % div];

                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];

                    rinsum -= sir[0];
                    ginsum -= sir[1];
                    binsum -= sir[2];

                    yi++;
                }
                yw += w;
            }
            for (x = 0; x < w; x++) {
                rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
                yp = -radius * w;
                for (i = -radius; i <= radius; i++) {
                    yi = Math.max(0, yp) + x;

                    sir = stack[i + radius];

                    sir[0] = r[yi];
                    sir[1] = g[yi];
                    sir[2] = b[yi];

                    rbs = r1 - Math.abs(i);

                    rsum += r[yi] * rbs;
                    gsum += g[yi] * rbs;
                    bsum += b[yi] * rbs;

                    if (i > 0) {
                        rinsum += sir[0];
                        ginsum += sir[1];
                        binsum += sir[2];
                    } else {
                        routsum += sir[0];
                        goutsum += sir[1];
                        boutsum += sir[2];
                    }

                    if (i < hm) {
                        yp += w;
                    }
                }
                yi = x;
                stackpointer = radius;
                for (y = 0; y < h; y++) {
                    // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                    pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16)
                            | (dv[gsum] << 8) | dv[bsum];

                    rsum -= routsum;
                    gsum -= goutsum;
                    bsum -= boutsum;

                    stackstart = stackpointer - radius + div;
                    sir = stack[stackstart % div];

                    routsum -= sir[0];
                    goutsum -= sir[1];
                    boutsum -= sir[2];

                    if (x == 0) {
                        vmin[y] = Math.min(y + r1, hm) * w;
                    }
                    p = x + vmin[y];

                    sir[0] = r[p];
                    sir[1] = g[p];
                    sir[2] = b[p];

                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];

                    rsum += rinsum;
                    gsum += ginsum;
                    bsum += binsum;

                    stackpointer = (stackpointer + 1) % div;
                    sir = stack[stackpointer];

                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];

                    rinsum -= sir[0];
                    ginsum -= sir[1];
                    binsum -= sir[2];

                    yi += w;
                }
            }

            bitmap.setPixels(pix, 0, w, 0, 0, w, h);

            return (bitmap);
        } catch (Throwable e) {
            // TODO Auto-generated catch block
        }
        return null;
    }

    /*
     * 获取灰色图片
     */
    public static Bitmap greyBitmap(String path) {

        try {

            if (path == null || path.length() < 1)
                return null;

            Bitmap bitmap = Pic.readBigBitmap(path);
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            Bitmap faceIconGreyBitmap = Bitmap.createBitmap(width, height,
                    Config.ARGB_8888);

            Canvas canvas = new Canvas(faceIconGreyBitmap);
            Paint paint = new Paint();
            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(0f);
            ColorMatrixColorFilter colorMatrixFilter = new ColorMatrixColorFilter(
                    colorMatrix);
            paint.setColorFilter(colorMatrixFilter);
            canvas.drawBitmap(bitmap, 0, 0, paint);

            return faceIconGreyBitmap;

        } catch (Throwable e) {
            // TODO: handle exception
            FileExt.logException(e, true, "");
        }
        return null;
    }

    /**
     * 生成矩形图片
     */
    public static Bitmap toSquarBitmap(String imagePath, int width, int height) {
        if (imagePath == null || imagePath.length() == 0)
            return null;

        try {
            Bitmap bitmap = null;

            bitmap =  Pic.readBigBitmap(imagePath, Pic.getMinVal(width, height));
            if (bitmap == null)
                return null;

            return toSquarBitmap(bitmap, width, height);
        } catch (Throwable e) {
            // TODO Auto-generated catch block
        }

        return null;
    }

    /**
     * 生成矩形图片
     */
    public static Bitmap toSquarBitmap(Bitmap bitmap, int width, int height) {
        try {
            // 计算缩放率，新尺寸除原始尺寸
            float scaleWidth = ((float) width) / bitmap.getWidth();
            float scaleHeight = ((float) height) / bitmap.getHeight();
            // 创建操作图片用的matrix对象
            Matrix matrix = new Matrix();
            // 缩放图片动作
            matrix.postScale(scaleWidth, scaleHeight);

            Bitmap output = Bitmap
                    .createBitmap(width, height, Config.ARGB_8888);

            final RectF rectF = new RectF(0, 0, width, height);
            final Paint paint = new Paint();

            // paint.setAntiAlias(true);
            paint.setColor(Color.BLACK);

            Canvas canvas = new Canvas(output);
            // canvas.setDrawFilter(new PaintFlagsDrawFilter(0,
            // Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG));
            // canvas.drawOval(rectF, paint);
            canvas.drawRect(0, 0, width, height, paint);
            paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
            canvas.drawBitmap(bitmap, matrix, paint);

            return output;
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            return null;
        }
    }

}
