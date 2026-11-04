package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LiquidationRecord
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseRed
import com.example.util.FormatUtils

@Composable
fun LiquidationCard(
    record: LiquidationRecord,
    currencySymbol: String,
    onSellClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onReopenClick: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier,
    currentPrice: Double? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    fun formatPrice(value: Double): String = FormatUtils.formatPrice(value)

    val totalCost = record.totalCost()
    val targetPrice = record.targetPrice()
    val targetProfit = record.targetProfit()

    val isEligibleForSale = !record.isSold && currentPrice != null && currentPrice > 0.0 && currentPrice >= targetPrice
    val currentGainPercent = if (currentPrice != null && currentPrice > 0.0 && record.buyPrice > 0.0) {
        ((currentPrice - record.buyPrice) / record.buyPrice) * 100.0
    } else null
    val livePnl = if (currentPrice != null && currentPrice > 0.0) {
        (currentPrice - record.buyPrice) * record.amount
    } else null

    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    val (dateLabel, dateTimestamp) = remember(record.createdAt, record.updatedAt, record.isSold, record.saleDate) {
        val tCreated = record.createdAt
        val tUpdated = record.updatedAt ?: 0L
        val tSold = if (record.isSold) (record.saleDate ?: 0L) else 0L
        when {
            tSold > 0L && tSold >= tUpdated && tSold >= tCreated -> "Satış" to tSold
            tUpdated > 0L && tUpdated >= tCreated -> "Düzenleme" to tUpdated
            else -> "Oluşturulma" to tCreated
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("liquidation_card_${record.id}")
            .clickable(enabled = record.isSold) { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        border = if (isEligibleForSale) BorderStroke(1.5.dp, EmeraldGreen) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isEligibleForSale) EmeraldGreen.copy(alpha = 0.04f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isEligibleForSale) 4.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Varlık / Sembol & Durum / Hedef Rozeti
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = record.assetSymbol,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Target percentage badge
                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Hedef: +%${FormatUtils.formatPercent(record.targetPercentage)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    // Status Pill
                    if (record.isSold) {
                        Surface(
                            color = Color(0xFF6B7280).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Satıldı",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4B5563),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    } else if (isEligibleForSale) {
                        Surface(
                            color = EmeraldGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Satışa Uygun",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreen
                                )
                            }
                        }
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Aktif",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // 3-dots Options Menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Seçenekler",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (record.isSold) {
                            DropdownMenuItem(
                                text = { Text("Detaylar") },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onCardClick()
                                }
                            )
                        }
                        if (!record.isSold) {
                            DropdownMenuItem(
                                text = { Text("Düzenle") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onEditClick()
                                }
                            )
                        }
                        if (record.isSold) {
                            DropdownMenuItem(
                                text = { Text("Pozisyonu Tekrar Aç") },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onReopenClick()
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Sat") },
                                leadingIcon = { Icon(Icons.Default.PointOfSale, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onSellClick()
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Sil", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                showMenu = false
                                showDeleteConfirmDialog = true
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Metrics: Alış Fiyatı | (Güncel Fiyat veya Miktar) | Hedef Satış Fiyatı
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (isEligibleForSale) EmeraldGreen.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Alış Fiyatı",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = FormatUtils.prefixCurrency(formatPrice(record.buyPrice), currencySymbol),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (!record.isSold && currentPrice != null && currentPrice > 0.0) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Güncel Fiyat",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isEligibleForSale) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = FormatUtils.prefixCurrency(formatPrice(currentPrice), currencySymbol),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isEligibleForSale) EmeraldGreen else if (currentPrice >= record.buyPrice) MaterialTheme.colorScheme.primary else RoseRed
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Miktar",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = FormatUtils.formatAmount(record.amount),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Hedef Fiyat",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = FormatUtils.prefixCurrency(formatPrice(targetPrice), currencySymbol),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Amount & Live PnL / Target Profit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Miktar: ${FormatUtils.formatAmount(record.amount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!record.isSold && currentPrice != null && currentPrice > 0.0) {
                    val pnl = livePnl ?: 0.0
                    val isProfit = pnl >= 0.0
                    Text(
                        text = "Anlık: ${if (isProfit) "+" else ""}${FormatUtils.prefixCurrency(FormatUtils.formatCurrency(pnl), currencySymbol)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isEligibleForSale) EmeraldGreen else if (isProfit) MaterialTheme.colorScheme.primary else RoseRed
                    )
                } else {
                    Text(
                        text = "Hedef Kâr: +${FormatUtils.prefixCurrency(FormatUtils.formatCurrency(targetProfit), currencySymbol)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen
                    )
                }
            }

            // Under "Anlık: .." section:
            // When currentPrice < targetPrice: show "Hedefe %... kaldı"
            // When currentPrice >= targetPrice (or target reached): show current increase percentage "+%..."
            if (!record.isSold && currentPrice != null && currentPrice > 0.0) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (isEligibleForSale) {
                        if (currentGainPercent != null) {
                            Text(
                                text = "+%${FormatUtils.formatPercent(currentGainPercent)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen
                            )
                        }
                    } else {
                        val distPercent = ((targetPrice - currentPrice) / currentPrice) * 100.0
                        if (distPercent > 0.0) {
                            Text(
                                text = "Hedefe %${FormatUtils.formatPercent(distPercent)} kaldı",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // If record is sold, display sale price and realized PnL
            if (record.isSold) {
                Spacer(modifier = Modifier.height(8.dp))
                val salePnl = record.salePnl ?: 0.0
                val isProfit = salePnl >= 0.0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (isProfit) Color(0xFF10B981).copy(alpha = 0.1f) else Color(0xFFEF4444).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isProfit) EmeraldGreen else RoseRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Satış Fiyatı: ${record.salePrice?.let { FormatUtils.prefixCurrency(formatPrice(it), currencySymbol) } ?: "-"}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = "Net: ${if (salePnl >= 0) "+" else ""}${FormatUtils.prefixCurrency(FormatUtils.formatCurrency(salePnl), currencySymbol)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (salePnl >= 0) EmeraldGreen else RoseRed
                    )
                }
            }

            // Date row (Oluşturulma, Düzenleme veya Satış) - Now placed ABOVE notes
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = when (dateLabel) {
                        "Satış" -> Icons.Default.CheckCircle
                        "Düzenleme" -> Icons.Default.Edit
                        else -> Icons.Default.Schedule
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "$dateLabel: ${dateFormat.format(Date(dateTimestamp))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            // Notes if provided - Now placed BELOW date
            if (record.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notes,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = record.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }

    // Confirmation Dialog before deletion
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "Kaydı Sil",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "${record.assetSymbol} likidasyon kaydını silmek istediğinize emin misiniz? Bu işlem geri alınamaz."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Sil")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false },
                    modifier = Modifier.testTag("cancel_delete_button")
                ) {
                    Text("İptal")
                }
            }
        )
    }
}
