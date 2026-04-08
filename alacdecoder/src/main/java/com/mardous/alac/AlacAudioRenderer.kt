package com.mardous.alac

import android.os.Handler
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.decoder.CryptoConfig
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DecoderAudioRenderer

@UnstableApi
class AlacAudioRenderer(
    eventHandler: Handler?,
    eventListener: AudioRendererEventListener?,
    audioSink: AudioSink
) : DecoderAudioRenderer<AlacDecoder>(eventHandler, eventListener, audioSink) {

    override fun getName(): String = "AlacAudioRenderer"

    override fun supportsFormatInternal(format: Format): Int {
        if (format.cryptoType != C.CRYPTO_TYPE_NONE) {
            return C.FORMAT_UNSUPPORTED_DRM
        }

        val mimeType = format.sampleMimeType
        if (!MimeTypes.AUDIO_ALAC.equals(mimeType, ignoreCase = true)) {
            return C.FORMAT_UNSUPPORTED_TYPE
        }

        val bitDepth = parseBitDepthFromAlacSpecificConfig(format)
            ?: return C.FORMAT_UNSUPPORTED_SUBTYPE

        val pcmEncoding = getPcmEncoding(bitDepth)

        val tempFormat = Util.getPcmFormat(pcmEncoding, format.channelCount, format.sampleRate)
        if (!sinkSupportsFormat(tempFormat)) {
            return C.FORMAT_UNSUPPORTED_SUBTYPE
        }

        return C.FORMAT_HANDLED
    }

    override fun createDecoder(format: Format, cryptoConfig: CryptoConfig?): AlacDecoder {
        val bitDepth = parseBitDepthFromAlacSpecificConfig(format)
            ?: throw IllegalArgumentException("Invalid or incomplete ALACSpecificConfig in initializationData")
        return AlacDecoder(
            numInputBuffers = DECODER_BUFFER_SIZE,
            numOutputBuffers = DECODER_BUFFER_SIZE,
            sampleRate = format.sampleRate,
            channels = format.channelCount,
            bitDepth = bitDepth,
            cookie = format.initializationData.getOrNull(0)
        )
    }

    override fun getOutputFormat(decoder: AlacDecoder): Format {
        val pcmEncoding = getPcmEncoding(decoder.bitDepth)
        return Util.getPcmFormat(pcmEncoding, decoder.channels, decoder.sampleRate)
    }

    private fun getPcmEncoding(bitDepth: Int): Int {
        return if (bitDepth == 20) C.ENCODING_PCM_24BIT else Util.getPcmEncoding(bitDepth)
    }

    private fun parseBitDepthFromAlacSpecificConfig(format: Format): Int? {
        val initData = format.initializationData
        if (initData.isEmpty()) {
            return null
        }

        // Require full ALACSpecificConfig to avoid desync between decoder and sink.
        val config = initData[0]
        if (config.size < 24) {
            return null
        }

        // Bit-depth is the second byte after the 4‑byte frameLength:
        // offset: 0-3 frameLength, 4 compatibleVersion, 5 bitDepth
        return when (val bitDepth = config[5].toInt() and 0xFF) {
            16, 20, 24, 32 -> bitDepth // Accept only common ALAC bit depths; reject anything else as invalid.
            else -> null
        }
    }

    companion object {
        private const val DECODER_BUFFER_SIZE = 16
    }
}