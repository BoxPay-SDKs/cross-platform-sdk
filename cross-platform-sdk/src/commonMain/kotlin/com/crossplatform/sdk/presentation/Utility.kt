package com.crossplatform.sdk.presentation

import androidx.compose.ui.graphics.Color
import com.crossplatform.sdk.data.handler.UserDataHandler
import com.crossplatform.sdk.data.model.CheckoutDetails
import com.crossplatform.sdk.data.model.DeliveryAddress
import com.crossplatform.sdk.data.model.Shopper
import com.crossplatform.sdk.data.model.UserDetails
import com.crossplatform.sdk.domain.model.CountryDetailsModel
import com.crossplatform.sdk.domain.model.SurchargeModel
import com.crossplatform.sdk.domain.model.TransactionStatusEnum
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi

private const val TEST_API_URL = "https://test-apis.boxpay.tech/"
private const val PROD_API_URL = "https://apis.boxpay.in/"
private const val ROUTE = "v0/checkout/sessions/"

fun getEndpoint(isTestEnv: Boolean) : String {
    val baseUrl = if (isTestEnv) TEST_API_URL else PROD_API_URL
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

fun String.toComposeColor(): Color {
    val hex = this.trimStart('#')
    return when (hex.length) {
        6 -> Color(
            red   = hex.take(2).toInt(16) / 255f,
            green = hex.substring(2, 4).toInt(16) / 255f,
            blue  = hex.substring(4, 6).toInt(16) / 255f
        )
        8 -> Color(
            alpha = hex.take(2).toInt(16) / 255f,
            red   = hex.substring(2, 4).toInt(16) / 255f,
            green = hex.substring(4, 6).toInt(16) / 255f,
            blue  = hex.substring(6, 8).toInt(16) / 255f
        )
        else -> Color.Black
    }
}

@OptIn(ExperimentalResourceApi::class)
suspend fun loadCountryData(): Map<String, CountryDetailsModel> {
    val bytes = Res.readBytes("files/countryCodes.json")
    val jsonString = bytes.decodeToString()
    return Json.decodeFromString(jsonString)
}

fun parseIso8601ToMillis(timestamp: String): Long {
    // "2026-05-07T07:13:41.225323198Z"
    return try {
        val parts     = timestamp.removeSuffix("Z").split("T")
        val dateParts = parts[0].split("-")
        val timeParts = parts[1].split(".")[0].split(":")

        val year   = dateParts[0].toInt()
        val month  = dateParts[1].toInt()
        val day    = dateParts[2].toInt()
        val hour   = timeParts[0].toInt()
        val minute = timeParts[1].toInt()
        val second = timeParts[2].toInt()

        val y = if (month <= 2) year - 1 else year
        val m = if (month <= 2) month + 12 else month
        val a = y / 100
        val b = 2 - a + a / 4
        val jd = (365.25 * (y + 4716)).toLong() +
                (30.6001 * (m + 1)).toLong() +
                day + b - 1524

        val epochDays = jd - 2440588L
        ((epochDays * 86400) + (hour * 3600) + (minute * 60) + second) * 1000L
    } catch (_: Exception) {
        0L
    }
}

fun formatTimer(seconds: Long): String {
    val totalSeconds = seconds.coerceAtLeast(0)

    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60

    return when {
        hours > 0 ->
            "${hours.toTwoDigits()}:${minutes.toTwoDigits()}:${secs.toTwoDigits()}"

        else ->
            "${minutes.toTwoDigits()}:${secs.toTwoDigits()}"
    }
}

fun formatTransactionTimestamp(
    transactionTimestampLocale: String
): Pair<String, String>? {

    if (transactionTimestampLocale.isBlank()) return null

    try {
        val cleaned = transactionTimestampLocale
            .replace(" IST", "")
            .trim()

        val parts = cleaned.split(" ")
        if (parts.size < 2) return null

        val datePart = parts[0]
        val timePart = parts[1]

        // -------- DATE --------
        val dateComponents = datePart.split("/")
        if (dateComponents.size != 3) return null

        val day = dateComponents[0]
        val month = dateComponents[1]
        val year = dateComponents[2]

        val monthName = when (month) {
            "01" -> "Jan"
            "02" -> "Feb"
            "03" -> "Mar"
            "04" -> "Apr"
            "05" -> "May"
            "06" -> "Jun"
            "07" -> "Jul"
            "08" -> "Aug"
            "09" -> "Sep"
            "10" -> "Oct"
            "11" -> "Nov"
            "12" -> "Dec"
            else -> month
        }

        val formattedDate = "$monthName $day, $year"

        // -------- TIME --------
        val timeComponents = timePart.split(":")
        if (timeComponents.size < 2) return null

        var hour = timeComponents[0].toInt()
        val minute = timeComponents[1]

        val amPm = if (hour >= 12) "PM" else "AM"

        hour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }

        val hourStr = if (hour < 10) "0$hour" else hour.toString()

        val formattedTime = "$hourStr:$minute $amPm"

        return Pair(formattedDate, formattedTime)

    } catch (_: Exception) {
        return null
    }
}

