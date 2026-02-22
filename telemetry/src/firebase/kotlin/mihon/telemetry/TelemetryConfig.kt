package mihon.telemetry

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

object TelemetryConfig {
    private var analytics: FirebaseAnalytics? = null
    private var crashlytics: FirebaseCrashlytics? = null

    fun init(context: Context) {
        // To stop forks/test builds from polluting our data
        if (!context.isTsundokuProductionApp()) return

        analytics = FirebaseAnalytics.getInstance(context)
        FirebaseApp.initializeApp(context)
        crashlytics = FirebaseCrashlytics.getInstance()
    }

    fun setAnalyticsEnabled(enabled: Boolean) {
        analytics?.setAnalyticsCollectionEnabled(enabled)
    }

    fun setCrashlyticsEnabled(enabled: Boolean) {
        crashlytics?.isCrashlyticsCollectionEnabled = enabled
    }

    private fun Context.isTsundokuProductionApp(): Boolean {
        if (packageName !in TSUNDOKU_PACKAGES) return false

        return packageManager.getPackageInfo(packageName, SignatureFlags)
            .getCertificateFingerprints()
            .any { it == TSUNDOKU_CERTIFICATE_FINGERPRINT }
    }
}

private val TSUNDOKU_PACKAGES = hashSetOf("app.tsundoku", "app.tsundoku.debug")
private const val TSUNDOKU_CERTIFICATE_FINGERPRINT =
    "CA:56:7A:E5:AA:C0:B4:9C:01:41:BF:CE:59:97:07:1E:1B:66:C3:17:54:B5:81:99:F0:A9:9B:45:42:8A:48:F1"
