/*
 * Copyright (c) 2026 Christians Martínez Alvarado
 * Project: Booming Music
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

#include <jni.h>
#include <vector>
#include <new>
#include "libalac/ALACDecoder.h"
#include "libalac/ALACBitUtilities.h"

#include <android/log.h>

#define LOG_TAG "booming_alac_decoder"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

struct AlacContext {
    ALACDecoder* decoder;
    uint32_t numChannels;
    uint32_t bitsPerSample;
    uint32_t frameLength;
};

extern "C" JNIEXPORT jlong JNICALL
Java_com_mardous_alac_AlacDecoder_nativeInit(
        JNIEnv *env, jobject,
        jint sample_rate, jint channels, jint bps, jbyteArray cookie) {

    auto* context = new(std::nothrow) AlacContext();
    if (!context) return 0;

    context->decoder = new(std::nothrow) ALACDecoder();
    if (!context->decoder) {
        delete context;
        return 0;
    }

    context->numChannels = static_cast<uint32_t>(channels);
    context->bitsPerSample = static_cast<uint32_t>(bps);
    context->frameLength = kALACDefaultFrameSize;

    if (cookie != nullptr) {
        jsize cookieSize = env->GetArrayLength(cookie);
        jbyte* cookieData = env->GetByteArrayElements(cookie, nullptr);

        if (cookieData) {
            int32_t status = context->decoder->Init(cookieData, static_cast<uint32_t>(cookieSize));
            env->ReleaseByteArrayElements(cookie, cookieData, JNI_ABORT);

            if (status != ALAC_noErr) {
                LOGE("ALAC decoder init failed with status: %d", status);
                delete context->decoder;
                delete context;
                return 0;
            }
        }
    }

    // Cache the frameLength if the decoder set it during Init
    if (context->decoder->mConfig.frameLength > 0) {
        context->frameLength = context->decoder->mConfig.frameLength;
    }

    return reinterpret_cast<jlong>(context);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_mardous_alac_AlacDecoder_nativeGetFrameLength(
        JNIEnv *env, jobject,
        jlong context_ptr) {
    if (!context_ptr) return 0;
    auto* context = reinterpret_cast<AlacContext*>(context_ptr);
    return static_cast<jint>(context->frameLength);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_mardous_alac_AlacDecoder_nativeDecode(
        JNIEnv *env, jobject,
        jlong context_ptr, jobject input_buffer, jobject output_buffer,
        jint input_size) {

    if (!context_ptr) return -1;
    auto* context = reinterpret_cast<AlacContext*>(context_ptr);

    if (!input_buffer || !output_buffer) {
        LOGE("Decode: input or output buffer is null");
        return -1;
    }

    auto* inputData = static_cast<uint8_t*>(env->GetDirectBufferAddress(input_buffer));
    auto* outputData = static_cast<uint8_t*>(env->GetDirectBufferAddress(output_buffer));

    if (!inputData || !outputData) {
        LOGE("Decode: buffers must be direct ByteBuffers");
        return -1;
    }

    // Safety check: verify output buffer capacity
    jlong output_capacity = env->GetDirectBufferCapacity(output_buffer);
    uint32_t bytesPerSample = context->bitsPerSample / 8;
    uint32_t expectedOutputSize = context->frameLength * context->numChannels * bytesPerSample;

    if (output_capacity < expectedOutputSize) {
        LOGE("Decode: output buffer too small. Cap: %lld, Need: %u", (long long)output_capacity, expectedOutputSize);
        return -2; // Buffer too small error
    }

    BitBuffer bitBuf;
    BitBufferInit(&bitBuf, inputData, static_cast<uint32_t>(input_size));

    uint32_t numFrames = 0;
    int32_t status = context->decoder->Decode(&bitBuf, outputData, context->frameLength, context->numChannels, &numFrames);

    if (status != ALAC_noErr) {
        LOGE("alac decode error: %d", status);
        return -1;
    }

    return static_cast<jint>(numFrames * context->numChannels * bytesPerSample);
}

extern "C" JNIEXPORT void JNICALL
Java_com_mardous_alac_AlacDecoder_nativeRelease(
        JNIEnv *env, jobject,
        jlong context_ptr) {
    if (context_ptr) {
        auto* context = reinterpret_cast<AlacContext*>(context_ptr);
        delete context->decoder;
        delete context;
    }
}
