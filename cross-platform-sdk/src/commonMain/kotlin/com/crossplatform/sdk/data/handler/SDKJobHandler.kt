package com.crossplatform.sdk.data.handler

import kotlinx.coroutines.Job

// commonMain
object SDKJobHandler {
    private val jobs = mutableListOf<Job>()

    fun register(job: Job) {
        jobs.add(job)
    }

    fun cancelAll() {
        jobs.forEach { it.cancel() }
        jobs.clear()
    }
}