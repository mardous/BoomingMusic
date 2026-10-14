#pragma once
#include <cmath>
#include <cstring>

class BassExciterDsp {
    int mMode = 0;
    float mFs = 48000.0f;
    float mCutoff = 60.0f;
    float mGainDb = 0.0f;
    float mBoostLinear = 0.0f;

    float lpf_b0[2], lpf_b1[2], lpf_b2[2], lpf_a1[2], lpf_a2[2];
    float lpf_z[2][2][2]{};

    float hpf_b0, hpf_b1, hpf_b2, hpf_a1, hpf_a2;
    float hpf_z[2][2]{};

    float apf_b0, apf_b1, apf_b2, apf_a1, apf_a2;
    float apf_z[2][2]{};

    float sub_lpf_b0[2], sub_lpf_b1[2], sub_lpf_b2[2], sub_lpf_a1[2], sub_lpf_a2[2];
    float sub_lpf_z[2][2][2]{};

    void designLPF(float fs, float fc, float q, float* b0, float* b1, float* b2, float* a1, float* a2) {
        float w0 = 2.0f * 3.14159265358979f * fc / fs;
        float alpha = std::sin(w0) / (2.0f * q);
        float cosw0 = std::cos(w0);
        float a0 = 1.0f + alpha;
        *b0 = ((1.0f - cosw0) * 0.5f) / a0;
        *b1 = (1.0f - cosw0) / a0;
        *b2 = *b0;
        *a1 = (-2.0f * cosw0) / a0;
        *a2 = (1.0f - alpha) / a0;
    }

    void designHPF(float fs, float fc, float q, float* b0, float* b1, float* b2, float* a1, float* a2) {
        float w0 = 2.0f * 3.14159265358979f * fc / fs;
        float alpha = std::sin(w0) / (2.0f * q);
        float cosw0 = std::cos(w0);
        float a0 = 1.0f + alpha;
        *b0 = ((1.0f + cosw0) * 0.5f) / a0;
        *b1 = -(1.0f + cosw0) / a0;
        *b2 = *b0;
        *a1 = (-2.0f * cosw0) / a0;
        *a2 = (1.0f - alpha) / a0;
    }

    void designAPF(float fs, float fc, float q, float* b0, float* b1, float* b2, float* a1, float* a2) {
        float w0 = 2.0f * 3.14159265358979f * fc / fs;
        float alpha = std::sin(w0) / (2.0f * q);
        float cosw0 = std::cos(w0);
        float a0 = 1.0f + alpha;
        *b0 = (1.0f - alpha) / a0;
        *b1 = (-2.0f * cosw0) / a0;
        *b2 = (1.0f + alpha) / a0;
        *a1 = *b1;
        *a2 = *b0;
    }

    float processBiquad(float x, int ch, int stage, const float* b0, const float* b1, const float* b2, const float* a1, const float* a2, float z[2][2][2]) {
        float y = x * b0[stage] + z[ch][stage][0];
        z[ch][stage][0] = x * b1[stage] - y * a1[stage] + z[ch][stage][1];
        z[ch][stage][1] = x * b2[stage] - y * a2[stage];
        return y;
    }

    float processBiquad1(float x, int ch, float b0, float b1, float b2, float a1, float a2, float z[2][2]) {
        float y = x * b0 + z[ch][0];
        z[ch][0] = x * b1 - y * a1 + z[ch][1];
        z[ch][1] = x * b2 - y * a2;
        return y;
    }

public:
    void setSampleRate(float fs) { mFs = (fs < 8000.0f) ? 48000.0f : fs; updateFilters(); }

    void setBand1(int mode, float cutoffHz, float gainDb) {
        mMode = mode;
        mCutoff = cutoffHz;
        mGainDb = gainDb;
        mBoostLinear = (gainDb / 15.0f) * 2.2f;
        if (mBoostLinear < 0.0f) mBoostLinear = 0.0f;
        updateFilters();
    }

    void updateFilters() {
        designHPF(mFs, 28.0f, 0.7071f, &hpf_b0, &hpf_b1, &hpf_b2, &hpf_a1, &hpf_a2);
        float q = (mMode == 1) ? 1.05f : 0.7071f;

        designLPF(mFs, mCutoff, q, &lpf_b0[0], &lpf_b1[0], &lpf_b2[0], &lpf_a1[0], &lpf_a2[0]);
        designLPF(mFs, mCutoff, q, &lpf_b0[1], &lpf_b1[1], &lpf_b2[1], &lpf_a1[1], &lpf_a2[1]);

        // Всегда рассчитываем APF
        designAPF(mFs, mCutoff, 0.7071f, &apf_b0, &apf_b1, &apf_b2, &apf_a1, &apf_a2);

        if (mMode == 2) {
            designLPF(mFs, mCutoff * 2.0f, 0.7071f, &sub_lpf_b0[0], &sub_lpf_b1[0], &sub_lpf_b2[0], &sub_lpf_a1[0], &sub_lpf_a2[0]);
            designLPF(mFs, mCutoff * 2.0f, 0.7071f, &sub_lpf_b0[1], &sub_lpf_b1[1], &sub_lpf_b2[1], &sub_lpf_a1[1], &sub_lpf_a2[1]);
        }
    }

    void reset() {
        std::memset(lpf_z, 0, sizeof(lpf_z));
        std::memset(hpf_z, 0, sizeof(hpf_z));
        std::memset(apf_z, 0, sizeof(apf_z));
        std::memset(sub_lpf_z, 0, sizeof(sub_lpf_z));
    }

    float processSample(float x, int ch) {
        if (mBoostLinear <= 0.001f) return x;

        float dry = processBiquad1(x, ch, hpf_b0, hpf_b1, hpf_b2, hpf_a1, hpf_a2, hpf_z);
        float bass = processBiquad(dry, ch, 0, lpf_b0, lpf_b1, lpf_b2, lpf_a1, lpf_a2, lpf_z);

        if (mMode == 0) {
            return dry + bass * mBoostLinear * 0.7f;
        } else if (mMode == 1) {
            bass = processBiquad(bass, ch, 1, lpf_b0, lpf_b1, lpf_b2, lpf_a1, lpf_a2, lpf_z);
            float phaseShiftedDry = processBiquad1(dry, ch, apf_b0, apf_b1, apf_b2, apf_a1, apf_a2, apf_z);
            return phaseShiftedDry + bass * mBoostLinear;
        } else if (mMode == 2) {
            bass = processBiquad(bass, ch, 1, lpf_b0, lpf_b1, lpf_b2, lpf_a1, lpf_a2, lpf_z);
            float phaseShiftedDry = processBiquad1(dry, ch, apf_b0, apf_b1, apf_b2, apf_a1, apf_a2, apf_z);

            float drive = bass * (1.5f + 0.1f * mGainDb);
            float harm = std::tanh(drive) + 0.25f * drive * drive * (drive >= 0.0f ? 1.0f : -1.0f);
            harm = processBiquad(harm, ch, 0, sub_lpf_b0, sub_lpf_b1, sub_lpf_b2, sub_lpf_a1, sub_lpf_a2, sub_lpf_z);
            harm = processBiquad(harm, ch, 1, sub_lpf_b0, sub_lpf_b1, sub_lpf_b2, sub_lpf_a1, sub_lpf_a2, sub_lpf_z);

            return phaseShiftedDry + (bass * 0.75f + harm * 0.45f) * mBoostLinear;
        }
        return dry;
    }
};
