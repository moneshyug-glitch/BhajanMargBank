package com.bhajanmarg.app

// --- Android Framework Imports ---
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

// --- Project-specific Imports ---
import com.bhajanmarg.app.databinding.ActivityIntroBinding

// --- Third-party / Google Library Imports ---
import com.google.android.play.core.install.model.AppUpdateType
import com.google.androidbrowserhelper.trusted.TwaLauncher

const val PREF_NAME = "app_prefs"
const val KEY_STARTED = "started"

/**
 * IntroActivity — Native onboarding launcher with automatic Android 13+
 * system notification permission request.
 */
class IntroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIntroBinding
    private lateinit var appUpdateManagerUtil: AppUpdateManagerUtil

    // 🔔 Android 13+ (API 33+) नेटिव सिस्टम नोटीफिकेशन परमिशन लॉन्चर
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            // अनुमति मिले या न मिले, सीधे TWA (ऐप) को फुलस्क्रीन में लॉन्च करें
            startMain()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { false }
        super.onCreate(savedInstanceState)

        binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)

        if (prefs.getBoolean(KEY_STARTED, false)) {
            // अगर यूज़र पहले ही ऑनबोर्डिंग कर चुका है, तो परमिशन चेक करके ऐप खोलें
            checkAndRequestNotificationPermission()
            return
        } else {
            binding.parent.visibility = View.VISIBLE
        }

        val manifest = readManifestData(this)
        binding.itemTitle.text = manifest.name
        binding.subtitle.text = manifest.description

        // 🎯 जब यूज़र "Get Started" दबाएगा, तो सिस्टम अपने आप "Allow Notifications?" पूछेगा
        binding.getStartedButton.setOnClickListener {
            prefs.edit { putBoolean(KEY_STARTED, true) }
            checkAndRequestNotificationPermission()
        }

        appUpdateManagerUtil = AppUpdateManagerUtil(this, binding, AppUpdateType.IMMEDIATE).apply {
            checkForUpdate()
        }
    }

    /**
     * Android 13+ पर सिस्टम डायलॉग खोलना ताकि फ़ोन की सेटिंग्स में हाथ न लगाना पड़े
     */
    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // नेटिव सिस्टम डायलॉग पॉप-अप करें
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        startMain()
    }

    private fun startMain() {
        val manifest = readManifestData(this)
        TwaLauncher(this).launch(manifest.startUrl.toUri())
    }
}
