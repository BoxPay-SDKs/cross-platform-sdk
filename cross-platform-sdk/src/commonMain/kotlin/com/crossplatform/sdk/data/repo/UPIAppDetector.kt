package com.crossplatform.sdk.data.repo

interface UPIAppDetector {
    fun getInstalledUPIApps(): List<String>
}