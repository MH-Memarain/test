package com.arnika.session

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

object WavRecorder {
    private const val RATE = 16000

    @SuppressLint("MissingPermission")
    fun record(file: File, seconds: Int) {
        val min = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val bufferSize = maxOf(min, 4096)
        val recorder = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)
        val data = ByteArray(bufferSize)
        FileOutputStream(file).use { out ->
            writeHeader(out, 0)
            recorder.startRecording()
            val end = System.currentTimeMillis() + seconds * 1000L
            var total = 0
            while (System.currentTimeMillis() < end) {
                val n = recorder.read(data, 0, data.size)
                if (n > 0) { out.write(data, 0, n); total += n }
            }
            recorder.stop(); recorder.release()
            patchHeader(file, total)
        }
    }

    private fun writeHeader(out: FileOutputStream, pcmBytes: Int) {
        val byteRate = RATE * 2
        val total = pcmBytes + 36
        fun le32(v: Int) = byteArrayOf(v.toByte(), (v shr 8).toByte(), (v shr 16).toByte(), (v shr 24).toByte())
        fun le16(v: Int) = byteArrayOf(v.toByte(), (v shr 8).toByte())
        out.write("RIFF".toByteArray()); out.write(le32(total)); out.write("WAVEfmt ".toByteArray()); out.write(le32(16)); out.write(le16(1)); out.write(le16(1)); out.write(le32(RATE)); out.write(le32(byteRate)); out.write(le16(2)); out.write(le16(16)); out.write("data".toByteArray()); out.write(le32(pcmBytes))
    }

    private fun patchHeader(file: File, pcmBytes: Int) {
        RandomAccessFile(file, "rw").use { r ->
            fun writeLE32(v: Int) { r.write(byteArrayOf(v.toByte(), (v shr 8).toByte(), (v shr 16).toByte(), (v shr 24).toByte())) }
            r.seek(4); writeLE32(pcmBytes + 36)
            r.seek(40); writeLE32(pcmBytes)
        }
    }
}
