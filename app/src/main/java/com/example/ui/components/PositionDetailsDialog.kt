package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.LiquidationRecord
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseRed
import com.example.util.FormatUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PositionDetailsDialog(
    record: LiquidationRecord,
    currencySymbol: String,
    currentPrice: Double? = null,
    onDismiss: () -> Unit,
    onSellClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null
) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    val totalCost = record.totalCost()
    val targetPrice = record.targetPrice()
    val targetProfit = record.targetProfit()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        title = {
            Text(
                text = "${record.assetSymbol} Kayıt Detayları",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = record.assetSymbol,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    val isEligible = !record.isSold && currentPrice != null && currentPrice >= targetPrice
                    Surface(
                        color = if (record.isSold) Color(0xFF6B7280).copy(alpha = 0.15f) else if (isEligible) EmeraldGreen.copy(alpha = 0.2f) else EmeraldGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (record.isSold) "SATILDI" else if (isEligible) "SATIŞA UYGUN" else "AKTİF",
                            fontWeight = FontWeight.Bold,
                            color = if (record.isSold) Color(0xFF4B5563) else EmeraldGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        DetailRow("Alış Fiyatı", FormatUtils.prefixCurrency(FormatUtils.formatPrice(record.buyPrice), currencySymbol))
                        if (currentPrice != null && currentPrice > 0.0) {
                            DetailRow(
                                "Güncel Piyasa Fiyatı",
                                FormatUtils.prefixCurrency(FormatUtils.formatPrice(currentPrice), currencySymbol),
                                color = if (currentPrice >= targetPrice) EmeraldGreen else MaterialTheme.colorScheme.primary
                            )
                        }
                        val unitSuffix = if (record.unit.isNotBlank()) " ${record.unit}" else ""
                        DetailRow("Miktar", "${FormatUtils.formatAmount(record.amount)}$unitSuffix")
                        DetailRow("Toplam Alış Maliyeti", FormatUtils.prefixCurrency(FormatUtils.formatCurrency(totalCost), currencySymbol))
                        DetailRow("Hedef Satış Yüzdesi", "+%${FormatUtils.formatPercent(record.targetPercentage)}", color = EmeraldGreen)
                        DetailRow("Hedef Satış Fiyatı", FormatUtils.prefixCurrency(FormatUtils.formatPrice(targetPrice), currencySymbol), color = EmeraldGreen)
                        DetailRow("Hedeflenen Kâr", "+${FormatUtils.prefixCurrency(FormatUtils.formatCurrency(targetProfit), currencySymbol)}", color = EmeraldGreen)

                        if (!record.isSold && currentPrice != null && currentPrice > 0.0) {
                            val livePnl = (currentPrice - record.buyPrice) * record.amount
                            val isEligibleNow = currentPrice >= targetPrice
                            DetailRow(
                                "Anlık Kâr/Zarar",
                                "${if (livePnl >= 0) "+" else ""}${FormatUtils.prefixCurrency(FormatUtils.formatCurrency(livePnl), currencySymbol)}",
                                color = if (livePnl >= 0) EmeraldGreen else RoseRed
                            )
                            DetailRow(
                                "Hedef Durumu",
                                if (isEligibleNow) "Hedefe Ulaşıldı (Satışa Uygun)" else "%${FormatUtils.formatPercent(((targetPrice - currentPrice) / currentPrice) * 100.0)} kaldı",
                                color = if (isEligibleNow) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (record.isSold) {
                    val salePnl = record.salePnl ?: 0.0
                    val isProfit = salePnl >= 0.0
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isProfit) Color(0xFF10B981).copy(alpha = 0.1f) else Color(0xFFEF4444).copy(alpha = 0.1f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            DetailRow("Gerçekleşen Satış Fiyatı", record.salePrice?.let { FormatUtils.prefixCurrency(FormatUtils.formatPrice(it), currencySymbol) } ?: "-")
                            DetailRow(
                                "Gerçekleşen Kâr/Zarar",
                                "${if (isProfit) "+" else ""}${FormatUtils.prefixCurrency(FormatUtils.formatCurrency(salePnl), currencySymbol)}",
                                color = if (isProfit) EmeraldGreen else RoseRed
                            )
                            record.saleDate?.let {
                                DetailRow("Satış Tarihi", dateFormat.format(Date(it)))
                            }
                        }
                    }
                }

                if (record.notes.isNotBlank()) {
                    Text(
                        text = "Notlar: ${record.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "Oluşturulma: ${dateFormat.format(Date(record.createdAt))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                record.updatedAt?.let { updated ->
                    Text(
                        text = "Düzenleme: ${dateFormat.format(Date(updated))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!record.isSold) {
                    OutlinedButton(onClick = {
                        onDismiss()
                        onEditClick()
                    }) {
                        Text("Düzenle")
                    }
                }
                if (!record.isSold) {
                    Button(onClick = {
                        onDismiss()
                        onSellClick()
                    }) {
                        Text("Sat")
                    }
                } else {
                    Button(onClick = onDismiss) {
                        Text("Tamam")
                    }
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onDeleteClick != null) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onDeleteClick()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Sil")
                    }
                }
                OutlinedButton(onClick = onDismiss) {
                    Text("Kapat")
                }
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String, color: Color = Color.Unspecified) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = if (color != Color.Unspecified) color else MaterialTheme.colorScheme.onSurface
        )
    }
}
