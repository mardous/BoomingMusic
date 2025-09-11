/*
 * Copyright (C) 2010-2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mardous.booming.service.equalizer;

/**
 * OpenSL ES constants class
 */
public final class OpenSLESConstants {

    private OpenSLESConstants() {
        // Empty constructor
    }

    /**
     * The minimum loudness gain2
     */
    public static final int MINIMUM_LOUDNESS_GAIN = 0;
    /**
     * The maximum loudness gain2
     */
    public static final int MAXIMUM_LOUDNESS_GAIN = 4000;
    /**
     * The minimum bass boost strength in o/oo.
     */
    public static final short BASSBOOST_MIN_STRENGTH = 0;
    /**
     * The maximum bass boost strength in o/oo.
     */
    public static final short BASSBOOST_MAX_STRENGTH = 1000;
    /**
     * The minimum virtualizer strength in o/oo.
     */
    public static final short VIRTUALIZER_MIN_STRENGTH = 0;
    /**
     * The maximum virtualizer strength in o/oo.
     */
    public static final short VIRTUALIZER_MAX_STRENGTH = 1000;
}

