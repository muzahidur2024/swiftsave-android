package com.swiftsave.app.download

import android.util.Log
import com.swiftsave.app.BuildConfig
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.nio.charset.StandardCharsets

/**
 * Reads a process stream into a buffer (from youtubedl-android [StreamGobbler]; internal there).
 */
internal class YtdlpStreamGobbler(
    private val buffer: StringBuffer,
    private val stream: InputStream,
) : Thread() {
    init {
        start()
    }

    override fun run() {
        try {
            val input: Reader = InputStreamReader(stream, StandardCharsets.UTF_8)
            var nextChar: Int
            while (input.read().also { nextChar = it } != -1) {
                buffer.append(nextChar.toChar())
            }
        } catch (e: IOException) {
            if (BuildConfig.DEBUG) Log.e(TAG, "failed to read stream", e)
        }
    }

    companion object {
        private val TAG = YtdlpStreamGobbler::class.java.simpleName
    }
}
