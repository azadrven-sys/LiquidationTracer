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
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.example.ui.theme.RoseRed
import com.example.util.FormatUtils

@Composable
fun SellDialog(
    record: LiquidationRecord,
    currencySymbol: String,
    currentPrice: Double? = null,
    onDismiss: () -> Unit,
    onConfirmSell: (salePrice: Double) -> Unit
) {
    fun formatPrice(value: Double): String = FormatUtils.formatPrice(value)

    val targetPrice = record.targetPrice()
    val initialPrice = if (currentPrice != null && currentPrice > 0.0) {
        currentPrice
    } else {
        targetPrice
    }
    var salePriceText by remember {
        mutableStateOf(FormatUtils.formatPlain(initialPrice))
    }
    var priceError by remember { mutableStateOf(false) }

    val salePrice = salePriceText.replace(',', '.').toDoubleOrNull() ?: targetPrice
    val pnl = (salePrice - record.buyPrice) * record.amount
    val totalCost = record.totalCost()
    val roi = if (record.buyPrice > 0.0) ((salePrice - record.buyPrice) / record.buyPrice) * 100.0 else 0.0
    val isProfit = pnl >= 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.PointOfSale,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Kaydı Sat / Kapat",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = record.assetSymbol,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Summary
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Alış Fiyatı:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                FormatUtils.prefixCurrency(FormatUtils.formatPrice(record.buyPrice), currencySymbol),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Miktar:", style = MaterialTheme.typography.bodySmall)
                            val unitSuffix = if (record.unit.isNotBlank()) " ${record.unit}" else ""
                            Text(
                                "${FormatUtils.formatAmount(record.amount)}$unitSuffix",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Toplam Alış Maliyeti:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                FormatUtils.prefixCurrency(FormatUtils.formatCurrency(totalCost), currencySymbol),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Alert Banner if current market price reached target
                if (currentPrice != null && currentPrice > 0.0 && currentPrice >= targetPrice) {
                    Surface(
                        color = EmeraldGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CrisisAlert,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Piyasa fiyatı hedef fiyata ulaştı!",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen
                            )
                        }
                    }
                }

                // Quick target and market price buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = salePriceText == FormatUtils.formatPlain(targetPrice),
                        onClick = {
                            salePriceText = FormatUtils.formatPlain(targetPrice)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.CrisisAlert, contentDescription = null, tint = EmeraldGreen)
                        },
                        label = {
                            Text("Hedef: ${FormatUtils.prefixCurrency(formatPrice(targetPrice), currencySymbol)}")
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF10B981).copy(alpha = 0.15f),
                            selectedLabelColor = EmeraldGreen
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    if (currentPrice != null && currentPrice > 0.0) {
                        FilterChip(
                            selected = salePriceText == FormatUtils.formatPlain(currentPrice),
                            onClick = {
                                salePriceText = FormatUtils.formatPlain(currentPrice)
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.PointOfSale,
                                    contentDescription = null,
                                    tint = if (currentPrice >= targetPrice) EmeraldGreen else MaterialTheme.colorScheme.primary
                                )
                            },
                            label = {
                                Text("Güncel: ${FormatUtils.prefixCurrency(formatPrice(currentPrice), currencySymbol)}")
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (currentPrice >= targetPrice) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = if (currentPrice >= targetPrice) EmeraldGreen else MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                val curSuffix = FormatUtils.sanitizeCurrency(currencySymbol).let { if (it.isNotBlank()) " ($it)" else "" }

                // Sale Price Field
                OutlinedTextField(
                    value = salePriceText,
                    onValueChange = {
                        salePriceText = it
                        priceError = false
                    },
                    label = { Text("Satış Fiyatı$curSuffix") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = priceError,
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_sale_price")
                )

                // Live PnL Preview
                Surface(
                    color = if (isProfit) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFFEF4444).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isProfit) "Net Kâr" else "Net Zarar",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isProfit) EmeraldGreen else RoseRed,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${if (isProfit) "+" else ""}${FormatUtils.prefixCurrency(FormatUtils.formatCurrency(pnl), currencySymbol)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isProfit) EmeraldGreen else RoseRed
                        )
                        Text(
                            text = "Getiri: ${if (isProfit) "+" else ""}%${FormatUtils.formatCurrency(roi)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isProfit) EmeraldGreen else RoseRed,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = salePriceText.replace(',', '.').toDoubleOrNull()
                    if (price != null && price > 0.0) {
                        onConfirmSell(price)
                    } else {
                        priceError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isProfit) EmeraldGreen else MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_sell_button")
            ) {
                Text("Satışı Onayla")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Vazgeç")
            }
        }
    )
}
