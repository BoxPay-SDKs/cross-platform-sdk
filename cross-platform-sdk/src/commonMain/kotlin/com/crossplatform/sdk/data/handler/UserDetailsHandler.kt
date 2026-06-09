package com.crossplatform.sdk.data.handler

import com.crossplatform.sdk.data.model.UserDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

object UserDataHandler {


    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

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
    var userData: UserDetails = defaultUserData()
        private set

    val firstNameFlow: StateFlow<String?> = _userDataFlow
        .map { it.firstName }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, defaultUserData().firstName)

    val lastNameFlow: StateFlow<String?> = _userDataFlow
        .map { it.lastName }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, defaultUserData().lastName)

    val emailFlow: StateFlow<String?> = _userDataFlow
        .map { it.email }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, defaultUserData().email)

    val completePhoneNumberFlow: StateFlow<String?> = _userDataFlow
        .map { it.completePhoneNumber }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, defaultUserData().completePhoneNumber)

    val phoneCodeFlow: StateFlow<String> = _userDataFlow
        .map { it.phoneCode }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, defaultUserData().phoneCode)

    val uniqueIdFlow: StateFlow<String> = _userDataFlow
        .map { it.uniqueId }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, defaultUserData().uniqueId)

    val labelFlow: StateFlow<Pair<String?, String?>> = _userDataFlow
        .map { it.labelType to it.labelName }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, Pair(defaultUserData().labelType, defaultUserData().labelName))

    val addressFlow: StateFlow<AddressConfig> = _userDataFlow
        .map {
            AddressConfig(
                address1 = it.address1,
                address2 = it.address2,
                city = it.city,
                state = it.state,
                countryCode = it.countryCode,
                countryName = it.countryName,
                pincode = it.pincode
            )
        }
        .distinctUntilChanged()
        .stateIn(
            scope,
            SharingStarted.Eagerly,
            AddressConfig(
                defaultUserData().address1,
                defaultUserData().address2,
                defaultUserData().city,
                defaultUserData().state,
                defaultUserData().countryCode,
                defaultUserData().countryName,
                defaultUserData().pincode
            )
        )

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

data class AddressConfig(
    val address1: String?,
    val address2: String?,
    val city: String?,
    val state: String?,
    val countryCode: String?,
    val countryName: String?,
    val pincode: String?
)