package net.rxaa.media

import android.media.MediaPlayer
import android.net.Uri
import net.rxaa.util.df
import net.rxaa.ext.FileExt
import java.io.File

class MediaPlayEx {

    var media = MediaPlayer()
        internal set

    /**
     * 是否正在加载网络数据
     */
    /**
     * 是否正在加载

     * @return
     */
    var isPrepare = false
        internal set


    /**
     * 是否正在加载且自动播放
     */
    internal var isPrepareAndPlaying = false

    /**
     * 自动播放指定url

     * @param url
     */
    fun replay(url: String) {
        replay(url, true)
    }

    /**
     * 播放资源id

     * @param resourseID
     */
    fun replay(resourseID: Int) {
        replay("android.resource://" + df.appContext!!.packageName + "/"
                + resourseID, true)
    }


    /**
     * 设置是否循环播放

     * @param loop
     */
    fun setLooping(loop: Boolean) {
        media.isLooping = loop
    }

    /**
     * 播放指定url

     * @param url
     * *
     * @param start 是否自动播放
     * *
     * @return
     */
    fun replay(url: String, start: Boolean): Boolean {
        try {
            media.reset()
            if (url.contains("android.resource")) {
                media.setDataSource(df.appContext!!, Uri.parse(url))
            } else {
                media.setDataSource(url)
            }

            media.prepareAsync()
            isPrepare = true
            isPrepareAndPlaying = start

            media.setOnPreparedListener {
                // TODO Auto-generated method stub
                isPrepare = false
                if (start)
                    media.start()
                isPrepareAndPlaying = false
            }
            return true
        } catch (e: Exception) {
            // TODO Auto-generated catch block
            df.msg("播放失败!")
            isPrepare = false
            isPrepareAndPlaying = false
            media.setOnCompletionListener(null)
        }

        return false
    }

    /**
     * 清空播放结束回调
     */
    fun clearOnStop() {
        media.setOnErrorListener(null)
        media.setOnCompletionListener(null)
    }

    /**
     * 播放结束回调

     * @param onStop
     */
    fun setOnStop(onStop: Runnable?) {
        media.setOnErrorListener { arg0, arg1, arg2 ->
            // TODO Auto-generated method stub
            media.setOnErrorListener(null)
            FileExt.catchLog { onStop?.run() }
            true
        }
        media.setOnCompletionListener {
            // TODO Auto-generated method stub
            FileExt.catchLog { onStop?.run() }
        }
    }

    /**
     * 暂停
     */
    fun pause() {
        try {
            media.pause()
        } catch (e: Exception) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }

    }

    /**
     * 开始
     */
    fun start() {
        try {
            media.start()
        } catch (e: Exception) {
            // TODO Auto-generated catch block
            FileExt.logException(e)
        }

    }

    // TODO Auto-generated catch block
    val isPlaying: Boolean
        get() {
            try {
                return media.isPlaying || isPrepareAndPlaying
            } catch (e: Exception) {
                FileExt.logException(e)
            }

            return false
        }

    val isStop: Boolean
        get() = duration <= 0


    fun stop() {
        try {
            clearOnStop()
            media.stop()
        } catch (e: Exception) {
            // TODO Auto-generated catch block
        }

    }

    /**
     * 千分比跳转

     * @param per
     */
    fun seekPercent(per: Int) {
        seekTime(per * duration / 1000)
    }

    /**
     * 毫秒跳转

     * @param pos
     */
    fun seekTime(pos: Int) {
        media.seekTo(pos)
    }

    /**
     * 获取当前播放位置,毫秒

     * @return
     */
    // TODO Auto-generated catch block
    val pos: Int
        get() {
            try {
                return media.currentPosition
            } catch (e: Exception) {
                return 0
            }

        }

    /**
     * 获取总时长(毫秒)

     * @return
     */
    // TODO Auto-generated catch block
    val duration: Int
        get() {
            if (isPrepare)
                return 0
            try {
                return media.duration
            } catch (e: Exception) {
                return 0
            }

        }

    companion object {

        /**
         * 格式化时间

         * @param time 毫秒
         * *
         * @return
         */
        fun formatTime(time: Long): String {
            return String.format("%02d", time / 1000 / 60) + ":" + String.format("%02d", time / 1000 % 60)
        }

        fun getVoiceLength(file: File): Int {
            try {
                val player = MediaPlayer.create(df.appContext, Uri.fromFile(file)) ?: return 0
                val duration = player.duration
                player.release();
                return duration
            } catch(e: Exception) {
                FileExt.logException(e, false);
            }
            return 0
        }

    }
}
