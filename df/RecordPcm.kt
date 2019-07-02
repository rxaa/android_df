package rxaa.df

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

class RecordPcm(val FREQUENCY: Int = 16000,
                val CHANNEL: Int = AudioFormat.CHANNEL_IN_MONO,
                val ENCODING: Int = AudioFormat.ENCODING_PCM_16BIT,
                val timeInterval: Int = 40
) {
    val bufferSize = AudioRecord.getMinBufferSize(FREQUENCY, CHANNEL, ENCODING)

    val mReadInterval = 10;

    private var isStop = false;

    private val mDataBuffer = ByteArray(bufferSize)
    var onBuffer = { buffer: ByteArray, size: Int -> }
    var onError = { e: Exception -> }


    fun stop() {
        isStop = true;
    }

    private fun create(): AudioRecord {
        for (i in 0..3) {
            try {
                val audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, FREQUENCY, CHANNEL, ENCODING, bufferSize)
                val state = audioRecord.state
                if (state == AudioRecord.STATE_INITIALIZED) {
                    audioRecord.startRecording()
                    return audioRecord;
                }
            } catch(e: Exception) {
                Thread.sleep(mReadInterval.toLong())
            }
        }
        throw Exception("录音启动失败!")
    }

    fun start() {
        Thread() {
            df.catchLog {
                val audioRecord = create()
                try {
                    while (true) {
                        if (isStop) {
                            break;
                        }
                        val len = audioRecord.read(mDataBuffer, 0, mDataBuffer.size)
                        if (len > 0) {
                            if (isStop)
                                break;

                            onBuffer(mDataBuffer, len)
                        }
                        if (isStop) {
                            break;
                        }

                        Thread.sleep(mReadInterval.toLong())
                    }
//                    onBuffer(mDataBuffer, 0)
                } finally {
                    audioRecord.release();
                }
            }
        }.start();
    }
}