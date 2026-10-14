#include <jni.h>
#include <vector>
#include <algorithm>
#include <cmath>
#include <android/log.h>
#include "DynamicBassDsp.h"
#include "BassExciterDsp.h"

#define LOG_TAG "BoomingViPerDsp"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

static DynamicBassDsp gDynBass;
static BassExciterDsp gExciter;

static bool gDynBassEnabled = false;
static int gDynBassPreset = 0;
static float gDynBassGainPct = 33.0f;

static bool gExciterEnabled = false;
static int gExciterMode = 0;
static float gExciterCutoffHz = 60.0f;
static float gExciterGainDb = 0.0f;

static float gSampleRate = 48000.0f;
static float gEnvState = 0.0f;

static std::vector<float> gLeftChannel;
static std::vector<float> gRightChannel;

inline float smoothLimit(float x) {
    const float T = 0.90f;
    float ax = std::fabs(x);
    if (ax <= T) return x;
    float sign = (x >= 0.0f) ? 1.0f : -1.0f;
    return sign * (T + (1.0f - T) * std::tanh((ax - T) / (1.0f - T)));
}

extern "C" {

JNIEXPORT void JNICALL
Java_com_mardous_booming_playback_processor_ViperDspBridge_init(
JNIEnv* env, jobject thiz, jint sampleRate) {
    LOGI("Init called with sampleRate: %d", sampleRate);
    gSampleRate = static_cast<float>(sampleRate);
    gDynBass.setSampleRate(gSampleRate);
    gExciter.setSampleRate(gSampleRate);

    gDynBass.reset();
    gExciter.reset();

    // Re-apply static state
    gDynBass.setPresetAndGain(gDynBassPreset, gDynBassGainPct);
    gExciter.setBand1(gExciterMode, gExciterCutoffHz, gExciterGainDb);
}

JNIEXPORT void JNICALL
Java_com_mardous_booming_playback_processor_ViperDspBridge_setDynamicBass(
JNIEnv* env, jobject thiz, jboolean enabled, jint presetIndex, jfloat gainPct) {
    LOGI("setDynamicBass: enabled=%d, preset=%d, gainPct=%.1f", enabled, presetIndex, gainPct);
    gDynBassEnabled = enabled;
    gDynBassPreset = presetIndex;
    gDynBassGainPct = gainPct;
    gDynBass.setPresetAndGain(presetIndex, gainPct);
}

JNIEXPORT void JNICALL
Java_com_mardous_booming_playback_processor_ViperDspBridge_setExciter(
JNIEnv* env, jobject thiz, jboolean enabled, jint mode, jfloat cutoffHz, jfloat gainDb) {
    LOGI("setExciter: enabled=%d, mode=%d, cutoffHz=%.1f, gainDb=%.1f", enabled, mode, cutoffHz, gainDb);
    gExciterEnabled = enabled;
    gExciterMode = mode;
    gExciterCutoffHz = cutoffHz;
    gExciterGainDb = gainDb;
    gExciter.setBand1(mode, cutoffHz, gainDb);
}

JNIEXPORT void JNICALL
Java_com_mardous_booming_playback_processor_ViperDspBridge_process(
JNIEnv* env, jobject thiz, jobject byteBuffer, jint offsetBytes, jint lengthBytes, jint encoding) {

    if (!gDynBassEnabled && !gExciterEnabled) return;

    auto* rawBytes = static_cast<uint8_t*>(env->GetDirectBufferAddress(byteBuffer));
    if (!rawBytes) return;

    rawBytes += offsetBytes;

    int bytesPerSample = 2;
    if (encoding == 2) bytesPerSample = 2;         // 16-bit
    else if (encoding == 4 || encoding == 0x30000000) bytesPerSample = 4; // float or 32-bit
    else if (encoding == 0x20000000) bytesPerSample = 3; // 24-bit
    else return; // Unsupported

    int bytesPerFrame = bytesPerSample * 2;
    int numFrames = lengthBytes / bytesPerFrame;

    if (numFrames <= 0) return;

    if (gLeftChannel.size() < static_cast<size_t>(numFrames)) {
        gLeftChannel.resize(numFrames);
        gRightChannel.resize(numFrames);
    }

    // Calculate auto-gain to prevent early clipping
    float preGainDb = 0.0f;
    if (gDynBassEnabled) {
        preGainDb -= (gDynBassGainPct / 100.0f) * 2.0f;
    }
    if (gExciterEnabled) {
        preGainDb -= (gExciterGainDb / 15.0f) * 1.5f;
    }
    float preGainLinear = std::pow(10.0f, preGainDb / 20.0f);

    // 1. Деинтерливинг и конвертация во Float
    for (int i = 0; i < numFrames; ++i) {
        float l = 0.0f, r = 0.0f;
        int offsetL = i * bytesPerFrame;
        int offsetR = offsetL + bytesPerSample;

        if (encoding == 2) { // 16-bit
            int16_t sl = *reinterpret_cast<int16_t*>(rawBytes + offsetL);
            int16_t sr = *reinterpret_cast<int16_t*>(rawBytes + offsetR);
            l = static_cast<float>(sl) * (1.0f / 32768.0f);
            r = static_cast<float>(sr) * (1.0f / 32768.0f);
        } else if (encoding == 4 || encoding == 0x30000000) { // Float or 32-bit (handled same if treating as Int but we do different conversions below)
            l = *reinterpret_cast<float*>(rawBytes + offsetL);
            r = *reinterpret_cast<float*>(rawBytes + offsetR);
        } else if (encoding == 0x20000000) { // 24-bit
            int32_t sl = (rawBytes[offsetL]) | (rawBytes[offsetL+1] << 8) | (rawBytes[offsetL+2] << 16);
            if (sl & 0x00800000) sl |= static_cast<int32_t>(0xFF000000);
            int32_t sr = (rawBytes[offsetR]) | (rawBytes[offsetR+1] << 8) | (rawBytes[offsetR+2] << 16);
            if (sr & 0x00800000) sr |= static_cast<int32_t>(0xFF000000);
            l = static_cast<float>(sl) * (1.0f / 8388608.0f);
            r = static_cast<float>(sr) * (1.0f / 8388608.0f);
        } else if (encoding == 0x30000000) { // 32-bit
            int32_t sl = *reinterpret_cast<int32_t*>(rawBytes + offsetL);
            int32_t sr = *reinterpret_cast<int32_t*>(rawBytes + offsetR);
            l = static_cast<float>(sl) * (1.0f / 2147483520.0f);
            r = static_cast<float>(sr) * (1.0f / 2147483520.0f);
        }

        l *= preGainLinear;
        r *= preGainLinear;

        if (gExciterEnabled) {
            l = gExciter.processSample(l, 0);
            r = gExciter.processSample(r, 1);
        }
        gLeftChannel[i] = l;
        gRightChannel[i] = r;
    }

    // 2. Dynamic Bass
    if (gDynBassEnabled) {
        gDynBass.processBlock(gLeftChannel.data(), gRightChannel.data(), numFrames);
    }

    // 3. Envelope Peak Limiter
    float attackCoef = std::exp(-1.0f / (0.0005f * gSampleRate)); // 0.5ms
    float releaseCoef = std::exp(-1.0f / (0.080f * gSampleRate)); // 80ms

    for (int i = 0; i < numFrames; ++i) {
        float peak = std::max(std::fabs(gLeftChannel[i]), std::fabs(gRightChannel[i]));
        if (peak > gEnvState) {
            gEnvState = attackCoef * gEnvState + (1.0f - attackCoef) * peak;
        } else {
            gEnvState = releaseCoef * gEnvState + (1.0f - releaseCoef) * peak;
        }

        float envGain = 1.0f;
        if (gEnvState > 0.92f) {
            envGain = 0.92f / gEnvState;
        }

        gLeftChannel[i] *= envGain;
        gRightChannel[i] *= envGain;

        // Smooth Soft-Knee Clipper
        gLeftChannel[i] = smoothLimit(gLeftChannel[i]);
        gRightChannel[i] = smoothLimit(gRightChannel[i]);
    }

    // 4. Сборка обратно
    for (int i = 0; i < numFrames; ++i) {
        float l = gLeftChannel[i];
        float r = gRightChannel[i];

        int offsetL = i * bytesPerFrame;
        int offsetR = offsetL + bytesPerSample;

        if (encoding == 2) {
            *reinterpret_cast<int16_t*>(rawBytes + offsetL) = static_cast<int16_t>(std::clamp(l * 32767.0f, -32768.0f, 32767.0f));
            *reinterpret_cast<int16_t*>(rawBytes + offsetR) = static_cast<int16_t>(std::clamp(r * 32767.0f, -32768.0f, 32767.0f));
        } else if (encoding == 4) {
            *reinterpret_cast<float*>(rawBytes + offsetL) = l;
            *reinterpret_cast<float*>(rawBytes + offsetR) = r;
        } else if (encoding == 0x20000000) {
            int32_t outL = static_cast<int32_t>(std::clamp(l * 8388607.0f, -8388608.0f, 8388607.0f));
            rawBytes[offsetL] = outL & 0xFF;
            rawBytes[offsetL+1] = (outL >> 8) & 0xFF;
            rawBytes[offsetL+2] = (outL >> 16) & 0xFF;

            int32_t outR = static_cast<int32_t>(std::clamp(r * 8388607.0f, -8388608.0f, 8388607.0f));
            rawBytes[offsetR] = outR & 0xFF;
            rawBytes[offsetR+1] = (outR >> 8) & 0xFF;
            rawBytes[offsetR+2] = (outR >> 16) & 0xFF;
        } else if (encoding == 0x30000000) {
            *reinterpret_cast<int32_t*>(rawBytes + offsetL) = static_cast<int32_t>(std::clamp(l * 2147483520.0f, -2147483648.0f, 2147483520.0f));
            *reinterpret_cast<int32_t*>(rawBytes + offsetR) = static_cast<int32_t>(std::clamp(r * 2147483520.0f, -2147483648.0f, 2147483520.0f));
        }
    }
}

} // extern "C"