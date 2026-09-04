package com.mardous.booming.util

import android.content.Context
import androidx.annotation.XmlRes

/**
 * This version of PackageValidator validates all connections.
 *
 * At the moment, we're not sure how advisable this is.
 * See for more details: https://github.com/mardous/BoomingMusic/issues/550
 */
class PackageValidator(context: Context, @XmlRes xmlResId: Int = 0) {
    fun isKnownCaller(callingPackage: String, callingUid: Int): Boolean {
        return true
    }
}
