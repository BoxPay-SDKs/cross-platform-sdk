package com.crossplatform.sdk.presentation

import com.crossplatform.sdk.data.repo.UPIAppDetector

class UPIService(private val detector: UPIAppDetector) {
    fun getAvailableApps(): List<String> = detector.getInstalledUPIApps()
}