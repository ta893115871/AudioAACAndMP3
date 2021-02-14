package com.bj.gxz.audioaacandmp3

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel


/**
 * Created by guxiuzhong@baidu.com on 2021/2/13.
 */
class DecodeMp3Thread(private val srcPath: String, private val outPath: String) : Thread() {
    companion object {
        private const val TAG = "DecodeMp3Thread"
    }

    private var mediaExtractor: MediaExtractor = MediaExtractor()

    override fun run() {
        super.run()
        val writePcmChannel: FileChannel = FileOutputStream(outPath).getChannel()
        mediaExtractor.setDataSource(srcPath)
        var index = -1
        val count = mediaExtractor.trackCount
        for (i in 0 until count) {
            val format = mediaExtractor.getTrackFormat(i)
            if (format.getString(MediaFormat.KEY_MIME)!!.startsWith("audio/")) {
                index = i
            }
        }
        mediaExtractor.selectTrack(index)
        val format = mediaExtractor.getTrackFormat(index)
        Log.d(TAG, "format=$format")
        val maxBufferSize: Int
        if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            maxBufferSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
            Log.d(TAG, "KEY_MAX_INPUT_SIZE");
        } else {
            maxBufferSize = 100 * 1000;
        }
        Log.d(TAG, "maxBufferSize=$maxBufferSize")
        val buffer = ByteBuffer.allocateDirect(maxBufferSize)

        val mediaCodec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        mediaCodec.configure(format, null, null, 0)
        mediaCodec.start()
        val info = MediaCodec.BufferInfo()
        while (true) {
            val inputIndex = mediaCodec.dequeueInputBuffer(10 * 1000);
            if (inputIndex >= 0) {
                val sampleTimeUs = mediaExtractor.getSampleTime();
                if (sampleTimeUs == -1L) {
                    Log.d(TAG, "break")
                    break
                }
                info.presentationTimeUs = sampleTimeUs
                info.flags = mediaExtractor.sampleFlags
                info.size = mediaExtractor.readSampleData(buffer, 0)

                val data = ByteArray(buffer.remaining())
                buffer.get(data)

                val inputBuffer = mediaCodec.getInputBuffer(inputIndex)
                inputBuffer!!.clear()
                inputBuffer.put(data)
                mediaCodec.queueInputBuffer(
                    inputIndex,
                    0,
                    info.size,
                    info.presentationTimeUs,
                    info.flags
                )
                mediaExtractor.advance()
            }

            var outputIndex = mediaCodec.dequeueOutputBuffer(info, 10_000)
            while (outputIndex >= 0) {
                val outByteBuffer = mediaCodec.getOutputBuffer(outputIndex)

                // to file
                writePcmChannel.write(outByteBuffer)

                mediaCodec.releaseOutputBuffer(outputIndex, false)
                outputIndex = mediaCodec.dequeueOutputBuffer(info, 0)
            }
        }

        writePcmChannel.close()
        mediaCodec.stop()
        mediaCodec.release()
        mediaExtractor.release()
        Log.d(TAG, "decode pcm done:$outPath")

        // pcm -> WAV
        val outWavPath = Environment.getExternalStorageDirectory().absolutePath + "/441k_2.wav"
        val wavFile = File(outWavPath)
        PcmToWavUtil(
            44100,
            2, 16
        ).pcmToWav(outPath, wavFile.absolutePath)
        Log.d(TAG, "pcm -> WAV done:$outWavPath")

    }
}