package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EmeraldGreen
import com.example.util.FormatUtils
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.example.data.model.UnitItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateLiquidationSheet(
    currencySymbol: String,
    units: List<UnitItem> = emptyList(),
    defaultTargetPercentage: Double = 1.0,
    defaultAmount: Double = 1.0,
    onDismiss: () -> Unit,
    onSubmit: (
        assetSymbol: String,
        buyPrice: Double,
        amount: Double,
        targetPercentage: Double,
        notes: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val defaultUnitItem = remember(units) { units.find { it.isDefault } ?: units.firstOrNull() }

    var assetSymbol by remember { mutableStateOf(defaultUnitItem?.name ?: "") }
    var buyPriceText by remember {
        mutableStateOf(
            defaultUnitItem?.let { u ->
                if (u.currentPrice > 0.0) FormatUtils.formatPlain(u.currentPrice) else ""
            } ?: ""
        )
    }
    var amountText by remember {
        mutableStateOf(if (defaultAmount > 0.0) FormatUtils.formatAmount(defaultAmount) else "")
    }
    var targetPercentageText by remember { mutableStateOf(FormatUtils.formatPercent(defaultTargetPercentage)) }
    var notes by remember { mutableStateOf("") }

    LaunchedEffect(defaultUnitItem, defaultAmount) {
        if (assetSymbol.isBlank() && defaultUnitItem != null) {
            assetSymbol = defaultUnitItem.name
            if (buyPriceText.isBlank() && defaultUnitItem.currentPrice > 0.0) {
                buyPriceText = FormatUtils.formatPlain(defaultUnitItem.currentPrice)
            }
        }
        if (amountText.isBlank() && defaultAmount > 0.0) {
            amountText = FormatUtils.formatAmount(defaultAmount)
        }
    }

    var symbolError by remember { mutableStateOf(false) }
    var buyPriceError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }
    var targetError by remember { mutableStateOf(false) }

    val buyPriceVal = buyPriceText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val amountVal = amountText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val targetPercentVal = targetPercentageText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val totalCost = buyPriceVal * amountVal

    // Hedef Fiyat: Alış Fiyatı * (1 + targetPercentage / 100)
    // Örn: Alış 1000, Hedef %1 -> 1010
    val targetPriceVal = buyPriceVal * (1.0 + (targetPercentVal / 100.0))
    val targetProfitVal = (targetPriceVal - buyPriceVal) * amountVal

    fun formatPrice(value: Double): String = FormatUtils.formatPrice(value)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Yeni Likidasyon Kaydı",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Kapat")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            var unitDropdownExpanded by remember { mutableStateOf(false) }

            // 1. Varlık / Sembol (Spinner)
            ExposedDropdownMenuBox(
                expanded = unitDropdownExpanded,
                onExpandedChange = { unitDropdownExpanded = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("spinner_asset_symbol")
            ) {
                OutlinedTextField(
                    value = assetSymbol,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Varlık / Sembol") },
                    placeholder = { Text("Varlık / Sembol Seçin") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitDropdownExpanded)
                    },
                    isError = symbolError,
                    supportingText = if (symbolError) {
                        { Text("Lütfen bir varlık/sembol seçin") }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .testTag("input_asset_symbol")
                )

                ExposedDropdownMenu(
                    expanded = unitDropdownExpanded,
                    onDismissRequest = { unitDropdownExpanded = false },
                    modifier = Modifier.testTag("asset_symbol_dropdown_menu")
                ) {
                    if (units.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Kayıtlı birim bulunamadı") },
                            onClick = { unitDropdownExpanded = false }
                        )
                    } else {
                        units.forEach { u ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = u.name,
                                            fontWeight = if (assetSymbol.equals(u.name, ignoreCase = true)) FontWeight.Bold else FontWeight.Medium
                                        )
                                        if (u.currentPrice > 0.0) {
                                            Text(
                                                text = FormatUtils.prefixCurrency(formatPrice(u.currentPrice), currencySymbol),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    assetSymbol = u.name
                                    symbolError = false
                                    if (u.currentPrice > 0.0) {
                                        buyPriceText = FormatUtils.formatPlain(u.currentPrice)
                                        buyPriceError = false
                                    }
                                    unitDropdownExpanded = false
                                },
                                modifier = Modifier.testTag("spinner_item_${u.name}")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val curSuffix = FormatUtils.sanitizeCurrency(currencySymbol).let { if (it.isNotBlank()) " ($it)" else "" }

            // 2. Alış Fiyatı & 3. Miktar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = buyPriceText,
                    onValueChange = {
                        buyPriceText = it
                        buyPriceError = false
                    },
                    label = { Text("Alış Fiyatı$curSuffix") },
                    placeholder = { Text("1000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = buyPriceError,
                    supportingText = if (buyPriceError) {
                        { Text("Geçersiz fiyat") }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_buy_price")
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        amountError = false
                    },
                    label = { Text("Miktar") },
                    placeholder = { Text("1") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountError,
                    supportingText = if (amountError) {
                        { Text("Geçersiz miktar") }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_amount")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Hedef Satış Yüzdesi (%)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Hedef Satış Yüzdesi (%)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "%$targetPercentageText",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Quick percentage chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("1", "2", "3", "5", "10").forEach { pct ->
                        FilterChip(
                            selected = targetPercentageText == pct,
                            onClick = {
                                targetPercentageText = pct
                                targetError = false
                            },
                            label = { Text("%$pct") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = targetPercentageText,
                    onValueChange = {
                        targetPercentageText = it
                        targetError = false
                    },
                    label = { Text("Özel Hedef Satış Yüzdesi (%)") },
                    placeholder = { Text("Örn: 1.0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = targetError,
                    supportingText = if (targetError) {
                        { Text("Lütfen geçerli bir yüzde girin") }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_target_percentage")
                )
            }

            // Real-time Hedef Fiyat & Kâr Calculation Card
            if (buyPriceVal > 0.0) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.CrisisAlert,
                                    contentDescription = null,
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Hedef Satış Fiyatı:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = FormatUtils.prefixCurrency(formatPrice(targetPriceVal), currencySymbol),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen
                            )
                        }

                        if (amountVal > 0.0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Tahmini Net Kâr (+%${FormatUtils.formatPercent(targetPercentVal)}):",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "+${FormatUtils.prefixCurrency(FormatUtils.formatCurrency(targetProfitVal), currencySymbol)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreen
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Notlar
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notlar (İsteğe bağlı)") },
                placeholder = { Text("Açıklama, strateji veya alış notu...") },
                maxLines = 3,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_notes")
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("İptal")
                }

                Button(
                    onClick = {
                        val sym = assetSymbol.trim()
                        val buyPrice = buyPriceText.replace(',', '.').toDoubleOrNull()
                        val amt = amountText.replace(',', '.').toDoubleOrNull()
                        val targetPct = targetPercentageText.replace(',', '.').toDoubleOrNull()

                        var valid = true
                        if (sym.isBlank()) {
                            symbolError = true
                            valid = false
                        }
                        if (buyPrice == null || buyPrice <= 0.0) {
                            buyPriceError = true
                            valid = false
                        }
                        if (amt == null || amt <= 0.0) {
                            amountError = true
                            valid = false
                        }
                        if (targetPct == null || targetPct < 0.0) {
                            targetError = true
                            valid = false
                        }

                        if (valid && buyPrice != null && amt != null && targetPct != null) {
                            onSubmit(sym, buyPrice, amt, targetPct, notes)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("create_record_submit_button")
                ) {
                    Text("Kaydet", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
