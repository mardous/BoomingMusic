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

        val bitDepth = getBitDepth(format)
        val pcmEncoding = getPcmEncoding(bitDepth)

        val tempFormat = Util.getPcmFormat(pcmEncoding, format.channelCount, format.sampleRate)
        if (!sinkSupportsFormat(tempFormat)) {
            return C.FORMAT_UNSUPPORTED_SUBTYPE
        }

        return C.FORMAT_HANDLED
    }

    override fun createDecoder(format: Format, cryptoConfig: CryptoConfig?): AlacDecoder {
        val bitDepth = getBitDepth(format)
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

    private fun getBitDepth(format: Format): Int {
        return if (format.initializationData.isNotEmpty() &&
            format.initializationData[0].size >= 6) {
            format.initializationData[0][5].toInt()
        } else {
            16
        }
    }

    private fun getPcmEncoding(bitDepth: Int): Int {
        return if (bitDepth == 20) C.ENCODING_PCM_24BIT else Util.getPcmEncoding(bitDepth)
    }

    companion object {
        private const val DECODER_BUFFER_SIZE = 16
    }
}