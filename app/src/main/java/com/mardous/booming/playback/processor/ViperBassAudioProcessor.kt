package com.mardous.booming.playback.processor

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

@OptIn(UnstableApi::class)
class ViperBassAudioProcessor : BaseAudioProcessor() {

    private val viperBridge = ViperDspBridge()

    private var sampleRateHz: Int = -1

    // State for DSP parameters
    private var dynBassEnabled = false
    private var dynBassPreset = 0
    private var dynBassGainPct = 33f

    private var viperBassEnabled = false
    private var viperBassMode = 0
    private var viperBassCutoffHz = 60f
    private var viperBassGainDb = 0f

    @Synchronized
    fun setDynamicBass(enabled: Boolean, presetIndex: Int, gainPct: Float) {
        dynBassEnabled = enabled
        dynBassPreset = presetIndex
        dynBassGainPct = gainPct
        viperBridge.setDynamicBass(enabled, presetIndex, gainPct)
    }

    @Synchronized
    fun setExciter(enabled: Boolean, mode: Int, cutoffHz: Float, gainDb: Float) {
        viperBassEnabled = enabled
        viperBassMode = mode
        viperBassCutoffHz = cutoffHz
        viperBassGainDb = gainDb
        viperBridge.setExciter(enabled, mode, cutoffHz, gainDb)
    }

    override fun isActive(): Boolean {
        // Active unconditionally to stay in the ExoPlayer chain
        return super.isActive()
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        val encoding = inputAudioFormat.encoding
        if (encoding != C.ENCODING_PCM_16BIT &&
            encoding != C.ENCODING_PCM_24BIT &&
            encoding != C.ENCODING_PCM_32BIT &&
            encoding != C.ENCODING_PCM_FLOAT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        
        // ViPER DSP works primarily with stereo
        if (inputAudioFormat.channelCount != 2) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }

        Log.d("ViperDSP", "onConfigure called: encoding=$encoding, sampleRate=${inputAudioFormat.sampleRate}, channels=${inputAudioFormat.channelCount}")

        if (sampleRateHz != inputAudioFormat.sampleRate) {
            sampleRateHz = inputAudioFormat.sampleRate
            viperBridge.init(sampleRateHz)
            
            // Re-apply settings to the newly initialized C++ instance
            viperBridge.setDynamicBass(dynBassEnabled, dynBassPreset, dynBassGainPct)
            viperBridge.setExciter(viperBassEnabled, viperBassMode, viperBassCutoffHz, viperBassGainDb)
        }

        // Return the exact same format so ExoPlayer doesn't insert an unneeded converter
        return inputAudioFormat
    }

    private var logCounter = 0

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val buffer = replaceOutputBuffer(remaining)
        buffer.order(ByteOrder.nativeOrder())

        // Copy data to output buffer
        buffer.put(inputBuffer)
        buffer.flip()

        if ((dynBassEnabled || viperBassEnabled) && buffer.isDirect) {
            if (++logCounter % 500 == 0) {
                Log.d("ViperDSP", "queueInput: processing buffer of size $remaining bytes. dynBass=$dynBassEnabled, viperBass=$viperBassEnabled")
            }
            synchronized(this) {
                // Process the buffer in-place natively based on encoding
                viperBridge.process(buffer, buffer.position(), buffer.remaining(), inputAudioFormat.encoding)
            }
        } else {
             if (++logCounter % 500 == 0) {
                Log.d("ViperDSP", "queueInput: bypassing buffer of size $remaining bytes. dynBass=$dynBassEnabled, viperBass=$viperBassEnabled")
            }
        }
    }
}
