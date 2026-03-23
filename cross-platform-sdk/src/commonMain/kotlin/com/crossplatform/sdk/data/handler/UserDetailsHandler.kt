package com.crossplatform.sdk.data.handler

import com.crossplatform.sdk.data.model.UserDetails

object UserDataHandler {

    // ─── Default Values ────────────────────────────────────────
    private fun defaultUserData() = UserDetails(
        firstName = null,
        lastName = null,
        email = null,
        completePhoneNumber = null,
        phoneCode = "+91",
        dob = null,
        pan = null,
        address1 = null,
        address2 = null,
        city = null,
        state = null,
        countryCode = "IN",
        countryName = "India",
        pincode = null,
        labelType = null,
        labelName = null,
        uniqueId = ""
    )

    // ─── State ─────────────────────────────────────────────────
    var userData: UserDetails = defaultUserData()

    // ─── setUserDataHandler ────────────────────────────────────
    fun set(data: UserDetails) {
        userData = data
    }

    // ─── setUserDataHandlerToDefault ───────────────────────────
    fun resetToDefault() {
        userData = defaultUserData()
    }
}