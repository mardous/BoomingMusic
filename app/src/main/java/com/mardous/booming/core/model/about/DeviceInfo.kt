package com.mardous.booming.core.model.about

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.IntRange
import androidx.appcompat.app.AppCompatDelegate
import com.mardous.booming.BuildConfig
import com.mardous.booming.util.Preferences
import java.util.Locale

class DeviceInfo {
    @SuppressLint("NewApi")
    private val abis = Build.SUPPORTED_ABIS

    @SuppressLint("NewApi")
    private val abis32Bits = Build.SUPPORTED_32_BIT_ABIS

    @SuppressLint("NewApi")
    private val abis64Bits = Build.SUPPORTED_64_BIT_ABIS
    private val baseTheme: String = Preferences.generalTheme
    private val brand = Build.BRAND
    private val buildID = Build.DISPLAY
    private val buildVersion = Build.VERSION.INCREMENTAL
    private val device = Build.DEVICE
    private val hardware = Build.HARDWARE
    private val manufacturer = Build.MANUFACTURER
    private val model = Build.MODEL
    private val nowPlayingTheme: String = Preferences.nowPlayingScreen.name
    private val product = Build.PRODUCT
    private val releaseVersion = Build.VERSION.RELEASE

    @IntRange(from = 0)
    private val sdkVersion = Build.VERSION.SDK_INT
    private var versionCode = BuildConfig.VERSION_CODE
    private var versionName = BuildConfig.VERSION_NAME
    private val selectedLang: String = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    fun toMarkdown(): String {
        return """
               Device info:
               ---
               <table>
               <tr><td><b>App version</b></td><td>$versionName</td></tr>
               <tr><td>App version code</td><td>$versionCode</td></tr>
               <tr><td>Android build version</td><td>$buildVersion</td></tr>
               <tr><td>Android release version</td><td>$releaseVersion</td></tr>
               <tr><td>Android SDK version</td><td>$sdkVersion</td></tr>
               <tr><td>Android build ID</td><td>$buildID</td></tr>
               <tr><td>Device brand</td><td>$brand</td></tr>
               <tr><td>Device manufacturer</td><td>$manufacturer</td></tr>
               <tr><td>Device name</td><td>$device</td></tr>
               <tr><td>Device model</td><td>$model</td></tr>
               <tr><td>Device product name</td><td>$product</td></tr>
               <tr><td>Device hardware name</td><td>$hardware</td></tr>
               <tr><td>ABIs</td><td>${abis.contentToString()}</td></tr>
               <tr><td>ABIs (32bit)</td><td>${abis32Bits.contentToString()}</td></tr>
               <tr><td>ABIs (64bit)</td><td>${abis64Bits.contentToString()}</td></tr>
               <tr><td>Language</td><td>$selectedLang</td></tr>
               </table>
               
               """.trimIndent()
    }

    override fun toString(): String {
        return """
            App version: $versionName
            App version code: $versionCode
            Android build version: $buildVersion
            Android release version: $releaseVersion
            Android SDK version: $sdkVersion
            Android build ID: $buildID
            Device brand: $brand
            Device manufacturer: $manufacturer
            Device name: $device
            Device model: $model
            Device product name: $product
            Device hardware name: $hardware
            ABIs: ${abis.contentToString()}
            ABIs (32bit): ${abis32Bits.contentToString()}
            ABIs (64bit): ${abis64Bits.contentToString()}
            Base theme: $baseTheme
            Now playing theme: $nowPlayingTheme
            System language: ${Locale.getDefault().toLanguageTag()}
            In-App Language: $selectedLang
            """.trimIndent()
    }
}