private fun Long.toTwoDigits(): String {
    return if (this < 10) "0$this" else this.toString()
}

fun resolveErrorMessage(
    reasonCode: String?,
    reason: String?,
    fallback: String
): String {
    return if (reasonCode?.startsWith("UF") == true) {
        reason?.let {
            if (it.contains(':')) it.split(':').getOrNull(1)?.trim() ?: fallback
            else it
        } ?: fallback
    } else {
        fallback
    }
}

fun getStatus(status : String) : TransactionStatusEnum {
    return when (status.uppercase()) {
        in listOf("APPROVED", "SUCCESS", "PAID") -> TransactionStatusEnum.SUCCESS
        "EXPIRED"                                 -> TransactionStatusEnum.EXPIRED
        in listOf("FAILED", "REJECTED")           -> TransactionStatusEnum.FAILED
        "REQUIRESACTION"                          -> TransactionStatusEnum.REQUIRESACTION
        else                                      -> TransactionStatusEnum.NOACTION
    }
}

fun buildAddressAndUserDetailsString(checkoutDetails: CheckoutDetails, userDetails: UserDetails): String {
    val showName = checkoutDetails.isFullNameEnabled
    val showPhone = checkoutDetails.isPhoneEnabled
    val showEmail = checkoutDetails.isEmailEnabled
    val showShipping = checkoutDetails.isShippingAddressEnabled

    val firstName = userDetails.firstName.orEmpty()
    val lastName = userDetails.lastName.orEmpty()
    val phone = userDetails.completePhoneNumber.orEmpty()
    val email = userDetails.email.orEmpty()
    val address1 = userDetails.address1.orEmpty()
    val address2 = userDetails.address2
    val city = userDetails.city.orEmpty()
    val state = userDetails.state.orEmpty()
    val postalCode = userDetails.pincode.orEmpty()

    return buildString {
        // Name / phone line
        when {
            (showPhone && showName) || showShipping -> append("$firstName $lastName ($phone)")
            showName -> append("$firstName $lastName")
            showPhone -> append("($phone)")
        }

        // Email line
        if (showEmail || showShipping) {
            append("\n$email")
        }

        // Address line
        if (showShipping) {
            append("\n")
            append(
                if (!address2.isNullOrEmpty())
                    "$address1, $address2, $city, $state, $postalCode"
                else
                    "$address1, $city, $state, $postalCode"
            )
        }
    }.trim()
}

fun buildAddressString(checkoutDetails: CheckoutDetails, userDetails: UserDetails): String {
    if (!checkoutDetails.isShippingAddressEnabled) return ""

    val address1 = userDetails.address1.orEmpty()
    val address2 = userDetails.address2
    val city = userDetails.city.orEmpty()
    val state = userDetails.state.orEmpty()
    val postalCode = userDetails.pincode.orEmpty()

    return if (!address2.isNullOrEmpty())
        "$address1, $address2, $city, $state, $postalCode"
    else
        "$address1, $city, $state, $postalCode"
}

fun isPresentInSurchargeModel(surchargeModel: List<SurchargeModel>, selectedMethod : String) : Boolean {
    return surchargeModel.any { it.applicableOn.equals(selectedMethod, ignoreCase = true) }
}
