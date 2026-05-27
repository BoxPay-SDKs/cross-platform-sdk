package com.crossplatform.sdk.data.handler

import com.crossplatform.sdk.data.model.UserDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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
    private val _userDataFlow = MutableStateFlow(defaultUserData())
    val userDataFlow: StateFlow<UserDetails> = _userDataFlow
    var userData: UserDetails = defaultUserData()

    // ─── setUserDataHandler ────────────────────────────────────
    fun set(
        firstName : String?,
        lastName : String?,
        email : String?,
        dob : String?,
        pan : String?,
        address1 : String?,
        address2 : String?,
        city : String?,
        state : String?,
        pincode : String?,
        labelType : String?,
        labelName : String?,
        uniqueId : String
    ) {
        userData = userData.copy(
            firstName = firstName,
            lastName = lastName,
            email = email,
            dob = dob,
            pan = pan,
            address1 = address1,
            address2 = address2,
            city = city,
            state = state,
            pincode = pincode,
            labelType = labelType,
            labelName = labelName,
            uniqueId = uniqueId
        )
        _userDataFlow.value = userData
    }

    fun  setUserPhoneAndCountryData(
        phoneCode : String?,
        completePhoneNumber : String?,
        countryCode : String?,
        countryName : String?
    ) {
        userData = userData.copy(
            phoneCode = phoneCode ?: "+91",
            completePhoneNumber = completePhoneNumber,
            countryCode = countryCode ?: "IN",
            countryName = countryName ?: "India"
        )
        _userDataFlow.value = userData
    }

    // ─── setUserDataHandlerToDefault ───────────────────────────
    fun resetToDefault() {
        userData = defaultUserData()
        _userDataFlow.value = userData
    }

    fun setUniqueRef(uniqueId: String){
        userData = userData.copy(
            uniqueId = uniqueId
        )
        _userDataFlow.value = userData
    }
}