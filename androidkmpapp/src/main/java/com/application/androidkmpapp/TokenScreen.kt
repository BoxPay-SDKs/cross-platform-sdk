package com.application.androidkmpapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.crossplatform.sdk.presentation.theme.defaultFontFamily
import com.crossplatform.sdk.presentation.toComposeColor

data class BoxPayConfig(
    val token: String,
    val shopperToken: String?,
    val isTestEnv: Boolean,
    val isSuccessScreenVisible: Boolean,
    val isFailedScreenVisible: Boolean,
    val showQROnLoad: Boolean,
    val ctaBorderRadius: Int,
    val isSICheckBoxChecked: Boolean,
    val isSICheckBoxEnabled: Boolean,
    val focusedTextInputBorderColor: String,
    val unfocusedTextInputBorderColor: String
)

@Composable
fun TokenScreen(onProceed: (BoxPayConfig) -> Unit) {

    val token                       = remember { mutableStateOf("") }
    val shopperToken                = remember { mutableStateOf("") }
    val isTestEnv                   = remember { mutableStateOf(true) }
    val isSuccessScreenVisible      = remember { mutableStateOf(false) }
    val isFailedScreenVisible       = remember { mutableStateOf(false) }
    val showQROnLoad                = remember { mutableStateOf(false) }
    val ctaBorderRadius             = remember { mutableIntStateOf(12) }
    val isSICheckBoxChecked         = remember { mutableStateOf(false) }
    val isSICheckBoxEnabled         = remember { mutableStateOf(false) }
    val focusedTextInputBorderColor = remember { mutableStateOf("#2D2B32") }
    val unfocusedTextInputBorderColor = remember { mutableStateOf("#ADACB0") }
    val focusedDropdownExpanded   = remember { mutableStateOf(false) }
    val unfocusedDropdownExpanded = remember { mutableStateOf(false) }

    val presetColors = listOf(
        "Custom"    to null,
        "Charcoal"  to "#2D2B32",
        "Grey"      to "#ADACB0",
        "Black"     to "#000000",
        "Blue"      to "#1A73E8",
        "Green"     to "#1CA672",
        "Red"       to "#E53935",
        "Purple"    to "#7B1FA2",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ── Token ──────────────────────────────────────────────
        TokenField(label = "Token *", value = token.value) { token.value = it }

        // ── Shopper Token ──────────────────────────────────────
        TokenField(label = "Shopper Token (optional)", value = shopperToken.value) { shopperToken.value = it }

        // ── Color fields ───────────────────────────────────────
        ColorPickerField(
            label                = "Focused Border Color",
            selectedColor        = focusedTextInputBorderColor.value,
            onColorChange        = { focusedTextInputBorderColor.value = it },
            presetColors         = presetColors,
            dropdownExpanded     = focusedDropdownExpanded.value,
            onDropdownExpandChange = { focusedDropdownExpanded.value = it }
        )

        // ── Unfocused Border Color ─────────────────────────────────
        ColorPickerField(
            label                = "Unfocused Border Color",
            selectedColor        = unfocusedTextInputBorderColor.value,
            onColorChange        = { unfocusedTextInputBorderColor.value = it },
            presetColors         = presetColors,
            dropdownExpanded     = unfocusedDropdownExpanded.value,
            onDropdownExpandChange = { unfocusedDropdownExpanded.value = it }
        )

        // ── CTA Border Radius slider ───────────────────────────
        OutlinedTextField(
            value = ctaBorderRadius.intValue.toString(),
            onValueChange = { ctaBorderRadius.intValue = it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 },
            label = {
                Text(text = "CTA Border Radius", fontFamily = defaultFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp)
            },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(62.dp),
            textStyle = TextStyle(
                fontFamily = defaultFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                color = Color(0xFF0A090B)
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            maxLines = 1
        )

        // ── Toggle switches ────────────────────────────────────
        ToggleRow(label = "Test Environment",        checked = isTestEnv.value)              { isTestEnv.value = it }
        ToggleRow(label = "Show Success Screen",     checked = isSuccessScreenVisible.value) { isSuccessScreenVisible.value = it }
        ToggleRow(label = "Show Failed Screen",      checked = isFailedScreenVisible.value)  { isFailedScreenVisible.value = it }
        ToggleRow(label = "Show QR On Load",         checked = showQROnLoad.value)           { showQROnLoad.value = it }
        ToggleRow(label = "SI CheckBox Enabled",     checked = isSICheckBoxEnabled.value)    { isSICheckBoxEnabled.value = it }
        ToggleRow(label = "SI CheckBox Pre-Checked", checked = isSICheckBoxChecked.value)    { isSICheckBoxChecked.value = it }

        // ── Proceed button ─────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (token.value.isNotEmpty()) Color(0xFF1CA672) else Color(0xFFE6E6E6))
                .clickable(enabled = token.value.isNotEmpty()) {
                    onProceed(
                        BoxPayConfig(
                            token                       = token.value,
                            shopperToken                = shopperToken.value.ifBlank { null },
                            isTestEnv                   = isTestEnv.value,
                            isSuccessScreenVisible      = isSuccessScreenVisible.value,
                            isFailedScreenVisible       = isFailedScreenVisible.value,
                            showQROnLoad                = showQROnLoad.value,
                            ctaBorderRadius             = ctaBorderRadius.intValue,
                            isSICheckBoxChecked         = isSICheckBoxChecked.value,
                            isSICheckBoxEnabled         = isSICheckBoxEnabled.value,
                            focusedTextInputBorderColor   = focusedTextInputBorderColor.value,
                            unfocusedTextInputBorderColor = unfocusedTextInputBorderColor.value
                        )
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = "Proceed",
                fontSize   = 16.sp,
                fontFamily = defaultFontFamily,
                fontWeight = FontWeight.SemiBold,
                color      = if (token.value.isNotEmpty()) Color.White else Color(0xFFADACAD),
                modifier   = Modifier.padding(vertical = 14.dp)
            )
        }
    }
}

// ── Reusable helpers ───────────────────────────────────────────────────────────

@Composable
private fun TokenField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value         = value,
        onValueChange = onChange,
        label = {
            Text(text = label, fontFamily = defaultFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp)
        },
        shape     = RoundedCornerShape(8.dp),
        modifier  = Modifier.fillMaxWidth().height(62.dp),
        textStyle = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize   = 16.sp,
            color      = Color(0xFF0A090B)
        ),
        maxLines = 1
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontFamily = defaultFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorPickerField(
    label: String,
    selectedColor: String,
    onColorChange: (String) -> Unit,
    presetColors: List<Pair<String, String?>>,
    dropdownExpanded: Boolean,
    onDropdownExpandChange: (Boolean) -> Unit
) {
    val isCustom = presetColors.none { it.second == selectedColor }
    val parsedColor = remember(selectedColor) {
        try { Color(selectedColor.toColorInt()) }
        catch (e: Exception) { Color.Transparent }
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {

        Text(text = label, fontFamily = defaultFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Medium)

        // ── Dropdown ───────────────────────────────────────────
        ExposedDropdownMenuBox(
            expanded = dropdownExpanded,
            onExpandedChange = onDropdownExpandChange
        ) {
            OutlinedTextField(
                value = presetColors.find { it.second == selectedColor }?.first ?: "Custom",
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(parsedColor)
                                .border(1.dp, Color.LightGray, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                    }
                },
                shape    = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                textStyle = TextStyle(fontFamily = defaultFontFamily, fontSize = 16.sp, color = Color(0xFF0A090B))
            )

            ExposedDropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { onDropdownExpandChange(false) }
            ) {
                presetColors.forEach { (name, hex) ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (hex != null) {
                                                try { Color(android.graphics.Color.parseColor(hex)) }
                                                catch (e: Exception) { Color.Transparent }
                                            } else Color.Transparent
                                        )
                                        .border(1.dp, Color.LightGray, CircleShape)
                                )
                                Text(text = name, fontFamily = defaultFontFamily, fontSize = 14.sp)
                                if (hex != null) {
                                    Text(text = hex, fontFamily = defaultFontFamily, fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        },
                        onClick = {
                            onColorChange(hex ?: selectedColor)
                            onDropdownExpandChange(false)
                        }
                    )
                }
            }
        }

        // ── Custom hex input (shown when Custom is selected or no preset matches) ──
        if (isCustom || presetColors.find { it.second == selectedColor }?.first == "Custom") {
            OutlinedTextField(
                value = selectedColor,
                onValueChange = { new ->
                    val clean = if (new.startsWith("#")) new else "#$new"
                    onColorChange(clean)
                },
                label = { Text("Hex value", fontFamily = defaultFontFamily, fontSize = 14.sp) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(parsedColor)
                            .border(1.dp, Color.LightGray, CircleShape)
                    )
                },
                shape     = RoundedCornerShape(8.dp),
                modifier  = Modifier.fillMaxWidth().height(62.dp),
                textStyle = TextStyle(fontFamily = defaultFontFamily, fontSize = 16.sp, color = Color(0xFF0A090B)),
                maxLines  = 1
            )
        }
    }
}

