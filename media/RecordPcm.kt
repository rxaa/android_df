package net.rxaa.media

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import net.rxaa.ext.FileExt
import java.io.FileOutputStream

class RecordPcm(
    val FREQUENCY: Int = 22050,
    val CHANNEL: Int = AudioFormat.CHANNEL_IN_MONO,
    val ENCODING: Int = AudioFormat.ENCODING_PCM_16BIT
) {
    val bufferSize = AudioRecord.getMinBufferSize(FREQUENCY, CHANNEL, ENCODING)

    val mReadInterval = 5;

    @Volatile
    private var isStop = false;

    private val mDataBuffer = ByteArray(bufferSize)
    var onBuffer = { buffer: ByteArray, size: Int -> }
    var onError = { e: Exception -> }


    fun stop() {
        isStop = true;
    }

    private fun byteArray2ShortArray(data: ByteArray, items: Int): ShortArray {
        val retVal = ShortArray(items)
        for (i in retVal.indices) {
            retVal[i] =
                (data[i * 2].toInt() and 0xff.toInt() or (data[i * 2 + 1].toInt() and 0xff.toInt() shl 8.toInt())).toShort()
        }
        return retVal
    }

    fun getVolumeMax(size: Int, bytes_pkg: ByteArray): Int {

        //way 2
        val mShortArrayLenght = size / 2
        val short_buffer = byteArray2ShortArray(bytes_pkg, mShortArrayLenght)
        var max = 0
        if (size > 0) {
            for (i in 0 until mShortArrayLenght) {
                if (Math.abs(short_buffer[i].toInt()) > max) {
                    max = Math.abs(short_buffer[i].toInt())
                }
            }
        }
        return max
    }

    private fun create(): AudioRecord {
        for (i in 0..3) {
            try {
                val audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    FREQUENCY,
                    CHANNEL,
                    ENCODING,
                    bufferSize
                )
                val state = audioRecord.state
                if (state == AudioRecord.STATE_INITIALIZED) {
                    audioRecord.startRecording()
                    return audioRecord;
                }
            } catch (e: Exception) {
                Thread.sleep(mReadInterval.toLong())
            }
        }
        throw Exception("录音启动失败!")
    }


    var onEncodeAAC = { buffer: ByteArray, size: Int -> }

    //编码结果输出到文件
    var outFile = ""

    fun start() {
        isStop = false
        Thread() {
            synchronized(this) {
                FileExt.catchLog {
                    val audioRecord = create()
                    val aac = AACEncode(FREQUENCY)
                    var fOut: FileOutputStream? = null
                    try {
                        aac.start()
                        if (outFile != "") {
                            val fos = FileOutputStream(outFile)
                            fOut = fos
                            aac.onBuffer = { buffer: ByteArray, size: Int ->
                                //Log.e("wwwww", "encodeSize" + size)
                                fos.write(buffer, 0, size);
                                onEncodeAAC(buffer, size)
                            }
                        } else {
                            aac.onBuffer = onEncodeAAC
                        }
                        while (true) {
                            if (isStop) {
                                break;
                            }
                            val len = audioRecord.read(mDataBuffer, 0, mDataBuffer.size)
                            if (len > 0) {
                                if (isStop)
                                    break;

                                onBuffer(mDataBuffer, len)
                                aac.encode(len, mDataBuffer)
                            }
                            if (isStop) {
                                break;
                            }

                            Thread.sleep(mReadInterval.toLong())
                        }
//                    onBuffer(mDataBuffer, 0)
                    } finally {
                        aac.stop()
                        audioRecord.release();
                        fOut?.close()
                    }
                }
            }
        }.start();
    }
}