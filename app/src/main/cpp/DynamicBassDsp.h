#pragma once
#include <cmath>
#include <cstring>

class DynamicBassDsp {
    float mFs = 48000.0f;
    float mGainPct = 33.0f;
    float mX1 = 100.0f, mX2 = 5600.0f, mY1 = 40.0f, mY2 = 80.0f, mSgx = 50.0f, mSgy = 50.0f;

    float bp_b0, bp_b1, bp_b2, bp_a1, bp_a2;
    float bp_z[2][2]{};

    float sub_b0, sub_b1, sub_b2, sub_a1, sub_a2;
    float sub_z[2]{};

    float hpf_b0, hpf_b1, hpf_b2, hpf_a1, hpf_a2;
    float hpf_z[2][2]{};

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

    void designBPF(float fs, float fc, float q, float* b0, float* b1, float* b2, float* a1, float* a2) {
        float w0 = 2.0f * 3.14159265358979f * fc / fs;
        float alpha = std::sin(w0) / (2.0f * q);
        float cosw0 = std::cos(w0);
        float a0 = 1.0f + alpha;
        *b0 = alpha / a0;
        *b1 = 0.0f;
        *b2 = -alpha / a0;
        *a1 = (-2.0f * cosw0) / a0;
        *a2 = (1.0f - alpha) / a0;
    }

    float processBiquad(float x, int ch, float b0, float b1, float b2, float a1, float a2, float z[2][2]) {
        float y = x * b0 + z[ch][0];
        z[ch][0] = x * b1 - y * a1 + z[ch][1];
        z[ch][1] = x * b2 - y * a2;
        return y;
    }

    float processMonoBiquad(float x, float b0, float b1, float b2, float a1, float a2, float z[2]) {
        float y = x * b0 + z[0];
        z[0] = x * b1 - y * a1 + z[1];
        z[1] = x * b2 - y * a2;
        return y;
    }

public:
    void setSampleRate(float fs) { mFs = (fs < 8000.0f) ? 48000.0f : fs; apply(); }

    void setPresetAndGain(int presetIndex, float gainPct) {
        mGainPct = gainPct;
        switch(presetIndex) {
            case 1: mX1=180; mX2=5800; mY1=55; mY2=80; mSgx=10; mSgy=70; break;
            case 2: mX1=120; mX2=6000; mY1=40; mY2=100; mSgx=20; mSgy=80; break;
            case 3: mX1=300; mX2=5600; mY1=60; mY2=100; mSgx=50; mSgy=20; break;
            case 4: mX1=90;  mX2=6200; mY1=45; mY2=85; mSgx=15; mSgy=10; break;
            default: mX1=100; mX2=5600; mY1=40; mY2=80; mSgx=50; mSgy=50; break;
        }
        apply();
    }

    void reset() {
        std::memset(hpf_z, 0, sizeof(hpf_z));
        std::memset(bp_z, 0, sizeof(bp_z));
        std::memset(sub_z, 0, sizeof(sub_z));
        apply();
    }

    void processBlock(float* l, float* r, int n) {
        if (mGainPct <= 0.1f) return;

        float boost = (mGainPct / 100.0f) * 1.8f;

        for (int i = 0; i < n; i++) {
            float dl = processBiquad(l[i], 0, hpf_b0, hpf_b1, hpf_b2, hpf_a1, hpf_a2, hpf_z);
            float dr = processBiquad(r[i], 1, hpf_b0, hpf_b1, hpf_b2, hpf_a1, hpf_a2, hpf_z);

            float bpl = processBiquad(dl, 0, bp_b0, bp_b1, bp_b2, bp_a1, bp_a2, bp_z);
            float bpr = processBiquad(dr, 1, bp_b0, bp_b1, bp_b2, bp_a1, bp_a2, bp_z);

            float monoSub = processMonoBiquad(0.5f * (dl + dr), sub_b0, sub_b1, sub_b2, sub_a1, sub_a2, sub_z);

            l[i] = dl + (bpl + monoSub) * boost;
            r[i] = dr + (bpr + monoSub) * boost;
        }
    }

private:
    void apply() {
        designHPF(mFs, 28.0f, 0.7071f, &hpf_b0, &hpf_b1, &hpf_b2, &hpf_a1, &hpf_a2);

        float centerFreq = (mY1 + mY2) * 0.5f;
        float q = 0.65f + (mSgx + mSgy) * 0.004f;
        designBPF(mFs, centerFreq, q, &bp_b0, &bp_b1, &bp_b2, &bp_a1, &bp_a2);

        designLPF(mFs, mX1 * 0.5f, 0.7071f, &sub_b0, &sub_b1, &sub_b2, &sub_a1, &sub_a2);
    }
};
