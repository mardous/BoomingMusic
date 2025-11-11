package com.mardous.booming.ui.screen.permissions

import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.app.AlarmManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.SpannableStringBuilder.SPAN_INCLUSIVE_INCLUSIVE
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.core.view.isVisible
import com.mardous.booming.R
import com.mardous.booming.databinding.ActivityPermissionBinding
import com.mardous.booming.extensions.hasS
import com.mardous.booming.extensions.hasT
import com.mardous.booming.extensions.resources.primaryColor
import com.mardous.booming.ui.component.base.AbsBaseActivity
import com.mardous.booming.ui.component.views.PermissionView
import com.mardous.booming.ui.screen.MainActivity

/**
 * @author Christians M. A. (mardous)
 */
class PermissionsActivity : AbsBaseActivity() {

    private var _binding: ActivityPermissionBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityPermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAppTitle()
        setupPermissionsVisibility()
        setupPermissionsOrder()

        binding.storageAccess.setButtonOnClickListener {
            requestPermissions()
        }
        if (binding.readImages.isVisible) {
            binding.readImages.setButtonOnClickListener {
                if (!binding.readImages.isGranted() && hasT()) {
                    ActivityCompat.requestPermissions(this,
                        arrayOf(READ_MEDIA_IMAGES), PERMISSION_REQUEST)
                }
            }
        }
        if (binding.nearbyDevices.isVisible) {
            binding.nearbyDevices.setButtonOnClickListener {
                if (!binding.nearbyDevices.isGranted() && hasS()) {
                    ActivityCompat.requestPermissions(this,
                        arrayOf(BLUETOOTH_CONNECT), BLUETOOTH_PERMISSION_REQUEST)
                }
            }
        }
        if (binding.scheduleExactAlarms.isVisible) {
            binding.scheduleExactAlarms.setButtonOnClickListener {
                if (!binding.scheduleExactAlarms.isGranted() && hasS()) {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                }
            }
        }
        binding.ringtone.setButtonOnClickListener {
            if (!binding.ringtone.isGranted()) {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.data = ("package:" + applicationContext.packageName).toUri()
                startSettingsActivity(intent)
            }
        }
        binding.finish.setOnClickListener {
            if (hasPermissions()) {
                startActivity(
                    Intent(this, MainActivity::class.java)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishAffinity()
                remove()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun setupAppTitle() {
        val appName = getString(R.string.app_name_long).trim()
        val styledAppName = SpannableStringBuilder(getString(R.string.welcome_to_x, appName).trim()).apply {
            setSpan(StyleSpan(Typeface.BOLD), this.indexOf(appName), length, SPAN_INCLUSIVE_INCLUSIVE)
            setSpan(ForegroundColorSpan(primaryColor()), this.lastIndexOf(" "), length, SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        binding.welcomeLabel.text = styledAppName
    }

    private fun setupPermissionsVisibility() {
        binding.readImages.isVisible = hasT()
        binding.nearbyDevices.isVisible = hasS()
        binding.scheduleExactAlarms.isVisible = hasS()
    }

    private fun setupPermissionsOrder() {
        var order = 0
        for (i in 0 until binding.permissionsColumn.childCount) {
            val child = binding.permissionsColumn.getChildAt(i)
            if (child is PermissionView && child.isVisible) {
                child.setNumber(++order)
            }
        }
    }

    private fun startSettingsActivity(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
        }
    }

    private fun checkPermissions() {
        binding.storageAccess.setGranted(hasPermissions())
        binding.ringtone.setGranted(canWriteSettings())
        if (hasS()) {
            binding.nearbyDevices.setGranted(hasNearbyDevicesPermission())
            binding.scheduleExactAlarms.setGranted(canScheduleExactAlarms())
        }
        if (hasT()) {
            binding.readImages.setGranted(hasReadImagesPermission())
        }
        binding.finish.isEnabled = binding.storageAccess.isGranted() && (!hasS() || binding.nearbyDevices.isGranted())
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun hasNearbyDevicesPermission(): Boolean =
        checkSelfPermission(BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun hasReadImagesPermission(): Boolean =
        checkSelfPermission(READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED

    @RequiresApi(Build.VERSION_CODES.S)
    private fun canScheduleExactAlarms(): Boolean =
        getSystemService<AlarmManager>()?.canScheduleExactAlarms() == true

    private fun canWriteSettings(): Boolean = Settings.System.canWrite(this)
}