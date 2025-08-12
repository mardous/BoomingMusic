package com.mardous.booming.ui.screen.error

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import cat.ereza.customactivityoncrash.CustomActivityOnCrash
import com.mardous.booming.R
import com.mardous.booming.ui.component.base.AbsThemeActivity
import com.mardous.booming.appContext
import com.mardous.booming.databinding.ActivityErrorBinding
import com.mardous.booming.extensions.applyWindowInsets
import com.mardous.booming.extensions.fileProviderAuthority
import com.mardous.booming.extensions.files.asFormattedFileTime
import com.mardous.booming.extensions.openUrl
import com.mardous.booming.extensions.showToast
import com.mardous.booming.ui.screen.about.ISSUE_TRACKER_LINK
import java.io.File

/**
 * @author Christians M. A. (mardous)
 */
class ErrorActivity : AbsThemeActivity() {

    private var _binding: ActivityErrorBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = CustomActivityOnCrash.getConfigFromIntent(intent)
        if (config == null) {
            finish()
            return
        }
        val errorReport = CustomActivityOnCrash.getAllErrorDetailsFromIntent(this, intent)
        val nameFromTime = System.currentTimeMillis().asFormattedFileTime()
        val errorReportFile = File(appContext().filesDir, "Crash_${nameFromTime}.log")
        if (!errorReportFile.exists() || errorReportFile.delete()) {
            errorReportFile.writeText(errorReport)
        }

        _binding = ActivityErrorBinding.inflate(layoutInflater)
        binding.root.applyWindowInsets(top = true, left = true, right = true, bottom = true)
        binding.errorReportText.text = errorReport
        binding.openGithub.setOnClickListener {
            openGithub(errorReport)
        }
        binding.sendReport.setOnClickListener {
            sendFile(errorReportFile)
        }
        binding.restartApp.setOnClickListener {
            CustomActivityOnCrash.restartApplication(this, config)
        }
        setContentView(binding.root)
    }

    private fun openGithub(report: String) {
        val clipboardManager = getSystemService<ClipboardManager>()
        val clipData = ClipData.newPlainText(getString(R.string.uncaught_error_report), report)
        clipboardManager?.setPrimaryClip(clipData)
        openUrl(CREATE_ISSUE_URL)
        showToast(R.string.uncaught_error_report_copied)
    }

    private fun sendFile(file: File) {
        val intent = ShareCompat.IntentBuilder(this)
            .setSubject("${getString(R.string.app_name)} - crash log")
            .setText("Please, add a description of the problem")
            .setType("*/*")
            .setStream(FileProvider.getUriForFile(this, fileProviderAuthority, file))
            .setChooserTitle(R.string.uncaught_error_send)
            .createChooserIntent()

        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        private const val CREATE_ISSUE_URL = "${ISSUE_TRACKER_LINK}/new?template=bug_report.yaml"
    }
}