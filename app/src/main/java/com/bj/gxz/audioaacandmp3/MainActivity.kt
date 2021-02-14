package com.bj.gxz.audioaacandmp3

import android.Manifest
import android.content.pm.PackageManager
import android.os.*
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.channels.FileChannel


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermission()
    }

    fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO,
                ), 1
            )
        }
    }

    var handler: Handler = Handler(Looper.getMainLooper())
    var audioEnCodeThread: AudioEnCodeThread = AudioEnCodeThread()
    fun encodeAAC(view: View) {
        audioEnCodeThread.startCodec()
        handler.postDelayed(
            {
                audioEnCodeThread.stopCodec()
            }, 5000
        )
    }

    fun decodePcm(view: View) {
        val src = Environment.getExternalStorageDirectory().absolutePath + "/441k_2.mp3"
        val out = Environment.getExternalStorageDirectory().absolutePath + "/441k_2.pcm"
        copyAssets("441k_2.mp3", src)
        DecodeMp3Thread(src, out).start()
    }

    @Throws(IOException::class)
    private fun copyAssets(assetsName: String, path: String) {
        val assetFileDescriptor = assets.openFd(assetsName)
        val from: FileChannel = FileInputStream(assetFileDescriptor.fileDescriptor).getChannel()
        val to: FileChannel = FileOutputStream(path).getChannel()
        from.transferTo(assetFileDescriptor.startOffset, assetFileDescriptor.length, to)
        from.close()
        to.close()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}