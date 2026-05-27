package com.crossplatform.sdk.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.handler.UserDataHandler
import com.crossplatform.sdk.presentation.ChevronIcon
import com.crossplatform.sdk.presentation.ErrorText
import com.crossplatform.sdk.presentation.components.CountryPickerDialog
import com.crossplatform.sdk.presentation.components.Footer
import com.crossplatform.sdk.presentation.components.PayButton
import com.crossplatform.sdk.presentation.theme.defaultFontFamily
import com.crossplatform.sdk.presentation.toComposeColor

@Composable
fun AddressScreen(
    onAddressSaved: () -> Unit
) {
    val checkoutDetails by CheckoutDetailsHandler.checkoutDetailsFlow.collectAsStateWithLifecycle()
    val userData = UserDataHandler.userData

    val isShippingEnabled    = checkoutDetails.isShippingAddressEnabled
    val isFullNameEnabled    = checkoutDetails.isFullNameEnabled
    val isPhoneEnabled       = checkoutDetails.isPhoneEnabled
    val isEmailEnabled       = checkoutDetails.isEmailEnabled

    // --- Field States ---
    var countryTextField             by remember { mutableStateOf("") }
    var fullNameTextField            by remember { mutableStateOf("") }
    var phoneNumberTextField         by remember { mutableStateOf("") }
    var selectedPhoneCode            by remember { mutableStateOf("+91") }
    var emailTextField               by remember { mutableStateOf("") }
    var pinTextField                 by remember { mutableStateOf("") }
    var cityTextField                by remember { mutableStateOf("") }
    var stateTextField               by remember { mutableStateOf("") }
    var mainAddressTextField         by remember { mutableStateOf("") }
    var secondaryAddressTextField    by remember { mutableStateOf("") }
    var showCountryPicker            by remember { mutableStateOf(false) }
    var selectedCountryCode          by remember { mutableStateOf("IN") }

    // --- Validation States ---
    var fullNameError      by remember { mutableStateOf("") }
    var phoneError         by remember { mutableStateOf("") }
    var emailError         by remember { mutableStateOf("") }
    var pinError           by remember { mutableStateOf("") }
    var cityError          by remember { mutableStateOf("") }
    var stateError         by remember { mutableStateOf("") }
    var mainAddressError   by remember { mutableStateOf("") }

    var isFullNameValid    by remember { mutableStateOf<Boolean?>(null) }
    var isPhoneValid       by remember { mutableStateOf<Boolean?>(null) }
    var isEmailValid       by remember { mutableStateOf<Boolean?>(null) }
    var isPinValid         by remember { mutableStateOf<Boolean?>(null) }
    var isCityValid        by remember { mutableStateOf<Boolean?>(null) }
    var isStateValid       by remember { mutableStateOf<Boolean?>(null) }
    var isMainAddressValid by remember { mutableStateOf<Boolean?>(null) }

    val phoneNumberLengthList = remember { mutableStateOf(listOf(10)) }
    val emailRegex = remember { Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") }

    // --- Pre-fill fields ---
    LaunchedEffect(Unit) {
        val firstName = userData.firstName ?: ""
        val lastName  = userData.lastName ?: ""
        fullNameTextField         = "$firstName $lastName".trim()
        emailTextField            = userData.email ?: ""
        mainAddressTextField      = userData.address1 ?: ""
        secondaryAddressTextField = userData.address2 ?: ""
        cityTextField             = userData.city ?: ""
        stateTextField            = userData.state ?: ""
        pinTextField              = userData.pincode ?: ""
        selectedCountryCode       = userData.countryCode ?: "IN"
        selectedPhoneCode = userData.phoneCode
        countryTextField  = userData.countryName ?: "India"

        val rawNumber = userData.completePhoneNumber
            ?.removePrefix(selectedPhoneCode) ?: ""
        phoneNumberTextField = rawNumber
    }

    // --- Validation Functions ---
    fun validateFullName(text: String) {
        if (text.trim().isEmpty()) {
            fullNameError = "Required"; isFullNameValid = false
        } else {
            fullNameError = ""; isFullNameValid = true
        }
    }

    fun validatePhone(text: String) {
        when {
            text.isEmpty() -> { phoneError = "Required"; isPhoneValid = false }
            !phoneNumberLengthList.value.contains(text.length) -> {
                phoneError = "Mobile number must be ${phoneNumberLengthList.value} digits"
                isPhoneValid = false
            }
            else -> { phoneError = ""; isPhoneValid = true }
        }
    }

    fun validateEmail(text: String) {
        when {
            text.trim().isEmpty() -> { emailError = "Required"; isEmailValid = false }
            !emailRegex.matches(text.trim()) -> { emailError = "Invalid Email"; isEmailValid = false }
            else -> { emailError = ""; isEmailValid = true }
        }
    }

    fun validatePin(text: String) {
        when {
            text.trim().isEmpty() -> { pinError = "Required"; isPinValid = false }
            selectedPhoneCode == "+91" && text.length < 6 -> {
                pinError = "Zip/Postal code must be 6 digits"; isPinValid = false
            }
            else -> { pinError = ""; isPinValid = true }
        }
    }

    fun validateCity(text: String) {
        if (text.trim().isEmpty()) { cityError = "Required"; isCityValid = false }
        else { cityError = ""; isCityValid = true }
    }

    fun validateState(text: String) {
        if (text.trim().isEmpty()) { stateError = "Required"; isStateValid = false }
        else { stateError = ""; isStateValid = true }
    }

    fun validateMainAddress(text: String) {
        if (text.trim().isEmpty()) { mainAddressError = "Required"; isMainAddressValid = false }
        else { mainAddressError = ""; isMainAddressValid = true }
    }

    fun isAllValid(): Boolean {
        var valid = true
        if (isFullNameEnabled || isShippingEnabled) {
            validateFullName(fullNameTextField)
            if (isFullNameValid == false) valid = false
        }
        if (isPhoneEnabled || isShippingEnabled) {
            validatePhone(phoneNumberTextField)
            if (isPhoneValid == false) valid = false
        }
        if (isEmailEnabled || isShippingEnabled) {
            validateEmail(emailTextField)
            if (isEmailValid == false) valid = false
        }
        if (isShippingEnabled) {
            validatePin(pinTextField)
            validateCity(cityTextField)
            validateState(stateTextField)
            validateMainAddress(mainAddressTextField)
            if (isPinValid == false || isCityValid == false ||
                isStateValid == false || isMainAddressValid == false) valid = false
        }
        return valid
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        if (isShippingEnabled) {
            AddressTextField(
                value       = countryTextField,
                label       = "Country*",
                onValueChange = {},
                readOnly    = true,
                trailingIcon = { ChevronIcon() },
                modifier    = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 28.dp),
                onClick     = { showCountryPicker = true },
                focusedBorderColor = checkoutDetails.focusedTextInputBorderColor,
                unfocusedBorderColor = checkoutDetails.unfocusedTextInputBorderColor
            )
        }

        // --- Full Name ---
        if (isShippingEnabled || isFullNameEnabled) {
            AddressTextField(
                value         = fullNameTextField,
                label         = "Full Name*",
                onValueChange = { fullNameTextField = it; validateFullName(it) },
                isError       = isFullNameValid == false,
                modifier      = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp),
                focusedBorderColor = checkoutDetails.focusedTextInputBorderColor,
                unfocusedBorderColor = checkoutDetails.unfocusedTextInputBorderColor
            )
            if (isFullNameValid == false) ErrorText(fullNameError)
        }

        // --- Phone ---
        if (isShippingEnabled || isPhoneEnabled) {
            Row(
                modifier          = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AddressTextField(
                    value         = selectedPhoneCode,
                    label         = "Code*",
                    onValueChange = {},
                    readOnly      = true,
                    trailingIcon  = { ChevronIcon() },
                    modifier      = Modifier.width(130.dp),
                    onClick       = { showCountryPicker = true },
                    focusedBorderColor = checkoutDetails.focusedTextInputBorderColor,
                    unfocusedBorderColor = checkoutDetails.unfocusedTextInputBorderColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                AddressTextField(
                    value         = phoneNumberTextField,
                    label         = "Mobile Number*",
                    onValueChange = { phoneNumberTextField = it; validatePhone(it) },
                    isError       = isPhoneValid == false,
                    keyboardType  = KeyboardType.Number,
                    modifier      = Modifier.weight(1f),
                    focusedBorderColor = checkoutDetails.focusedTextInputBorderColor,
                    unfocusedBorderColor = checkoutDetails.unfocusedTextInputBorderColor
                )
            }
            if (isPhoneValid == false) ErrorText(phoneError)
        }

        // --- Email ---
        if (isShippingEnabled || isEmailEnabled) {
            AddressTextField(
                value         = emailTextField,
                label         = "Email ID*",
                onValueChange = { emailTextField = it; validateEmail(it) },
                isError       = isEmailValid == false,
                keyboardType  = KeyboardType.Email,
                modifier      = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp),
                focusedBorderColor = checkoutDetails.focusedTextInputBorderColor,
                unfocusedBorderColor = checkoutDetails.unfocusedTextInputBorderColor
            )
            if (isEmailValid == false) ErrorText(emailError)
        }

        // --- Shipping Only Fields ---
        if (isShippingEnabled) {

            // PIN + City
            Row(
                modifier          = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    AddressTextField(
                        value         = pinTextField,
                        label         = "ZIP/Postal code*",
                        onValueChange = { pinTextField = it; validatePin(it) },
                        isError       = isPinValid == false,
                        keyboardType  = KeyboardType.Number,
                        modifier      = Modifier.fillMaxWidth(),
                        focusedBorderColor = checkoutDetails.focusedTextInputBorderColor,
                        unfocusedBorderColor = checkoutDetails.unfocusedTextInputBorderColor
                    )
                    if (isPinValid == false) ErrorText(pinError)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    AddressTextField(
                        value         = cityTextField,
                        label         = "City*",
                        onValueChange = { cityTextField = it; validateCity(it) },
                        isError       = isCityValid == false,
                        modifier      = Modifier.fillMaxWidth(),
                        focusedBorderColor = checkoutDetails.focusedTextInputBorderColor,
                        unfocusedBorderColor = checkoutDetails.unfocusedTextInputBorderColor
                    )
                    if (isCityValid == false) ErrorText(cityError)
                }
            }

            // State
            AddressTextField(
                value         = stateTextField,
                label         = "State*",
                onValueChange = { stateTextField = it; validateState(it) },
                isError       = isStateValid == false,
                modifier      = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp),
                focusedBorderColor = checkoutDetails.focusedTextInputBorderColor,
                unfocusedBorderColor = checkoutDetails.unfocusedTextInputBorderColor
            )
            if (isStateValid == false) ErrorText(stateError)

            // Main Address
            AddressTextField(
                value         = mainAddressTextField,
                label         = "House number, Apartment*",
                onValueChange = { mainAddressTextField = it; validateMainAddress(it) },
                isError       = isMainAddressValid == false,
                modifier      = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp),
                focusedBorderColor = checkoutDetails.focusedTextInputBorderColor,
                unfocusedBorderColor = checkoutDetails.unfocusedTextInputBorderColor
            )
            if (isMainAddressValid == false) ErrorText(mainAddressError)

            // Secondary Address
            AddressTextField(
                value         = secondaryAddressTextField,
                label         = "Area, Colony, Street, Sector",
                onValueChange = { secondaryAddressTextField = it },
                modifier      = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp),
                focusedBorderColor = checkoutDetails.focusedTextInputBorderColor,
                unfocusedBorderColor = checkoutDetails.unfocusedTextInputBorderColor
            )
        }

        Spacer(Modifier.weight(1f))

        // --- Bottom Button ---
        PayButton(
            text = if (isShippingEnabled) "Save Address" else "Save Personal Details",
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(checkoutDetails.ctaBorderRadius.dp))
                .background(checkoutDetails.buttonColor.toComposeColor())
                .clickable {
                    val (firstName, lastName) = extractNames(fullNameTextField)
                    UserDataHandler.set(
                        firstName           = firstName,
                        lastName            = lastName,
                        email               = emailTextField,
                        address1            = mainAddressTextField,
                        address2            = secondaryAddressTextField,
                        city                = cityTextField,
                        state               = stateTextField,
                        pincode             = pinTextField,
                        labelType           =  "",
                        labelName           = "",
                        uniqueId            = userData.uniqueId,
                        dob                 = null,
                        pan                 = null
                    )
                    UserDataHandler.setUserPhoneAndCountryData(
                        completePhoneNumber = "$selectedPhoneCode$phoneNumberTextField",
                        phoneCode           = selectedPhoneCode,
                        countryCode         = selectedCountryCode,
                        countryName         = countryTextField
                    )
                    onAddressSaved()
                },
            amount = 0.0,
            currencySymbol = "",
            isValid = isAllValid(),
            buttonTextColor = checkoutDetails.buttonTextColor
        )
        Footer()
    }
    if (showCountryPicker) {
        CountryPickerDialog(
            onDismiss = { showCountryPicker = false },
            focusedBorderColor = checkoutDetails.focusedTextInputBorderColor,
            unfocusedBorderColor = checkoutDetails.unfocusedTextInputBorderColor,
            onSelect  = { code, isdCode, fullName, phoneLengths ->
                selectedCountryCode            = code
                selectedPhoneCode              = isdCode
                countryTextField               = fullName
                phoneNumberLengthList.value    = phoneLengths
                showCountryPicker              = false
            }
        )
    }
}

