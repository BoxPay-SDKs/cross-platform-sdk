package com.crossplatform.ios

import com.crossplatform.sdk.data.repo.UPIAppDetector
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

class UPIAppDetectorIOS : UPIAppDetector {
    private val upiSchemes = mapOf(
        "gpay" to "gpay://",
        "phonepe" to "phonepe://",
        "tez" to "tez://",
        "paytm" to "paytmmp://"
    )

    override fun getInstalledUPIApps(): List<String> {
        return upiSchemes.filter { (_, url) ->
            val nsUrl = NSURL.URLWithString(url)
            UIApplication.sharedApplication.canOpenURL(nsUrl!!)
        }.keys.toList()
    }
}
