package com.mardous.booming.util.oem

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.media.MediaRouter2
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Taken from [here](https://github.com/Moriafly/media-kit/blob/main/media-kit-core/src/main/java/com/moriafly/mediakit/core/oem/MiPlayAudioSupport.kt)
 * and [here](https://github.com/Yos-X/FlamingoSank/blob/master/app/src/main/java/yos/music/player/code/SystemMediaControlResolver.kt)
 */
object SystemMediaControlResolver {

    fun openMediaOutputSwitcher(context: Context) {
        when {
            MiPlayAudioSupport.supportMiPlay(context) -> {
                val intent = Intent().apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    setClassName(
                        "miui.systemui.plugin",
                        "miui.systemui.miplay.MiPlayDetailActivity"
                    )
                }
                if (!startIntent(context, intent)) {
                    startSystemMediaOutputSwitcher(context)
                }
            }

            // zh：临时禁用OneUI MediaActivity调用，等待未来确定不会被删除再添加回去
            (getOneUIVersionReadable() != null) -> {
                val intent = Intent().apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    setClassName(
                        "com.samsung.android.mdx.quickboard",
                        "com.samsung.android.mdx.quickboard.view.MediaActivity"
                    )
                }
                if (!startIntent(context, intent)) {
                    startSystemMediaOutputSwitcher(context)
                }
            }

            else -> {
                startSystemMediaOutputSwitcher(context)
            }
        }
    }

    private fun startSystemMediaOutputSwitcher(context: Context) {
        if (Build.VERSION.SDK_INT >= 34) {
            // Android 14 及以上
            startNativeMediaDialogForAndroid14(context)
        } else if (Build.VERSION.SDK_INT >= 31) {
            // Android 12 及以上
            val intent = Intent().apply {
                action = "com.android.systemui.action.LAUNCH_MEDIA_OUTPUT_DIALOG"
                setPackage("com.android.systemui")
                putExtra("package_name", context.packageName)
            }
            startNativeMediaDialog(context, intent)
        } else if (Build.VERSION.SDK_INT == 30) {
            // Android 11
            startNativeMediaDialogForAndroid11(context)
        } else {
            val intent = Intent().apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                action = "com.android.settings.panel.action.MEDIA_OUTPUT"
                putExtra("com.android.settings.panel.extra.PACKAGE_NAME", context.packageName)
            }
            startNativeMediaDialog(context, intent)
        }
    }

    private fun startNativeMediaDialog(context: Context, intent: Intent): Boolean {
        val resolveInfoList: List<ResolveInfo> =
            context.packageManager.queryIntentActivities(intent, 0)
        for (resolveInfo in resolveInfoList) {
            val activityInfo = resolveInfo.activityInfo
            val applicationInfo: ApplicationInfo? = activityInfo?.applicationInfo
            if (applicationInfo != null && (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) {
                context.startActivity(intent)
                return true
            }
        }
        return false
    }

    private fun startNativeMediaDialogForAndroid11(context: Context): Boolean {
        val intent = Intent().apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            action = "com.android.settings.panel.action.MEDIA_OUTPUT"
            putExtra("com.android.settings.panel.extra.PACKAGE_NAME", context.packageName)
        }
        val resolveInfoList: List<ResolveInfo> =
            context.packageManager.queryIntentActivities(intent, 0)
        for (resolveInfo in resolveInfoList) {
            val activityInfo = resolveInfo.activityInfo
            val applicationInfo: ApplicationInfo? = activityInfo?.applicationInfo
            if (applicationInfo != null && (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) {
                context.startActivity(intent)
                return true
            }
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun startNativeMediaDialogForAndroid14(context: Context): Boolean {
        val mediaRouter2 = MediaRouter2.getInstance(context)
        return mediaRouter2.showSystemOutputSwitcher()
    }

    private fun startIntent(context: Context, intent: Intent): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    @SuppressLint("PrivateApi")
    private fun getOneUIVersionReadable(): String? {
        return try {
            val systemProperties = Class.forName("android.os.SystemProperties")
            val get = systemProperties.getMethod("get", String::class.java)
            val value = (get.invoke(null, "ro.build.version.oneui") as String).trim()
            if (value.isEmpty()) return null
            val code = value.toIntOrNull() ?: return null
            val major = code / 10000
            val minor = (code / 100) % 100
            val patch = code % 100
            "$major.$minor.$patch"
        } catch (e: Exception) {
            null
        }
    }
}