// --- Reusable TextField ---
@Composable
fun AddressTextField(
    value        : String,
    label        : String,
    onValueChange: (String) -> Unit,
    modifier     : Modifier = Modifier,
    isError      : Boolean = false,
    readOnly     : Boolean = false,
    keyboardType : KeyboardType = KeyboardType.Text,
    trailingIcon : @Composable (() -> Unit)? = null,
    onClick      : (() -> Unit)? = null,
    focusedBorderColor : String,
    unfocusedBorderColor : String
) {
    Box(modifier = modifier) {
        OutlinedTextField(
            value           = value,
            onValueChange   = onValueChange,
            label           = {
                Text(
                    text       = label,
                    fontFamily = defaultFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize   = 16.sp
                )
            },
            isError         = isError,
            readOnly        = readOnly,
            trailingIcon    = trailingIcon,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape           = RoundedCornerShape(8.dp),
            modifier        = Modifier
                .fillMaxWidth()
                .height(62.dp),
            textStyle       = TextStyle(
                fontFamily = defaultFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize   = 16.sp,
                color      = Color(0xFF0A090B)
            ),
            maxLines = 1,
            colors = OutlinedTextFieldDefaults.colors(
                // Border
                focusedBorderColor   = focusedBorderColor.toComposeColor(),
                unfocusedBorderColor = unfocusedBorderColor.toComposeColor(),
            )
        )

        // ← transparent overlay captures clicks ✅
        if (onClick != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()          // same size as TextField
                    .clickable { onClick() }    // intercepts all clicks
            )
        }
    }
}

// --- Extract Names ---
fun extractNames(fullName: String): Pair<String, String> {
    val parts = fullName.trim().split(" ", limit = 2)
    return Pair(parts.getOrElse(0) { "" }, parts.getOrElse(1) { "" })
}