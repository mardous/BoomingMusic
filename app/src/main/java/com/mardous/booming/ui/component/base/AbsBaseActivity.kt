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

import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import com.google.android.material.snackbar.Snackbar
import com.mardous.booming.R
import com.mardous.booming.extensions.getStoragePermissions
import com.mardous.booming.extensions.rootView

abstract class AbsBaseActivity : AbsThemeActivity() {

    private lateinit var permissions: Array<String>
    private var hadPermissions = false

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
            hadPermissions = hasPermissions
            onHasPermissionsChanged(hasPermissions)
        }
    }

    private fun requestPermissions() {
        requestPermissions(this, permissions, PERMISSION_REQUEST)
    }

    protected open fun onHasPermissionsChanged(hasPermissions: Boolean) {}

    protected open fun getPermissionsToRequest(): Array<String> {
        return getStoragePermissions().toTypedArray()
    }

    protected fun hasPermissions(): Boolean {
        return permissions.all {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST) {
            for (grantResult in grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    val shouldShowRationale = this.permissions.any {
                        shouldShowRequestPermissionRationale(this, it)
                    }
                    if (shouldShowRationale) {
                        Snackbar.make(snackBarContainer, getString(R.string.permissions_denied), Snackbar.LENGTH_SHORT)
                            .setAction(R.string.action_grant) { requestPermissions() }
                            .show()
                    } else {
                        Snackbar.make(snackBarContainer, getString(R.string.permissions_denied), Snackbar.LENGTH_INDEFINITE)
                            .setAction(R.string.settings_title) {
                                startActivity(
                                    Intent()
                                        .setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                        .setData(Uri.fromParts("package", packageName, null))
                                )
                            }
                            .show()
                    }
                    return
                }
            }
            hadPermissions = true
            onHasPermissionsChanged(true)
        }
    }

    protected open val snackBarContainer: View
        get() = rootView

    companion object {
        const val PERMISSION_REQUEST = 100
    }
}