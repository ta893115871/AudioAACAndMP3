package com.bj.gxz.audioaacandmp3

import android.media.*
import android.os.Environment
import android.util.Log
import java.io.FileOutputStream


/**
 * Created by guxiuzhong on 2021/2/13.
 */
class AudioEnCodeThread : Thread() {

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_COUNT = 1
        private const val TAG = "AudioEnCodeThread"
    }

    @Volatile
    private var isEncoding: Boolean = false
    private var mediaCodec: MediaCodec? = null
    private var minBufferSize: Int = 0
    private var audioRecord: AudioRecord? = null
    private var fileOutputStream: FileOutputStream =
        FileOutputStream(Environment.getExternalStorageDirectory().absolutePath + "/1.aac")


    fun startCodec() {
        Log.d(TAG, "AudioEnCodeThread startCodec")
        isEncoding = true
        //AAC
        val format = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            SAMPLE_RATE,
            CHANNEL_COUNT
        )
        //录音质量
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        //码率,1s的bit
        format.setInteger(MediaFormat.KEY_BIT_RATE, 64_000)

        mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        mediaCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mediaCodec?.start()

        minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT, minBufferSize
        )

        this.start()
    }

    fun stopCodec() {
        isEncoding = false
        audioRecord?.stop()
        audioRecord?.release()
        this.interrupt()
        mediaCodec?.stop()
        mediaCodec?.release()
    }


    override fun run() {
        super.run()
        audioRecord?.startRecording()
        val bufferInfo = MediaCodec.BufferInfo()
        while (isEncoding) {
            // 1.获取音频
            val buffer = ByteArray(minBufferSize)
            val len: Int? = audioRecord?.read(buffer, 0, buffer.size)
            if (len!! <= 0) {
                continue
            }
            // 2.编码
            val index = mediaCodec?.dequeueInputBuffer(10_1000)
            if (index!! >= 0) {
                val inputBuffer = mediaCodec?.getInputBuffer(index)
                inputBuffer!!.clear()
                inputBuffer.put(buffer, 0, len)
                mediaCodec?.queueInputBuffer(index, 0, len, System.nanoTime() / 1000, 0)
            }

            // 3.获取编码后的数据进行下一步的处理（比如：推流等）
            var outIndex = mediaCodec?.dequeueOutputBuffer(bufferInfo, 10_000)
            while (outIndex!! >= 0 && isEncoding) {
                mediaCodec?.getOutputBuffer(outIndex)
                val outData = ByteArray(bufferInfo.size)

                // outData 为编码后的aac数据,temp to file
                fileOutputStream.write(outData)

                mediaCodec?.releaseOutputBuffer(outIndex, false)
                outIndex = mediaCodec?.dequeueOutputBuffer(bufferInfo, 0)
            }
        }
        fileOutputStream.flush()
        fileOutputStream.close()
        Log.d(TAG, "AudioEnCodeThread done")
    }
}
