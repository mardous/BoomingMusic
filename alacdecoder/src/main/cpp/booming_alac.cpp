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
#include "libalac/ALACDecoder.h"
#include "libalac/ALACBitUtilities.h"

#include <android/log.h>

#define LOG_TAG "booming_alac_decoder"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

struct AlacContext {
    ALACDecoder* decoder;
    uint32_t numChannels;
    uint32_t bitsPerSample;
};

extern "C" JNIEXPORT jlong JNICALL
Java_com_mardous_alac_AlacDecoder_nativeInit(
        JNIEnv *env, jobject,
        jint sample_rate, jint channels, jint bps, jbyteArray cookie) {
    auto* context = new AlacContext();
    context->decoder = new ALACDecoder();
    context->numChannels = channels;
    context->bitsPerSample = bps;

    if (cookie != nullptr) {
        jbyte* cookieData = env->GetByteArrayElements(cookie, nullptr);
        jsize cookieSize = env->GetArrayLength(cookie);

        context->decoder->Init(cookieData, cookieSize);

        env->ReleaseByteArrayElements(cookie, cookieData, JNI_ABORT);
    }

    return reinterpret_cast<jlong>(context);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_mardous_alac_AlacDecoder_nativeDecode(
        JNIEnv *env, jobject,
        jlong context_ptr, jobject input_buffer, jobject output_buffer,
        jint input_size) {
    auto* context = reinterpret_cast<AlacContext*>(context_ptr);

    auto* inputData = (uint8_t*)env->GetDirectBufferAddress(input_buffer);
    auto* outputData = (uint8_t*)env->GetDirectBufferAddress(output_buffer);

    BitBuffer bitBuf;
    BitBufferInit(&bitBuf, inputData, input_size);

    uint32_t numFrames = 0;
    int32_t status = context->decoder->Decode(&bitBuf, outputData, 4096, context->numChannels, &numFrames);

    if (status != 0) {
        LOGE("alac decode error: %d", status);
        return -1;
    }

    return static_cast<jint>(numFrames * context->numChannels * (context->bitsPerSample / 8));
}

extern "C" JNIEXPORT void JNICALL
Java_com_mardous_alac_AlacDecoder_nativeRelease(
        JNIEnv *env, jobject,
        jlong context_ptr) {
    auto* context = reinterpret_cast<AlacContext*>(context_ptr);
    delete context->decoder;
    delete context;
}