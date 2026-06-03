package com.swiftsave.app.download

import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.YoutubeDLResponse
import java.io.File
import java.io.IOException
import java.util.ArrayList

/**
 * [YoutubeDL.execute] feeds [YtdlpProgressStreamExtractor] from **stdout** only, but yt-dlp prints
 * `[download] …%` lines to **stderr**. Merging stderr into stdout when a progress callback is used
 * matches the behavior of the upstream `redirectErrorStream` overload and restores live progress.
 */
internal object YtdlpDownloadExecutor {

    @Throws(YoutubeDLException::class, InterruptedException::class, YoutubeDL.CanceledException::class)
    fun executeWithProgressCallback(
        request: YoutubeDLRequest,
        processId: String?,
        callback: ((Float, Long, String) -> Unit)?,
    ): YoutubeDLResponse {
        val youtubeDl = youtubeDlSingleton()
        val idMap = processIdMap(youtubeDl)
        if (processId != null) {
            YoutubeDL.getInstance().destroyProcessById(processId)
        }
        if (processId != null && idMap.containsKey(processId)) {
            throw YoutubeDLException("Process ID already exists")
        }
        if (!request.hasOption("--cache-dir") || request.getOption("--cache-dir") == null) {
            request.addOption("--no-cache-dir")
        }
        if (request.buildCommand().contains("libaria2c.so")) {
            val ssl = stringField(youtubeDl, "ENV_SSL_CERT_FILE")
            request
                .addOption("--external-downloader-args", "aria2c:--summary-interval=1")
                .addOption("--external-downloader-args", "aria2c:--ca-certificate=$ssl")
        }
        val ffmpegPath = fileField(youtubeDl, "ffmpegPath")
            ?: throw YoutubeDLException("FFmpeg is not initialized")
        request.addOption("--ffmpeg-location", ffmpegPath.absolutePath)

        val outBuffer = StringBuffer()
        val errBuffer = StringBuffer()
        val startTime = System.currentTimeMillis()
        val command: MutableList<String> = ArrayList()
        val pythonPath = fileField(youtubeDl, "pythonPath")
            ?: throw YoutubeDLException("Python runtime is not initialized")
        val ytdlpPath = fileField(youtubeDl, "ytdlpPath")
            ?: throw YoutubeDLException("yt-dlp is not initialized")
        val binDir = fileField(youtubeDl, "binDir")
            ?: throw YoutubeDLException("yt-dlp binary directory is not initialized")
        command.add(pythonPath.absolutePath)
        command.add(ytdlpPath.absolutePath)
        command.addAll(request.buildCommand())

        val processBuilder = ProcessBuilder(command)
        processBuilder.redirectErrorStream(callback != null)
        processBuilder.environment().apply {
            this["LD_LIBRARY_PATH"] = stringField(youtubeDl, "ENV_LD_LIBRARY_PATH")
            this["SSL_CERT_FILE"] = stringField(youtubeDl, "ENV_SSL_CERT_FILE")
            this["PATH"] = listOfNotNull(System.getenv("PATH"), binDir.absolutePath).joinToString(":")
            this["PYTHONHOME"] = stringField(youtubeDl, "ENV_PYTHONHOME")
            this["HOME"] = stringField(youtubeDl, "ENV_PYTHONHOME")
            this["TMPDIR"] = stringField(youtubeDl, "TMPDIR")
        }

        val process =
            try {
                processBuilder.start()
            } catch (e: IOException) {
                throw YoutubeDLException(e)
            }
        if (processId != null) {
            idMap[processId] = process
        }
        val outStream = process.inputStream
        val errStream = process.errorStream
        val stdOutProcessor = YtdlpProgressStreamExtractor(outBuffer, outStream, callback)
        val stdErrProcessor = YtdlpStreamGobbler(errBuffer, errStream)
        val exitCode: Int =
            try {
                stdOutProcessor.join()
                stdErrProcessor.join()
                process.waitFor()
            } catch (e: InterruptedException) {
                process.destroy()
                if (processId != null) idMap.remove(processId)
                throw e
            }
        val out = outBuffer.toString()
        val err = errBuffer.toString()
        if (exitCode > 0) {
            if (processId != null && !idMap.containsKey(processId)) {
                throw YoutubeDL.CanceledException()
            }
            if (!ignoreErrors(request, out)) {
                processId?.let { idMap.remove(it) }
                throw YoutubeDLException(err)
            }
        }
        processId?.let { idMap.remove(it) }
        val elapsedTime = System.currentTimeMillis() - startTime
        return YoutubeDLResponse(command, exitCode, elapsedTime, out, err)
    }

    private fun ignoreErrors(request: YoutubeDLRequest, out: String): Boolean {
        return request.hasOption("--dump-json") && out.isNotEmpty() && request.hasOption("--ignore-errors")
    }

    private fun youtubeDlSingleton(): Any {
        val clazz = Class.forName("com.yausername.youtubedl_android.YoutubeDL")
        val instanceField = clazz.getDeclaredField("INSTANCE")
        instanceField.isAccessible = true
        return instanceField.get(null) ?: throw YoutubeDLException("YouTube-DL singleton is not initialized")
    }

    @Suppress("UNCHECKED_CAST")
    private fun processIdMap(youtubeDl: Any): MutableMap<String, Process> {
        val f = youtubeDl.javaClass.getDeclaredField("idProcessMap")
        f.isAccessible = true
        return f.get(youtubeDl) as MutableMap<String, Process>
    }

    private fun fileField(youtubeDl: Any, name: String): File? {
        val f = youtubeDl.javaClass.getDeclaredField(name)
        f.isAccessible = true
        return f.get(youtubeDl) as File?
    }

    private fun stringField(youtubeDl: Any, name: String): String {
        val f = youtubeDl.javaClass.getDeclaredField(name)
        f.isAccessible = true
        return f.get(youtubeDl) as String
    }
}
