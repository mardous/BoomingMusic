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

package com.mardous.booming.ui.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mardous.booming.R
import com.mardous.booming.extensions.create
import com.mardous.booming.extensions.files.saveTreeUri

class SAFDialog : DialogFragment() {

    private lateinit var documentTreeLauncher: ActivityResultLauncher<Uri?>

    interface SAFResultListener {
        fun onSAFResult(treeUri: Uri?)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        documentTreeLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { result ->
            val listener = listener
            if (result != null) {
                listener?.onSAFResult(context?.saveTreeUri(result))
            } else {
                listener?.onSAFResult(null)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.saf_access_required_title)
            .setMessage(R.string.saf_access_required_message)
            .setPositiveButton(R.string.saf_show_files_button, null)
            .create { dialog ->
                dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.setOnClickListener {
                    documentTreeLauncher.launch(null)
                }
            }
    }

    private val listener: SAFResultListener?
        get() = (parentFragment as? SAFResultListener) ?: (activity as? SAFResultListener)

    companion object {
        const val TAG = "SAFDialog"

        fun <T> show(activity: T) where T : AppCompatActivity, T : SAFResultListener? {
            SAFDialog().show(activity.supportFragmentManager, TAG)
        }

        fun <T> show(fragment: T) where T : Fragment, T : SAFResultListener? {
            SAFDialog().show(fragment.childFragmentManager, TAG)
        }
    }
}