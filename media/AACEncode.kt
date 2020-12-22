package net.rxaa.media

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaFormat.MIMETYPE_AUDIO_AAC
import android.os.Build
import net.rxaa.ext.notNull

/**
 * aac音频编码
 */
class AACEncode(
    val SAMPLE_RATE: Int = 22050,
    val BIT_RATE: Int = 96000,
    val channelCount: Int = 1
) {

    @Volatile
    var mMediaCodec: MediaCodec? = null

    var onBuffer = { buffer: ByteArray, size: Int -> }

    fun start() {
        val format = MediaFormat.createAudioFormat(MIMETYPE_AUDIO_AAC, SAMPLE_RATE, channelCount)
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)

        mMediaCodec = MediaCodec.createEncoderByType(MIMETYPE_AUDIO_AAC)
        mMediaCodec!!.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mMediaCodec!!.start()
    }

    fun stop() {
        mMediaCodec.notNull { codec ->
            mMediaCodec = null
            codec.stop();
            codec.release();
        }
    }

    fun end() {

    }

    fun encode(size: Int, bytes_pkg: ByteArray) {
        mMediaCodec.notNull { codec ->
            val inputBufferIndex = codec.dequeueInputBuffer(-1)
            if (inputBufferIndex >= 0) {

                val inputBuffer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    codec.getInputBuffer(inputBufferIndex)
                } else {
                    val inputBuffers = codec.getInputBuffers();
                    inputBuffers[inputBufferIndex]
                }
                if (inputBuffer != null) {
                    inputBuffer.clear()
                    inputBuffer.put(bytes_pkg)
                    inputBuffer.limit(bytes_pkg.size)
                }

                codec.queueInputBuffer(inputBufferIndex, 0, size, 0, 0)
            }

            val bufferInfo = MediaCodec.BufferInfo()
            var outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
            while (outputBufferIndex >= 0) {
                val outputBuffer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    codec.getOutputBuffer(outputBufferIndex)
                } else {
                    val outputBuffer = codec.getOutputBuffers();
                    outputBuffer[outputBufferIndex]
                }

                if (outputBuffer != null) {
                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                    val chunkAudio = ByteArray(bufferInfo.size + 7)// 7 is ADTS size
                    addADTStoPacket(chunkAudio, chunkAudio.size)
                    outputBuffer.get(chunkAudio, 7, bufferInfo.size)
                    outputBuffer.position(bufferInfo.offset)

                    onBuffer(chunkAudio, chunkAudio.size)
                }

                codec.releaseOutputBuffer(outputBufferIndex, false)
                outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
            }
        }

    }

    companion object {
        val freqMap = mapOf(
            Pair(96000, 0),
            Pair(88200, 1),
            Pair(64000, 2),
            Pair(48000, 3),
            Pair(44100, 4),
            Pair(32000, 5),
            Pair(24000, 6),
            Pair(22050, 7),
            Pair(16000, 8),
            Pair(12000, 9),
            Pair(11025, 10),
            Pair(8000, 11),
            Pair(7350, 12)
        )
    }

    private fun addADTStoPacket(packet: ByteArray, packetLen: Int) {

        val profile = 2  //AAC LC
        //0: 96000 Hz
        //1: 88200 Hz
        //2: 64000 Hz
        //3: 48000 Hz
        //4: 44100 Hz
        //5: 32000 Hz
        //6: 24000 Hz
        //7: 22050 Hz
        //8: 16000 Hz
        //9: 12000 Hz
        //10: 11025 Hz
        //11: 8000 Hz
        //12: 7350 Hz
        //13: Reserved
        //14: Reserved
        //15: frequency is written explictly
        val freqIdx = freqMap[SAMPLE_RATE] ?: 4 //44100 Hz

        //0: Defined in AOT Specifc Config
        //1: 1 channel: front-center
        //2: 2 channels: front-left, front-right
        //3: 3 channels: front-center, front-left, front-right
        //4: 4 channels: front-center, front-left, front-right, back-center
        //5: 5 channels: front-center, front-left, front-right, back-left, back-right
        //6: 6 channels: front-center, front-left, front-right, back-left, back-right, LFE-channel
        //7: 8 channels: front-center, front-left, front-right, side-left, side-right, back-left, back-right, LFE-channel
        //8-15: Reserved
        val chanCfg = 1  //CPE
        // fill in ADTS data
        packet[0] = 0xFF.toByte()
        packet[1] = 0xF9.toByte()
        packet[2] = ((profile - 1 shl 6) + (freqIdx shl 2) + (chanCfg shr 2)).toByte()
        packet[3] = ((chanCfg and 3 shl 6) + (packetLen shr 11)).toByte()
        packet[4] = (packetLen and 0x7FF shr 3).toByte()
        packet[5] = ((packetLen and 7 shl 5) + 0x1F).toByte()
        packet[6] = 0xFC.toByte()
    }
}