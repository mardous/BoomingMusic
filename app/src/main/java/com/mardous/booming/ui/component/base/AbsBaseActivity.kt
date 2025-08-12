/*
 * Copyright (c) 2024 Christians Martínez Alvarado
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

package com.mardous.booming.ui.component.base

import android.Manifest.permission.*
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import com.google.android.material.snackbar.Snackbar
import com.mardous.booming.R
import com.mardous.booming.extensions.hasR
import com.mardous.booming.extensions.hasT
import com.mardous.booming.extensions.rootView

abstract class AbsBaseActivity : AbsThemeActivity() {

    private lateinit var permissions: Array<String>
    private var hadPermissions = false
    private var permissionDeniedMessage: String? = null

    protected fun setPermissionDeniedMessage(message: String) {
        permissionDeniedMessage = message
    }

    private fun getPermissionDeniedMessage(): String {
        return permissionDeniedMessage ?: getString(R.string.permissions_denied)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_MUSIC
        permissions = getPermissionsToRequest()
        hadPermissions = hasPermissions()
    }

    override fun onResume() {
        super.onResume()
        val hasPermissions = hasPermissions()
        if (hasPermissions != hadPermissions) {
            hadPermissions = hasPermissions()
            onHasPermissionsChanged(hasPermissions())
        }
    }

    protected open fun onHasPermissionsChanged(hasPermissions: Boolean) {}

    protected fun requestPermissions() {
        requestPermissions(this, permissions, PERMISSION_REQUEST)
    }

    protected open fun getPermissionsToRequest(): Array<String> {
        return mutableSetOf<String>().apply {
            if (hasT()) {
                add(READ_MEDIA_AUDIO)
                add(POST_NOTIFICATIONS)
            } else {
                add(READ_EXTERNAL_STORAGE)
            }
            if (!hasR()) {
                add(WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }

    protected fun hasPermissions(): Boolean {
        for (permission in permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST) {
            for (grantResult in grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    if (shouldShowRequestPermissionRationale(this, READ_EXTERNAL_STORAGE) ||
                        shouldShowRequestPermissionRationale(this, WRITE_EXTERNAL_STORAGE)) {
                        // User has deny from permission dialog
                        Snackbar.make(snackBarContainer, getPermissionDeniedMessage(), Snackbar.LENGTH_SHORT)
                            .setAction(R.string.action_grant) { requestPermissions() }
                            .show()
                    } else {
                        // User has deny permission and checked never show permission dialog so you can redirect to Application settings page
                        Snackbar.make(snackBarContainer, getPermissionDeniedMessage(), Snackbar.LENGTH_INDEFINITE)
                            .setAction(R.string.settings_title) {
                                val intent = Intent().apply {
                                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                    data = Uri.fromParts("package", this@AbsBaseActivity.packageName, null)
                                }
                                startActivity(intent)
                            }
                            .show()
                    }
                    return
                }
            }
            hadPermissions = true
            onHasPermissionsChanged(true)
        } else if (requestCode == BLUETOOTH_PERMISSION_REQUEST) {
            for (grantResult in grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    if (shouldShowRequestPermissionRationale(this, BLUETOOTH_CONNECT)) {
                        // User has deny from permission dialog
                        Snackbar.make(snackBarContainer, R.string.permission_bluetooth_denied, Snackbar.LENGTH_SHORT)
                            .setAction(R.string.action_grant) {
                                requestPermissions(this, arrayOf(BLUETOOTH_CONNECT), BLUETOOTH_PERMISSION_REQUEST)
                            }
                            .show()
                    }
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_MENU && event.action == KeyEvent.ACTION_UP) {
            showOverflowMenu()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    protected open fun showOverflowMenu() {}

    protected open val snackBarContainer: View
        get() = rootView

    companion object {
        const val PERMISSION_REQUEST = 100
        const val BLUETOOTH_PERMISSION_REQUEST = 101
    }
}