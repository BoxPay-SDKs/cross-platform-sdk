package com.crossplatform.sdk.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.crossplatform.sdk.domain.model.CountryDetailsModel
import com.crossplatform.sdk.presentation.loadCountryData
import com.crossplatform.sdk.presentation.theme.defaultFontFamily

// commonMain
@Composable
fun CountryPickerDialog(
    onDismiss : () -> Unit,
    onSelect  : (code: String, isdCode: String, fullName: String, phoneLengths: List<Int>) -> Unit
) {
    val countryData = remember { mutableStateOf<Map<String, CountryDetailsModel>>(emptyMap()) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        countryData.value = loadCountryData()
    }

    val filteredCountries = remember(searchQuery, countryData.value) {
        countryData.value.entries
            .filter {
                it.value.fullName.contains(searchQuery, ignoreCase = true) ||
                        it.value.isdCode.contains(searchQuery)
            }
            .sortedBy { it.value.fullName }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
        ) {

            // --- Header ---
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text       = "Select Country",
                    fontSize   = 18.sp,
                    fontFamily = defaultFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.weight(1f)
                )
                Image(
                    imageVector        = Icons.Default.Close,
                    contentDescription = "Close",
                    modifier           = Modifier
                        .size(24.dp)
                        .clickable { onDismiss() }
                )
            }

            HorizontalDivider(color = Color(0xFFECECED))

            // --- Search ---
            OutlinedTextField(
                value         = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder   = {
                    Text(
                        text       = "Search country",
                        fontFamily = defaultFontFamily,
                        fontWeight = FontWeight.Normal,
                        color      = Color(0xFFADACAD)
                    )
                },
                leadingIcon = {
                    Image(
                        imageVector        = Icons.Default.Search,
                        contentDescription = null,
                        colorFilter    = ColorFilter.tint(Color(0xFFADACAD))
                    )
                },
                shape    = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                textStyle = TextStyle(
                    fontFamily = defaultFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp
                )
            )

            // --- Country List ---
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(filteredCountries) { (code, details) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(
                                    code,
                                    details.isdCode,
                                    details.fullName,
                                    details.phoneNumberLength
                                )
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text       = details.fullName,
                            fontFamily = defaultFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize   = 14.sp,
                            modifier   = Modifier.weight(1f)
                        )
                        Text(
                            text       = details.isdCode,
                            fontFamily = defaultFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize   = 14.sp,
                            color      = Color(0xFF888780)
                        )
                    }
                    HorizontalDivider(
                        color    = Color(0xFFECECED),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}