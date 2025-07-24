package com.mardous.booming

import com.google.firebase.Firebase
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.crashlytics

private val firebaseCrashlytics: FirebaseCrashlytics by lazy { Firebase.crashlytics }

fun recordException(throwable: Throwable) {
    firebaseCrashlytics.recordException(throwable)
}