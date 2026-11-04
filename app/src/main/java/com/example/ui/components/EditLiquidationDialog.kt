package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.data.model.LiquidationRecord
import com.example.ui.theme.EmeraldGreen
import com.example.util.FormatUtils
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.example.data.model.UnitItem

@Composable
fun EditLiquidationDialog(
    record: LiquidationRecord,
    currencySymbol: String,
    units: List<UnitItem> = emptyList(),
    onDismiss: () -> Unit,
    onConfirmUpdate: (
        assetSymbol: String,
        buyPrice: Double,
        amount: Double,
        targetPercentage: Double,
        notes: String
    ) -> Unit
) {
    var assetSymbol by remember { mutableStateOf(record.assetSymbol) }
    var buyPriceText by remember { mutableStateOf(FormatUtils.formatPlain(record.buyPrice)) }
    var amountText by remember { mutableStateOf(FormatUtils.formatAmount(record.amount)) }
    var targetPercentageText by remember { mutableStateOf(FormatUtils.formatPercent(record.targetPercentage)) }
    var notes by remember { mutableStateOf(record.notes) }

    var symbolError by remember { mutableStateOf(false) }
    var buyPriceError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }
    var targetError by remember { mutableStateOf(false) }

    val buyPriceVal = buyPriceText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val amountVal = amountText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val targetPercentVal = targetPercentageText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val targetPriceVal = buyPriceVal * (1.0 + (targetPercentVal / 100.0))

    fun formatPrice(value: Double): String = FormatUtils.formatPrice(value)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Kaydı Düzenle",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Varlık / Sembol
                OutlinedTextField(
                    value = assetSymbol,
                    onValueChange = {
                        assetSymbol = it
                        symbolError = false
                    },
                    label = { Text("Varlık / Sembol") },
                    isError = symbolError,
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (units.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(units) { u ->
                            FilterChip(
                                selected = assetSymbol.equals(u.name, ignoreCase = true),
                                onClick = {
                                    assetSymbol = u.name
                                    symbolError = false
                                    if (u.currentPrice > 0.0) {
                                        buyPriceText = if (u.currentPrice >= 1.0) {
                                            String.format(java.util.Locale.US, "%.2f", u.currentPrice)
                                        } else {
                                            String.format(java.util.Locale.US, "%.4f", u.currentPrice)
                                        }
                                        buyPriceError = false
                                    }
                                },
                                label = {
                                    Text("${u.name}${if (u.currentPrice > 0.0) " (${FormatUtils.prefixCurrency(formatPrice(u.currentPrice), currencySymbol)})" else ""}")
                                }
                            )
                        }
                    }
                }

                val curSuffix = FormatUtils.sanitizeCurrency(currencySymbol).let { if (it.isNotBlank()) " ($it)" else "" }

                // Alış Fiyatı & Miktar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = buyPriceText,
                        onValueChange = {
                            buyPriceText = it
                            buyPriceError = false
                        },
                        label = { Text("Alış$curSuffix") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = buyPriceError,
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = it
                            amountError = false
                        },
                        label = { Text("Miktar") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = amountError,
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Hedef Satış Yüzdesi (%)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("1", "2", "3", "5", "10").forEach { pct ->
                            FilterChip(
                                selected = targetPercentageText == pct,
                                onClick = {
                                    targetPercentageText = pct
                                    targetError = false
                                },
                                label = { Text("%$pct") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = targetPercentageText,
                        onValueChange = {
                            targetPercentageText = it
                            targetError = false
                        },
                        label = { Text("Hedef Satış Yüzdesi (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = targetError,
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Target Price preview
                if (buyPriceVal > 0.0) {
                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.CrisisAlert, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                                Text("Hedef Satış Fiyatı:", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                FormatUtils.prefixCurrency(formatPrice(targetPriceVal), currencySymbol),
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen
                            )
                        }
                    }
                }

                // Notlar
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notlar") },
                    maxLines = 2,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
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
                        onConfirmUpdate(sym, buyPrice, amt, targetPct, notes)
                    }
                },
                modifier = Modifier.testTag("confirm_edit_button")
            ) {
                Text("Güncelle")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Vazgeç")
            }
        }
    )
}
