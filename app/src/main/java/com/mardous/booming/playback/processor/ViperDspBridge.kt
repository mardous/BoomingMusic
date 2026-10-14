package com.mardous.booming.playback.processor

import java.nio.ByteBuffer

class ViperDspBridge {
    companion object {
        init {
            System.loadLibrary("viper_dsp")
        }
    }

    external fun init(sampleRate: Int)

    external fun setDynamicBass(enabled: Boolean, presetIndex: Int, gainPct: Float)

    external fun setExciter(enabled: Boolean, mode: Int, cutoffHz: Float, gainDb: Float)

    /**
     * Processes PCM stereo samples in-place for multiple formats.
     * @param byteBuffer direct byte buffer containing audio data
     * @param offsetBytes starting position in bytes
     * @param lengthBytes length in bytes to process
     * @param encoding C.ENCODING_PCM_... format
     */
    external fun process(byteBuffer: ByteBuffer, offsetBytes: Int, lengthBytes: Int, encoding: Int)
}
