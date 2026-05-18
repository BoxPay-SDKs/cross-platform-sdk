package com.crossplatform.android

import android.content.Context
import android.content.pm.PackageManager
import com.crossplatform.sdk.data.repo.UPIAppDetector

class UPIAppDetectorAndroid(
    private val context: Context
) : UPIAppDetector {

    private val upiPackages: Map<String, String> = mapOf(
        "gpay" to "com.google.android.apps.nbu.paisa.user",
        "paytm" to "net.one97.paytm",
        "phonepe" to "com.phonepe.app"
    )

    override fun getInstalledUPIApps(): List<String> {
        val pm = context.packageManager
        return upiPackages.filter { (_, packageName) ->
            try {
                pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }.keys.toList()
    }
}
