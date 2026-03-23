package com.crossplatform.sdk.presentation

import com.crossplatform.sdk.data.handler.UserDataHandler
import com.crossplatform.sdk.data.model.BrowserData
import com.crossplatform.sdk.data.model.DeliveryAddress
import com.crossplatform.sdk.data.model.DeviceDetails
import com.crossplatform.sdk.data.model.Shopper

private const val TEST_API_URL = "https://test-apis.boxpay.tech/"
private const val PROD_API_URL = "https://apis.boxpay.in/"
private const val ROUTE = "/v0/checkout/sessions/"

fun getEndpoint(env: String) : String {
    val baseUrl = when (env) {
        "test" -> TEST_API_URL
        "prod" -> PROD_API_URL
        else -> PROD_API_URL
    }
    return "${baseUrl}${ROUTE}"
}

fun generateRandomAlphanumericString(length: Int): String {
    val charPool = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    return (1..length)
        .map { charPool.random() }
        .joinToString("")
}

fun getShopperDetails(): Shopper {
    val userData = UserDataHandler.userData

    val deliveryAddress = DeliveryAddress(
        address1 = userData.address1,
        address2 = userData.address2,
        city = userData.city,
        state = userData.state,
        countryCode = userData.countryCode,
        postalCode = userData.pincode,
        labelType = userData.labelType,
        labelName = userData.labelName
    )

    return Shopper(
        email = userData.email,
        firstName = userData.firstName,
        lastName = userData.lastName,
        phoneNumber = userData.completePhoneNumber,
        uniqueReference = userData.uniqueId,
        dateOfBirth = userData.dob,
        panNumber = userData.pan,
        deliveryAddress = if (isDeliveryAddressEmpty(deliveryAddress)) null else deliveryAddress
    )
}

private fun isDeliveryAddressEmpty(address: DeliveryAddress): Boolean {
    return listOf(
        address.address1,
        address.address2,
        address.city,
        address.state,
        address.countryCode,
        address.postalCode,
        address.labelType,
        address.labelName
    ).all { it.isNullOrEmpty() }
}

expect fun getBrowserData(): BrowserData
expect fun getDeviceDetails(): DeviceDetails
