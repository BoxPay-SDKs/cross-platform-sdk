package com.crossplatform.sdk.data

import kotlinx.coroutines.Job


open class BaseEndpoint {

    private var job: Job? = null  // null = not initialized (same as lateinit check)

    // Call this when starting a request, pass the job
    protected fun setJob(newJob: Job) {
        job = newJob
    }

    open fun cancel() {
        if (job != null && job?.isActive == true) {  // same as isInitialized check
            job?.cancel()
        }
    }
}